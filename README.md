# Hollow Demo — Two-Dataset Streaming Platform

Demo application showcasing [Netflix Hollow](https://hollow.how), an open-source library for distributing large read-only datasets in memory across a service fleet using a snapshot + delta model.

Measured live against AWS (S3 + DynamoDB). Full numbers in [`demo-facts.md`](./demo-facts.md).

---

## The Problem

A streaming platform needs to answer two questions millions of times per second:

- *What movies are available right now?*
- *What has this user been watching?*

Querying a database on every request collapses under that load. Aggressive caching fights cache invalidation. Data replication fights consistency.

**Hollow** distributes your entire dataset as a read-only, in-memory snapshot to every service instance. No database hop. No cache miss. Every read is a local memory lookup.

---

## Architecture

Two independent Hollow datasets, each with its own producer and update cadence:

| Dataset | Records | Update cadence | Storage |
|---|---|---|---|
| **Catalog** | 50,000 movies | Every 7 minutes | `s3://.../catalog/` |
| **Users** | 350,000 profiles | Every 10 minutes | `s3://.../users/` |

Each producer publishes blobs to **S3** and announces new versions to **DynamoDB**. Consumers poll DynamoDB every 5 seconds and stream only the **delta** — not a full reload.

```
Producer (catalog)          S3 bucket                Consumer (fleet)
──────────────────    ──────────────────────────    ────────────────────
generateInitial()  →  catalog/snapshot-v1           triggerRefresh() →
generateUpdated()  →  catalog/delta-v1-v2      →    DELTA applied in 25ms
                      catalog/reversedelta-v2-v1
                      users/snapshot-v1
                      users/delta-v1-v2
```

The interesting endpoint is the **cross-dataset join**:

```
GET /users/{userId}/recently-watched
  1. Look up user   in the users   snapshot  → O(1), local memory
  2. Look up movies in the catalog snapshot  → O(1) per ID, local memory
  3. Return enriched results                 → no database, no network
```

Two independent datasets, two different update cadences, joined in memory at query time. This is the pattern Netflix uses at scale.

---

## Measured Numbers

Numbers from a live AWS run. Reproduce with `./gradlew measureJsonSize`.

### Wire & heap comparison

| Format | Catalog (50K movies) | Users (350K users) | Combined |
|---|---|---|---|
| JSON uncompressed | 31.1 MB | 74.6 MB | **105.7 MB** |
| JSON gzipped | 4.4 MB | 24.4 MB | **28.8 MB** |
| **POJO heap after deserialize** | **69.7 MB** | **218.5 MB** | **288 MB** |
| Hollow snapshot (S3) | 10.4 MB | 24.7 MB | **35.1 MB** |
| **Hollow heap (live)** | **11.4 MB** | **24.6 MB** | **35.9 MB** |

**POJO heap is 8× larger than Hollow heap for the same data.**

### The delta advantage

| What | Size |
|---|---|
| Full catalog snapshot | 10.4 MB |
| **Catalog delta (one 7-min cycle)** | **620 KB** |
| Reverse delta (rollback) | 249 KB |
| Consumer delta apply time | **25–41 ms** |

At 100 consumer instances, each catalog refresh cycle costs **62 MB** of egress (delta × 100) vs **3.1 GB** if you were broadcasting JSON.

### Consumer startup (cold boot)

| Event | Time |
|---|---|
| Catalog snapshot load + index build | 1.3 s |
| Users snapshot load + index build | 2.4 s |
| Full Spring Boot startup (both datasets) | **5.7 s** |

After startup, all updates are deltas — no restart, no full reload, no memory spike.

---

## The Gzip Trap

The instinctive objection: *"We already gzip our API responses, so the wire size is fine."*

That's true for the wire. It misses what happens next.

```
Wire:       28.8 MB  (gzipped)
              ↓  GZIPInputStream
JSON text:  105.7 MB
              ↓  Jackson deserializer
JVM heap:   288 MB   ← this is what your service actually pays
```

When Jackson deserializes into Java objects, it allocates one object per record. For each `Movie`: object header, 12 `String` fields (each its own heap allocation with char array), 3 `ArrayList` fields (wrapper + backing array + individual `String` objects per element). For each `User`: every `Long` in `recentlyWatchedMovieIds` becomes a **boxed object** — 16 bytes instead of 8 — multiplied across 350,000 users × 10 watch history entries = 3.5 million extra objects.

Hollow holds the same data in 36 MB because its binary format uses packed arrays: all movie titles share a single ordinal-indexed string pool, list fields are offset ranges into flat arrays, numeric fields stay as primitives.

> *Gzip is transport compression. Hollow is heap compression. They solve different problems, and only one of them matters once the data is live in your service.*

---

## Running the Demo

### Prerequisites

- Java 21
- AWS credentials with access to S3 and DynamoDB
- An S3 bucket and DynamoDB table (see setup below)

### AWS Setup

```bash
# Create the S3 bucket
aws s3 mb s3://YOUR-BUCKET-NAME --region us-east-1

# Create the DynamoDB table
aws dynamodb create-table \
  --table-name YOUR-TABLE-NAME \
  --attribute-definitions AttributeName=dataset_id,AttributeType=S \
  --key-schema AttributeName=dataset_id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Environment Variables

```bash
export HOLLOW_AWS_REGION=us-east-1
export HOLLOW_AWS_BUCKET=your-bucket-name
export HOLLOW_AWS_DYNAMODB_TABLE=your-table-name

# AWS credentials (or use an IAM role / instance profile)
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
```

### Start the Producer

```bash
SPRING_PROFILES_ACTIVE=producer ./gradlew bootRun
```

On startup the producer generates and publishes:
1. **Catalog snapshot** — 50,000 movies, ~3s, ~10.4 MB to S3
2. **Users snapshot** — 350,000 users, ~4.5s, ~24.7 MB to S3

Then runs on schedule: catalog every 7 minutes (delta ~620 KB), users every 10 minutes.

### Start the Consumer (separate terminal)

```bash
SPRING_PROFILES_ACTIVE=consumer java -jar build/libs/hollowdemo-*.jar --server.port=8081
```

### API Endpoints

**Producer** (port 8080):

```bash
GET  /producer/stats            # cycle counts, record counts, S3 bucket info
POST /producer/catalog/publish  # force a catalog publish cycle
POST /producer/users/publish    # force a users publish cycle
```

**Consumer** (port 8081):

```bash
GET /movies/{id}                      # O(1) movie lookup by ID
GET /movies/genre/{genre}             # genre index lookup
GET /movies/stats                     # snapshot version, record count

GET /users/{userId}                   # O(1) user lookup by UUID
GET /users/{userId}/recently-watched  # cross-dataset join demo
GET /users/stats                      # plan distribution across 350K users
```

### Docker Compose

```bash
cp .env.example .env   # fill in your AWS values
docker compose up --build
```

---

## Running Tests

The full produce → consume → delta → join pipeline runs without any AWS credentials:

```bash
./gradlew test
```

`HollowFilesystemIntegrationTest` uses `HollowFilesystemPublisher` / `HollowFilesystemBlobRetriever` with temp directories. Swap one constructor argument and you're talking to S3. The application code is unchanged — that's Hollow's storage abstraction at work.

Tests cover:
- Snapshot record counts for both datasets
- O(1) primary key lookup (movie by ID, user by UUID)
- Cross-dataset join correctness (recently-watched IDs resolve to full Movie objects)
- Determinism (same seed always produces identical datasets)
- Delta file is smaller than snapshot after second produce cycle

---

## Gradle Tasks

```bash
./gradlew test                  # run all tests (no AWS needed)
./gradlew measureJsonSize       # compare JSON vs Hollow sizes + POJO heap
./gradlew generateHollowAPI     # regenerate Hollow consumer API for catalog
./gradlew generateUserAPI       # regenerate Hollow consumer API for users
./gradlew generateAllAPIs       # regenerate both
```

### First-time setup — codegen required

The Hollow consumer API classes (`src/main/java/.../api/` and `src/main/java/.../api/users/`) are generated from the data model and are **not committed to the repository**. You must generate them before building or running tests:

```bash
./gradlew generateAllAPIs
```

Re-run this any time you change `Movie.kt` or `User.kt`. The generator reads the model, writes the Java consumer API, and the normal build picks it up from there.

---

## Stack

- **Spring Boot 3.5.7** — application framework
- **Kotlin 1.9.25** + **Java 21** — language / runtime
- **Netflix Hollow 7.14.39** — dataset distribution
- **hollow-aws 0.1.0** — S3 publisher + DynamoDB announcer/watcher
- **Datafaker 2.4.2** — deterministic fake data generation (seed = 42)

---

## Resources

- [Hollow Documentation](https://hollow.how)
- [Netflix OSS announcement](https://netflixtechblog.com/netflixoss-announcing-hollow-9f12e9b64a7e)
- [hollow-aws library](https://github.com/vichu/hollow-aws)
