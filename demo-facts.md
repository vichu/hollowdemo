# Demo Facts & Narrative

Measured live against AWS (S3 + DynamoDB, `us-east-1`) on 2026-05-18.  
Run `./gradlew measureJsonSize` to reproduce the JSON/Hollow size comparison locally.

---

## The Problem This Demo Solves

A streaming platform needs to serve two questions millions of times per second:

- *What movies are available right now?*
- *What has this user been watching?*

The naive answer — query a database on every request — collapses under that load. You can cache aggressively, but then you fight cache invalidation. You can replicate data, but then you fight consistency.

**Netflix Hollow** takes a different approach: distribute your entire dataset as a read-only, in-memory snapshot to every service instance. No database hop. No cache miss. Every read is a local memory lookup.

---

## The Architecture

This demo runs two independent Hollow datasets, each with its own producer and its own update cadence:

| Dataset | Records | Update cadence | Why |
|---|---|---|---|
| **Catalog** | 50,000 movies | Every 7 minutes | Content changes frequently: new releases, rating updates, availability |
| **Users** | 350,000 profiles | Every 10 minutes | Preferences change slowly; dataset is recomputed from source of truth |

Each producer publishes to **S3** (blob storage) and **DynamoDB** (version announcement). Consumers across the fleet watch DynamoDB every 5 seconds and stream only the **delta** on each update cycle — not a full reload.

---

## Size & Timing — Measured Numbers

### Catalog dataset (50,000 movies)

| What | Size | Notes |
|---|---|---|
| JSON — uncompressed | **31.1 MB** | What you'd GET from a REST API |
| JSON — gzipped | **4.4 MB** | Best-case HTTP transfer size |
| **POJO heap after deserialize** | **69.7 MB** | What actually stays in RAM |
| Hollow snapshot (S3) | **10.4 MB** | Binary blob, random-access |
| **Hollow heap (in-memory)** | **11.4 MB** | Live consumer footprint |
| **Hollow delta (one cycle)** | **620 KB** | ~4% churn per update |
| Hollow reverse delta | **249 KB** | Stored for rollback |

> Gzip is transport compression only. Once deserialized into Java objects, the JVM overhead
> (object headers, reference pointers, String char arrays, ArrayList backing arrays) inflates
> 31 MB of JSON text into 70 MB of live heap — 6× the Hollow footprint for the same data.

**Data generation time:** 286 ms (deterministic, seed = 99)  
**Cycle 1 (snapshot) publish time:** 2,997 ms end-to-end  
**Cycle 2 (delta) publish time:** 3,406 ms end-to-end  
**Consumer delta apply time:** 25–41 ms

### Users dataset (350,000 users)

| What | Size | Notes |
|---|---|---|
| JSON — uncompressed | **74.6 MB** | UUID-heavy records |
| JSON — gzipped | **24.4 MB** | Similar to Hollow on wire |
| **POJO heap after deserialize** | **218.5 MB** | What actually stays in RAM |
| Hollow snapshot (S3) | **24.7 MB** | Matches gzip wire size |
| **Hollow heap (in-memory)** | **24.6 MB** | Live consumer footprint |

> For users, gzip and Hollow are nearly identical on the wire (24.4 MB vs 24.7 MB) — the
> UUID strings and names compress well. But the POJO heap is 9× larger than Hollow once
> deserialized, because each Long in `recentlyWatchedMovieIds` becomes a boxed `Long`
> object (16 bytes each), and every string field is a separate heap allocation.

**Data generation time:** 1,393 ms (deterministic, seed = 42, using Datafaker)  
**Cycle 1 (snapshot) publish time:** 4,490 ms end-to-end  
**Consumer snapshot load time:** 2.4 s

### Combined — both datasets

| Format | Total size |
|---|---|
| JSON (uncompressed) | **105.7 MB** |
| JSON (gzipped / wire) | **28.8 MB** |
| **POJO heap after deserialize** | **288.3 MB** |
| **Hollow snapshots (S3 / wire)** | **35.1 MB** |
| **Hollow heap (live memory)** | **35.9 MB** |

**POJO heap is 8× larger than Hollow heap for the same data.**

### Consumer startup

Both datasets are loaded from S3 snapshots on first boot:

| Event | Time |
|---|---|
| Catalog snapshot load + index build | **1.3 s** |
| Users snapshot load + index build | **2.4 s** |
| Full consumer startup (Spring + both datasets) | **5.7 s** |

After startup, all subsequent updates are deltas — no full reload, no restart required.

---

## The Delta Is the Real Story

Each catalog refresh cycle ships **620 KB** over the wire instead of 31 MB (JSON) or 10.4 MB (full snapshot).

At 100 consumer instances:

| Per-cycle network egress | Volume |
|---|---|
| JSON broadcast | 3.1 GB |
| Full Hollow snapshot broadcast | 1.04 GB |
| **Hollow delta broadcast** | **62 MB** |

That's **50× less data per refresh cycle** compared to JSON, and **17× less** compared to re-broadcasting the full snapshot. Every 7 minutes, every update cycle.

---

## The Cross-Dataset Join

The interesting endpoint is:

```
GET /users/{userId}/recently-watched
```

1. Look up the user in the `users` Hollow dataset — **O(1), local memory, no network**
2. For each `recentlyWatchedMovieId`, look up the full `Movie` in the `catalog` dataset — **O(1), local memory, no network**
3. Return enriched results

Two independent datasets, two different update cadences, **joined in memory at query time**. No database join. No API fan-out. No cache warming.

This is the pattern Netflix uses at scale.

---

## The Gzip Trap

The instinctive counter-argument to Hollow is: *"We already gzip our API responses, so the wire size is fine."* That's true for the wire. It misses what happens next.

**The pipeline for gzipped JSON:**

```
Disk / wire : 28.8 MB  (gzipped)
     ↓  GZIPInputStream — streaming, fine
JSON text   : 105.7 MB — briefly in memory if buffered
     ↓  Jackson deserializer
JVM heap    : 288 MB   ← this is what your service actually pays
```

Gzip decompresses before Jackson ever sees a byte. Jackson then allocates one Java object per record. For each `Movie` that means:

- A `Movie` POJO: object header (16 bytes) + field storage for 18 fields
- 12 `String` fields (title, genre, description, director...): each its own heap object — header + char array
- 3 `List` fields (cast, writers, tags): `ArrayList` wrapper + `Object[]` backing array + individual `String` objects per element
- Primitive numerics (`id`, `budget`, `boxOffice`) stay efficient as JVM primitives

For each `User` the multiplier is worse:

- `recentlyWatchedMovieIds: List<Long>` — every `Long` in the list is a **boxed object** (16 bytes on heap), not a primitive (8 bytes). A watch history of 10 movies costs 160 bytes in objects vs 80 bytes as a primitive array.
- 350,000 users × average 10 watched IDs = 3.5 million `Long` objects allocated and kept alive.

**Measured result:** 288 MB of live JVM heap for data that is 29 MB gzipped on the wire.

Hollow holds the same data in **36 MB** because its binary format is designed around packed arrays from the ground up:

- All movie titles across all 50,000 records share a single ordinal-indexed string pool — one `byte[]`, no per-string object headers
- List fields (`cast`, `recentlyWatchedMovieIds`) are stored as offset ranges into flat arrays — no `ArrayList`, no `Object[]`, no boxing
- Numeric fields stay as primitives in contiguous arrays

**The slide-worthy framing:**

> *Gzip is transport compression. Hollow is heap compression. They solve different problems, and only one of them matters once the data is live in your service.*

---

## Hollow vs JSON — The Right Comparison

There are three layers to this comparison, and the one that matters most is often skipped:

| Layer | JSON | Hollow |
|---|---|---|
| Wire / S3 size | 28.8 MB (gzipped) | 35.1 MB snapshot / **620 KB delta** |
| **Heap after load** | **288 MB (POJOs)** | **35.9 MB** |
| Random access | Full scan or index rebuild | O(1) primary key index, always |

**Wire size** — gzip and Hollow are roughly equivalent on first load. Hollow wins heavily on subsequent refreshes because it ships only the delta.

**Heap size** — this is the real gap. Gzip is transport compression only. The moment you deserialize into Java objects, the JVM materialises an object per record: a `Movie` POJO with 18 fields, `ArrayList` wrappers for each list field, individual `String` objects for every title, genre, and description, and boxed `Long` objects for every movie ID in a watch history. The result is 288 MB of live heap for data that Hollow holds in 36 MB.

Hollow's binary format uses packed arrays — all 50,000 movie titles share a single ordinal-indexed string pool, list fields are stored as offset ranges into flat arrays, and numeric fields are stored as primitives rather than boxed objects. There are no per-record object headers.

**Random access** — you cannot seek into a gzipped stream. To retrieve record #12,437 from a gzip+JSON payload you must decompress the entire thing first and then scan or rebuild an index. Hollow's primary key index is built at load time and lives for the lifetime of the snapshot.

---

## Testability — The Underrated Feature

The entire pipeline — produce a snapshot, publish deltas, consume, refresh, cross-dataset join — runs in a unit test with no AWS credentials, no mocks, no Docker:

```kotlin
val catalogDir = Files.createTempDirectory("hollow-catalog-test")
val producer = HollowProducer
    .withPublisher(HollowFilesystemPublisher(catalogDir))
    .withAnnouncer(HollowFilesystemAnnouncer(catalogDir))
    .build()
```

Swap `HollowFilesystemPublisher` for `HollowS3Publisher` and you're in production. The application code doesn't change. The test suite runs the same producer/consumer logic that talks to S3 in prod — not a stub, not a mock, the actual code.

That's the architectural guarantee Hollow's storage abstraction provides.

---

## Numbers to Commit to Memory

| Fact | Number |
|---|---|
| Catalog size (movies in demo) | 50,000 |
| Users size (profiles in demo) | 350,000 |
| Combined JSON uncompressed | **105.7 MB** |
| Combined JSON gzipped (wire) | **28.8 MB** |
| Combined POJO heap (after deserialize) | **288 MB** |
| Combined Hollow wire (snapshots) | **35.1 MB** |
| Combined Hollow heap (live) | **35.9 MB** |
| **POJO heap vs Hollow heap** | **8× larger** |
| Delta vs full catalog snapshot | **6%** of snapshot size |
| Delta apply time (consumer) | **25–41 ms** |
| Consumer startup (cold, both datasets) | **5.7 s** |
| Catalog update cycle | every 7 minutes |
| Users update cycle | every 10 minutes |
