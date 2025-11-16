# Versioned Datasets: Rethinking Local In-Memory Caches

**Event:** ACM Fremont Chapter - Emerging Technologies Tech Talk
**Date:** November 16, 2025
**Duration:** 20 minutes + 5 minutes Q&A
**Speaker:** Vish Ranganathan

---

## Abstract

In the world of distributed computing, your data is often served through API requests. For datasets that are relatively small (in the order of a few GBs), one way to have faster data access is by using a local in-memory cache. However, traditional approaches force uncomfortable trade-offs: cold starts, full reloads causing memory spikes and GC pauses, and complex incremental update infrastructure. For gigabyte-scale datasets accessed millions of times per second, these approaches break down—yet this is precisely where in-memory caching matters most.

This talk introduces a different paradigm: treating datasets as versioned, immutable snapshots with delta-based distribution—essentially applying Git's model to in-memory data. We'll explore Hollow, an open-source library by Netflix that implements this pattern, enabling applications to efficiently distribute and update multi-gigabyte datasets with minimal memory overhead, zero downtime, and no GC pressure. Through a live demo and real-world examples, you'll learn when versioned dataset distribution makes sense, how it works under the hood, and how to apply this pattern to solve persistent caching challenges in your own systems.

---

## Talk Structure Overview

```
┌─────────────────────────────────────────────────┐
│ "Versioned Datasets: Rethinking Local Caches"  │
├─────────────────────────────────────────────────┤
│                                                 │
│ Opening Story (0.5 min)                         │
│ └─ The 3 AM cache incident                     │
│                                                 │
│ Act 1: The Context (2.5 min)                   │
│ └─ When and why local caches matter            │
│                                                 │
│ Act 2: The Problem (1.5 min)                   │
│ └─ The operational challenge at GB scale       │
│                                                 │
│ Act 3: The Insight (2.5 min)                   │
│ └─ Git model for data distribution             │
│                                                 │
│ Act 4: Hollow in Action (8 min) ⭐              │
│ └─ Architecture & Live demo                    │
│                                                 │
│ Act 5: Application (2 min)                     │
│ └─ When to use + Production notes              │
│                                                 │
│ Wrap (1 min)                                    │
│                                                 │
│ Q&A (5 min)                                     │
└─────────────────────────────────────────────────┘
```

**Total:** 20 minutes presentation + 5 minutes Q&A

---

## Detailed Section Breakdown

### I. Opening: The Cache Stampede Story (0.5 minutes)

**Opening Hook (via speaker notes, not separate slide):**
> "Let me start with a story that might sound familiar.
>
> It's 2 PM on a Monday. Everything's running smoothly. Your cache just expired—it's been serving data for the past hour, and now it's time to refresh.
>
> But here's what happens: All 50 of your service instances realize the cache is empty at the exact same moment. They all race to reload it.
>
> 50 simultaneous queries hit your database asking for the same 2 GB of data. Your database grinds to a halt. Response times go from milliseconds to minutes. Users start seeing errors.
>
> By the time you realize what's happening, you're in the middle of a full outage.
>
> Sound familiar?"

**[PAUSE - Let it sink in]**

**The Quote (on slide):**
> "And you know what? Phil Karlton was absolutely right about this: 'There are only two hard things in Computer Science: cache invalidation, naming things, and off-by-one errors.'
>
> Caching is one of those fundamental problems we deal with constantly, yet it remains surprisingly complex.
>
> Today, we're going to tackle one of those hard problems—and I want to show you a different way of thinking about it."

**Key Message:** Cache stampedes are universal, language-agnostic operational challenges

---

### II. The Local Cache Context (2.5 minutes)

#### The Scenario (90 sec)

Modern applications serve data through APIs:
- Product catalogs, user permissions, configuration
- Need: **Microsecond latency** for millions of requests/sec
- Solution: **In-memory caching**

**The "Redis is Overkill" Dataset:**
- **Size:** 100 MB to 5 GB
- **Access:** Read-heavy (100:1 ratio)
- **Updates:** Every few minutes to hours

**Why NOT distributed cache?**
- Network latency: 1-5ms vs local <1μs (**1000x difference**)
- Infrastructure complexity: clusters, failover, monitoring
- Operational overhead: another system to manage

**Decision: Local in-memory cache makes sense**

Great JVM libraries: Caffeine, Guava Cache, Ehcache
- TTL, eviction policies
- Thread-safe
- Production-proven

**But... operational challenges persist at GB scale**

#### The Breaking Point (60 sec)

> "Here's the irony: these challenges matter most exactly where local caching matters most—gigabyte-scale datasets with millions of accesses per second."

**Visual:** Decision matrix showing local cache sweet spot

---

### III. The Operational Challenge (1.5 minutes)

**One core problem with two painful symptoms:**

#### The Cold Start + Reload Problem (90 sec)

```
Scenario: Service restart, deployment, scale-up

Option A: Load on-demand
├─ Cache stampede on first requests
├─ Source system overwhelmed
└─ Response times: seconds instead of microseconds

Option B: Pre-warm cache
├─ Query all data from source
├─ 2 GB catalog = 3-5 minutes to load
└─ Service unavailable during warmup

Regular Updates (every 10 min):
├─ Build new cache while serving traffic
├─ Memory: Old (2 GB) + New (2 GB) = 4 GB spike!
├─ Create millions of objects → promoted to old gen
├─ Full GC triggered → 200-500ms pause
└─ This happens every update cycle!

The Impact:
├─ Every deployment: minutes of downtime
├─ Every update: memory spike + GC pause
├─ Auto-scaling: new instances slow to be useful
└─ User-facing latency spikes
```

**Visual:** Memory graph showing sawtooth pattern with GC pauses

**Key Message:** "The problem isn't the cache library—it's the reload pattern itself."

---

### IV. The Paradigm Shift: Versioned Datasets (2.5 minutes)

#### Reframing (45 sec)

```
What's great about Redis?
├─ One system updates data
├─ All instances see changes
└─ Single source of truth

What we don't want:
├─ Network latency
└─ Infrastructure complexity

The Question:
└─ What if one process prepared data, published it,
   and all instances consumed it locally?

   "Centralized preparation, decentralized serving"
```

**Important:** "This isn't a silver bullet—but for specific characteristics, it's transformative."

#### The Git Analogy (90 sec)

```
Git for Code → Same Pattern for Data

Git has:
├─ Commits: Complete snapshots (immutable)
├─ Diffs: Just the changes
└─ Only transfers differences

Applied to Datasets:

Version 1:
└─ Complete dataset: 10M products, 2 GB

Version 2 (Delta from V1):
├─ Added: 5K products
├─ Removed: 3K products
├─ Modified: 10K products
├─ Unchanged: 9,982K products (NOT transmitted!)
└─ Delta: 125 MB instead of 2 GB!

Consumer Process:
├─ Starts at V1 (2 GB in memory)
├─ Downloads delta (125 MB)
├─ Applies changes in-place
├─ Now at V2 (still 2 GB in memory)
└─ No full reload!
```

**Visual:** Git commit graph adapted for data

#### Why This Solves Our Problems (30 sec)

```
✅ Cold starts: Load pre-built snapshot (fast)
✅ Updates: Apply small deltas (not full reload)
✅ Memory: In-place updates (no 2x spike)
✅ GC: Memory pooling (no mass allocations)
```

**Transition:** "This is exactly what Hollow does. Let me show you..."

---

### V. Hollow in Action (8 minutes) ⭐ **CENTERPIECE**

#### What is Hollow? (20 sec)

```
Hollow: Open-source dataset distribution system
├─ Netflix OSS (2016), Apache 2.0
├─ Producer-consumer architecture
└─ Production-proven at scale
```

#### Architecture: The Decoupling (60 sec)

```
Data Source (DB, API, Files)
         ↓ query once
Producer (Batch Job)
├─ Runs periodically (e.g., every 10 min)
├─ Queries source, processes data
├─ Calculates diffs automatically
└─ Creates snapshot + delta
         ↓ publish
Blob Store (S3, NFS, Filesystem) ⭐ FLEXIBLE
├─ Stores versioned snapshots
├─ Stores deltas
└─ Distribution point
         ↓ pull/watch
Consumers (Your Applications)
├─ Load data into local memory
├─ Apply deltas automatically
├─ Microsecond access, zero network calls
└─ Multiple instances stay synchronized

Key: Producer and consumer NEVER talk directly → DECOUPLED
```

**Visual:** Large diagram highlighting "DECOUPLED" and "FLEXIBLE"

#### Demo Walkthrough (6.5 minutes)

**Setup (15 sec):**
"Movie catalog demo—thousands of movies. Producer runs every 2 minutes. Watch how they communicate through blobs only."

##### Data Model (20 sec)

```kotlin
@HollowPrimaryKey(fields = ["id"])
data class Movie(
    val id: Long,
    val title: String,
    val genre: String,
    val rating: Double,
    val releaseYear: Int,
    val duration: Int
)
```

**Point:** "Hollow generates type-safe APIs and optimized storage from this."

##### Producer (45 sec)

```kotlin
@Scheduled(fixedDelay = 120000)
fun publishData() {
    producer.runCycle { writeState ->
        currentMovies.forEach { movie ->
            writeState.add(movie)  // ALL movies
        }
    }
}
```

**Key:** "You give ALL data. Hollow calculates the diff automatically."

**Terminal:**
```log
[INFO] Producer cycle #2 at 2025-11-15T07:10:46
[INFO] Dataset: 51,200 movies
[INFO] Completed in 152ms
```

##### The Blob Store - KEY MOMENT (90 sec)

```bash
$ ls -lh /Users/vranganathan/vish/hollowdemo/hollow-data/

Snapshots (complete dataset):
snapshot-20251115070935001    10M    # V1: 50K movies
snapshot-20251115071046002    11M    # V2: 51K movies
snapshot-20251115071050003    11M    # V3
snapshot-20251115071054004    12M    # V4

Deltas (only what changed):
delta-...935001-...046002     623K   # V1 → V2 delta
delta-...046002-...050003     645K   # V2 → V3 delta
delta-...050003-...054004     666K   # V3 → V4 delta
```

**BIG MOMENT:** "**10 megabytes** for complete snapshot. **623 kilobytes** for delta. The delta is **16 times smaller** because it only contains what changed—maybe 1,200 movies added, some ratings updated. The other 48,800 movies? Not included."

**Scale Up:** "Production scale: 2 GB catalog with 0.18% change rate = 125 MB delta instead of 2 GB reload. **16x smaller**. With 10 instances, that's 90% less network traffic."

**Storage:** "These use Hollow's optimized columnar encoding—not just gzipped JSON."

##### Consumer (45 sec)

```kotlin
consumer = HollowConsumer
    .withBlobRetriever(blobRetriever)
    .withAnnouncementWatcher(announcementWatcher)
    .withGeneratedAPIClass(MovieAPI::class.java)
    .build()

consumer.triggerRefresh()  // Initial load
movieIndex = Movie.uniqueIndex(consumer, "id")
```

**Terminal:**
```log
[INFO] Initializing MovieConsumer...
[INFO] ✅ Update successful: v...001
[INFO] Primary key index created (O(1))
```

##### Auto-Update (45 sec)

**Terminal:**
```log
[INFO] 🔄 Update started: v...001 → v...002
[INFO] Downloading delta (623 KB)...
[INFO] DELTA APPLIED IN 4ms
[INFO] ✅ Update successful
```

**EMPHASIS:** "**4 milliseconds.** Downloaded 623 KB, applied in-place. Zero downtime. Queries kept working the whole time. Atomic swap to new version. No memory spike, no GC pause."

##### Query Performance (30 sec)

```bash
$ curl localhost:8081/movies/stats
```

```json
{
  "totalMovies": 51200,
  "currentVersion": 20251115071046002,
  "genreBreakdown": {...},
  "averageRating": 7.35
}
```

**Point:** "Version **20251115071046002**—Git-style identifier. Every consumer knows its exact version. Queries are **microseconds**—O(1) indexes in local memory."

##### Type-Safe APIs (30 sec)

```java
MovieAPI api = (MovieAPI) consumer.getAPI();

// Generated type-safe APIs
for (HollowMovie movie : api.getAllHollowMovie()) {
    String title = movie.getTitle().getValue();
}

// Or use indexes (preferred)
Movie movie = movieIndex.findMatch(12345L);
```

**Benefits:** Compile-time safety, IDE autocomplete, zero reflection

##### Performance Comparison (60 sec)

```
┌─────────────────────┬──────────────┬─────────────────┐
│ Metric              │ Traditional  │ Hollow          │
├─────────────────────┼──────────────┼─────────────────┤
│ Cold Start          │ 3-5 min      │ 10-15 sec (20x) │
│ Update Time         │ 90 sec       │ 4 ms (22,500x)  │
│ Memory Peak         │ 4 GB (2x!)   │ 2.01 GB (flat)  │
│ GC Pause (P99)      │ 200-500ms    │ <5ms (50x)      │
│ Update Downtime     │ ~90 sec      │ 0 sec           │
│ DB Load (10 nodes)  │ 10x/cycle    │ 1x/cycle (90%)  │
└─────────────────────┴──────────────┴─────────────────┘
```

**Visual:** Memory graphs side-by-side (sawtooth vs flat)

**Message:** "Dramatic improvements across ALL operational metrics."

#### Summary (15 sec)

"Simple code, powerful results:
- **Decoupled** producer/consumer
- **Flexible** blob store (S3, NFS, filesystem)
- **Type-safe** generated APIs
- **16x compression** on deltas
- **Zero downtime** updates"

---

### VI. Production & When to Use (2 minutes)

#### Production Essentials (45 sec)

**Blob Cleanup:**
```java
HollowProducer.BlobStorageCleaner cleaner =
    new HollowFilesystemBlobStorageCleaner(publishDir, 10);
```
"Keep last 10 snapshots. New consumers need complete snapshots. Existing consumers use deltas."

**Schema Evolution:**
- Adding fields: ✅ Backward compatible
- Removing fields: ⚠️ Deploy consumers FIRST, then producer
- Type changes: ❌ Breaking—use new field name (e.g., priceV2)

"Schema evolution is your biggest operational risk. Plan carefully!"

#### When to Use (75 sec)

**✅ Sweet Spot:**
```
Dataset: 100 MB - 50 GB (sweet spot: 1-10 GB)
Access: Read-heavy (100:1 ratio)
Updates: Minutes to hours, not seconds
Pain: Cold starts, memory spikes, GC pauses
```

**Real examples:** Product catalogs, user permissions, content metadata, pricing tables, config data

**❌ When NOT to use:**
```
• Dataset < 50 MB (traditional libraries work fine)
• Dataset > 100 GB (too large for single node)
• Write-heavy workloads (Hollow optimized for reads)
• Real-time consistency needed (eventual consistency inherent)
• Single instance (benefits less compelling)
```

**Emphasis:** "NOT a silver bullet. Transformative for specific characteristics."

**Quick Check:**
```
1. Dataset 100 MB - 50 GB?
2. Experiencing cold start pain?
3. Cache reloads cause memory/GC issues?
4. Can tolerate minute-level updates?
5. Need microsecond read latency?

4-5 yes → Strong candidate
0-1 yes → Stick with traditional
```

---

### VII. Conclusion (1 minute)

#### Key Takeaways (45 sec)

```
1. Local caches right choice for GB-scale data
   └─ But traditional approaches have operational costs

2. Versioned datasets solve these challenges
   └─ Cold starts 20x faster
   └─ Updates in milliseconds
   └─ No memory spikes or GC pauses

3. Decoupling is the key innovation
   └─ Producer separate from consumer
   └─ Blob store provides flexibility

4. Hollow: production-proven since 2016
   └─ Open source, type-safe, operationally simple
```

#### Getting Started (15 sec)

```
• Docs: hollow.how
• GitHub: github.com/Netflix/hollow
• Timeline: Prototype in afternoon, production in week
```

**Final Thought:**
> "Local caching at GB scale doesn't require operational headaches. By versioning datasets like Git, we get both performance and simplicity—for the right use case. Now go figure out what to name your variables!"

---

## Q&A Preparation (5 minutes)

### Anticipated Questions:

**Q: "How does this compare to ConcurrentHashMap?"**
A: ConcurrentHashMap works for small datasets but lacks efficient updates, memory management, distribution mechanism, and tooling. Hollow provides all these for GB-scale.

**Q: "What if producer fails?"**
A: Consumers serve last good version. No disruption. Data stales but service stays up. Monitor producer health and alert on staleness.

**Q: "Can consumers load subset of data?"**
A: Yes! Type-level and record-level filtering. Example: Load only products for specific regions.

**Q: "Multi-datacenter deployments?"**
A: S3 cross-region replication, regional producers, or CDN in front of blob store.

**Q: "Schema evolution details?"**
A: Adding fields is backward compatible. Removing requires consumer updates first. Type changes need new field. This is biggest operational risk—coordinate carefully!

**Q: "Performance overhead?"**
A: Minimal. Snapshots compressed (~50% of RAM). Network transfers only deltas after initial. CPU for delta application very efficient. Benefits far outweigh costs at GB scale.

**Q: "Consistency guarantees?"**
A: Eventually consistent. All consumers eventually see same version. Update lag typically seconds to minutes. Not suitable for strong consistency requirements.

---

## Timing Summary

| Section | Target | Content |
|---------|--------|---------|
| Opening | 0.5 min | 3 AM incident + Phil Karlton quote |
| Context | 2.5 min | API world + "Redis is overkill" |
| Challenge | 1.5 min | Cold starts + Memory/GC combined |
| Insight | 2.5 min | Reframe + Git analogy |
| Hollow Demo | 8 min | Architecture + Live demo + Performance |
| Production & When | 2 min | Essentials + Decision framework |
| Conclusion | 1 min | Takeaways + Resources |
| **TOTAL** | **18 min** | **+ 2 min buffer = 20 min** |
| Q&A | 5 min | |

---

## Speaker Notes

### Pacing:
- **First 4 min:** Problem (conversational)
- **Middle 11 min:** Insight + Demo (engaging, centerpiece)
- **Last 3 min:** Application (concise, practical)

### Key Emphasis Points:
1. **3 AM story** - Emotional connection
2. **Architecture** - Point to "DECOUPLED"
3. **File sizes** - "10 MB vs 623 KB = 16x" - PAUSE
4. **4ms update** - "Zero downtime, no spike"
5. **Performance table** - Let numbers speak
6. **"Not a silver bullet"** - Realistic expectations

### Engagement:
- Opening pain story
- "Stop thinking caching" paradigm shift
- File size reveal - surprise factor
- Performance comparison - proof

### If Short on Time:
- Condense production (45 sec → 30 sec)
- Brief schema evolution
- Keep demo at 8 min!

### If Extra Time:
- Show S3 bucket structure
- Discuss Netflix use cases
- Deeper on type-safe APIs

---

**Demo Data:** `/Users/vranganathan/vish/hollowdemo/hollow-data`
**Key Numbers:** 10 MB snapshot, 623 KB delta, 16x compression

**Last Updated:** 2025-01-14
