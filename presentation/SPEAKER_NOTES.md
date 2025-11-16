# Versioned Datasets: Rethinking Local In-Memory Caches
## Speaking Script - ACM Fremont Chapter

**Event:** ACM Fremont Chapter - Emerging Technologies Tech Talk
**Date:** November 16, 2025
**Duration:** 20 minutes + 5 minutes Q&A
**Speaker:** Vish Ranganathan

---

## Section I: Opening - The Cache Stampede (0.5 minutes)

### The Story

Let me start with a story that might sound familiar.

It's 2 PM on a Monday. Everything's running smoothly. Your cache just expired—it's been serving data for the past hour, and now it's time to refresh.

But here's what happens: All 50 of your service instances realize the cache is empty at the exact same moment. They all race to reload it.

50 simultaneous queries hit your database asking for the same 2 GB of data. Your database grinds to a halt. Response times go from milliseconds to minutes. Users start seeing errors.

By the time you realize what's happening, you're in the middle of a full outage.

Sound familiar?

**[PAUSE - Let it sink in]**

---

### The Quote

**[Show Phil Karlton quote on slide]**

And you know what? Phil Karlton was absolutely right about this quote: "There are only two hard things in Computer Science: cache invalidation, naming things, and off-by-one errors."

Caching is one of those fundamental problems we deal with constantly, yet it remains surprisingly complex.

Today, we're going to tackle one of those hard problems—and I want to show you a different way of thinking about it.

---

## Section II: The Local Cache Context (2.5 minutes)

### The Scenario (90 sec)

Let's set the context. In modern distributed systems, data is typically served through APIs. You might have a product catalog, user profiles, configuration data—and multiple services all depend on that same dataset.

The need is simple: microsecond latency for high-frequency access. When you're handling millions of requests per second, every millisecond matters. That's where in-memory caching becomes essential.

But here's the thing—not every caching problem needs a distributed solution.

Let me paint a specific scenario. Your dataset characteristics look like this:
- Size: somewhere between 100 megabytes and 5 gigabytes
- Access pattern: heavily read-dominated—think 100 reads for every 1 write, maybe even higher
- Update frequency: you're refreshing the data every few minutes to a few hours

Now, you could reach for Redis or Memcached. But consider the trade-offs:
- Network latency—even on a fast network, you're looking at 1 to 5 milliseconds per call. Compare that to local memory access at under a microsecond. That's a thousand times difference.
- Infrastructure complexity—clusters, failover, monitoring, replication
- Operational overhead—it's another system to manage, patch, scale

And honestly, for this scale, the risk often outweighs the reward.

So the decision makes sense: use a local in-memory cache. There are excellent libraries like Caffeine, Guava Cache, and Ehcache that are production-proven. They handle TTL, eviction policies, thread-safety—all the cache invalidation patterns work well.

But operational challenges persist, especially when your dataset is multiple gigabytes.

---

### The Breaking Point (60 sec)

**[Show decision matrix slide]**

Here's the irony: these challenges matter most exactly where local caching matters most—gigabyte-scale datasets with millions of accesses per second.

---

## Section III: The Operational Challenge (1.5 minutes)

### The Cold Start + Reload Problem (90 sec)

Let me show you the core problem. It has two painful symptoms.

Think about this scenario: you deploy a new version of your service, or an instance restarts, or you're auto-scaling up during a traffic spike.

Your cache starts completely empty.

Now you have two options, neither of them great:

**Option A: Load on-demand**—The first requests cause cache misses, stampeding your source system. Response times spike from milliseconds to seconds.

**Option B: Pre-warm the cache**—Query all the data from your source before serving traffic. If you've got a 2 gigabyte catalog, you're looking at 3 to 5 minutes to load everything. During that time? Your service is effectively unavailable.

And this isn't a one-time problem. Every deployment, every scale-up event, every incident recovery—you're waiting minutes for caches to warm up.

But there's more. Your regular updates—let's say every 10 minutes—have their own problem.

The common pattern: build a completely new cache instance, then atomically swap it with the old one.

**[Show memory graph slide]**

Look at the memory timeline:
- T=0: Old cache sitting at 2 gigabytes
- T=30s: Building new cache while old one serves traffic = 4 gigabytes total—a 2x memory spike
- T=90s: Swap the reference, old cache becomes garbage
- T=120s: After full GC, back down to 2 gigabytes

This 2x memory spike happens every 10 minutes like clockwork.

And that triggers something worse: when you create millions of objects, they get promoted to old generation. Old gen fills up. Full GC triggers. We're talking 200 to 500 millisecond pauses. Every reload cycle.

The impact? Service latency spikes. Request timeouts. User-facing degradation.

The problem isn't the cache library—it's the reload pattern itself.

---

## Section IV: The Paradigm Shift (2.5 minutes)

### Reframing (45 sec)

So what if we approached this from a completely different angle?

Let's think about this differently.

You know what's great about distributed caches like Redis? One system updates the data, and all your application instances see the change. Single source of truth. No coordination needed.

But we don't want the operational overhead—cluster management, network latency, infrastructure complexity.

So here's the question: What if one process prepared the data, published it, and all your application instances consumed it locally? No network calls for reads, but everyone stays synchronized.

This is what I call centralized preparation, decentralized serving.

Now, this isn't a silver bullet for all caching problems. But for a specific set of characteristics—which we'll talk about later—it's transformative.

---

### The Git Analogy (90 sec)

This model mirrors Git perfectly.

Git has commits—complete snapshots—and diffs—just the changes. Commits are immutable. Git only transfers differences.

Apply this to datasets:

**Version 1:** Complete dataset—10 million products, 2 gigabytes.

**Version 2:** Delta from V1—5K added, 3K removed, 10K modified. The key: 9,982,000 products unchanged. Your delta is 125 megabytes, not 2 gigabytes.

You start at Version 1 (2 GB in memory), download the delta (125 MB), apply changes in place, and you're at Version 2 (still 2 GB). No full reload.

**[Show Git commit graph adapted for data]**

---

### Why This Solves Our Problems (30 sec)

**Cold starts?** Load pre-built snapshots—much faster than database queries.

**Updates?** Apply small deltas—only what changed.

**Memory spikes?** In-place updates—no 2x memory footprint.

**GC pressure?** Memory pooling—no mass object creation.

You get local access performance with centralized data preparation simplicity.

This is exactly what Hollow does. Let me show you...

---

## Section V: Hollow in Action (8 minutes) ⭐ **CENTERPIECE**

### What is Hollow? (20 sec)

This pattern—versioned dataset distribution—is implemented in an open-source library called Hollow.

Hollow came out of Netflix in 2016. It's Apache 2.0 licensed, production-proven at scale, and specifically built for this use case. It's not a cache library—it's a dataset distribution system.

---

### Architecture: The Decoupling (60 sec)

**[Show architecture diagram]**

It's a producer-consumer model:

At the top, you have your data source—database, API, files, whatever.

The producer is a batch job that runs periodically—say, every 10 minutes. It queries your source, and here's the magic: it automatically calculates diffs from the previous version. You don't write diff logic. You just give it all the data, and Hollow figures out what changed.

It publishes snapshots for new consumers and deltas for existing ones to a blob store—S3, filesystem, NFS, HTTP endpoint—whatever you want.

Your applications are the consumers. They load data into local memory, watch for new versions, and automatically apply deltas when available.

**[Point to diagram]** Notice how the producer and consumer never talk to each other directly. They're completely decoupled. The blob store can be anything—S3 in production, shared NFS mount, even an HTTP endpoint. This flexibility is key to the pattern's power.

Result: microsecond access with zero network calls. Multiple instances stay synchronized. Data loading completely decoupled from data serving.

---

### Demo Walkthrough (6.5 minutes)

**Introduction (15 sec):**

Now let me show you this in action with a working demo. I've got a movie catalog with thousands of movies. The producer runs every 2 minutes, simulating real-world changes. The key thing to watch: producer and consumer are separate processes communicating only through published blobs.

---

#### Data Model (20 sec)

**[Show code slide]**

First, you define your data model with simple classes:

```kotlin
@HollowPrimaryKey(fields = ["id"])
data class Movie(...)
```

That `@HollowPrimaryKey` annotation tells Hollow this is our unique identifier. From this simple class, Hollow generates an optimized storage format and type-safe APIs. You get compile-time safety, IDE autocomplete, and zero reflection overhead.

---

#### Producer: Creating Versions (45 sec)

**[Show producer code]**

Here's the producer code—this is the batch job that creates snapshots:

```kotlin
@Scheduled(fixedDelay = 120000)
fun publishData() {
    producer.runCycle { writeState ->
        currentMovies.forEach { movie ->
            writeState.add(movie)
        }
    }
}
```

Notice something important: you give Hollow **all** the data, not just what changed. You don't write any diff logic. Hollow automatically compares to the previous version and creates the delta for you.

**[Show producer terminal output]**

Here's what the producer output looks like:

```log
[INFO] Producer cycle #2 at 2025-11-15T07:10:46
[INFO] Dataset: 51,200 movies
[INFO] Completed in 152ms
```

152 milliseconds to process over 51,000 movies and calculate what changed. Fast.

---

#### The Blob Store - KEY MOMENT (90 sec)

**[Show file listing slide]**

Now let me show you what's actually written to disk. This is the key moment:

```bash
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

**[Emphasis]** Look at these numbers: **10 megabytes** for a complete snapshot. **623 kilobytes** for the delta.

**[PAUSE - Let that sink in]**

The delta is **16 times smaller** because it only contains what changed—maybe 1,200 movies added, some ratings updated. The other 48,800 movies? Not included at all.

**[Show key numbers slide - make it huge: "10 MB vs 623 KB = 16x"]**

Now imagine this at production scale. You have a 2 gigabyte product catalog with a typical 0.18% change rate per update. Your delta would be around 125 megabytes instead of reloading 2 gigabytes. That's still 16 times smaller transfer.

And it compounds—if you have 10 instances pulling updates, that's 90% less network traffic, 90% less load on your blob store.

**[Quickly mention]** These blobs use Hollow's optimized columnar encoding—highly compressed, designed for efficient memory layout. This isn't just gzipped JSON; it's a format optimized for this exact use case.

---

#### Consumer: Loading and Querying (45 sec)

**[Show consumer code]**

On the consumer side:

```kotlin
consumer = HollowConsumer
    .withBlobRetriever(blobRetriever)
    .withAnnouncementWatcher(announcementWatcher)
    .withGeneratedAPIClass(MovieAPI::class.java)
    .build()

consumer.triggerRefresh()
movieIndex = Movie.uniqueIndex(consumer, "id")
```

The consumer loads the initial snapshot—just downloads that 10 MB file and unpacks it into memory. Then it watches for new versions. When the producer publishes, the consumer automatically detects it and applies the delta. All automatic.

**[Show consumer terminal]**

```log
[INFO] Initializing MovieConsumer...
[INFO] ✅ Update successful: v...001
[INFO] Primary key index created (O(1))
```

---

#### Auto-Update Process (45 sec)

**[Show consumer auto-update terminal]**

When the producer publishes cycle #2:

```log
[INFO] 🔄 Update started: v...001 → v...002
[INFO] Downloading delta (623 KB)...
[INFO] DELTA APPLIED IN 4ms
[INFO] ✅ Update successful
```

**[BIG EMPHASIS]** **4 milliseconds.** Downloaded 623 KB, applied the delta in-place in 4 milliseconds.

During those 4 milliseconds, queries were still working against the previous version. Then—atomic pointer swap—now serving the new version.

**No memory spike, no GC pause, zero downtime.**

**[Contrast]** Compare that to traditional approach: 90 seconds to reload, 4 GB memory spike, 500ms GC pause. This: 4 milliseconds, flat memory.

---

#### Query Performance (30 sec)

**[Show query slide]**

The consumer exposes a REST API. Let me query it:

```bash
$ curl localhost:8081/movies/stats
```

```json
{
  "totalMovies": 51200,
  "currentVersion": 20251115071046002,
  ...
}
```

See that version number? **20251115071046002**—that's our Git-style version identifier. Every consumer knows exactly what version they're serving.

And the queries are fast—we're talking microseconds because it's all in local memory with O(1) primary key indexes.

```bash
$ curl localhost:8081/movies/12345
```

Direct memory access—microsecond latency. No network calls, no serialization overhead.

---

#### Type-Safe APIs (30 sec)

**[Show type-safe API code]**

One more thing about those generated APIs:

```java
MovieAPI api = (MovieAPI) consumer.getAPI();

for (HollowMovie movie : api.getAllHollowMovie()) {
    String title = movie.getTitle().getValue();
}

Movie movie = movieIndex.findMatch(12345L);
```

This isn't magic reflection or runtime proxies. Hollow generates actual Java classes at build time. Your IDE knows about these types, the compiler checks them, and there's zero reflection overhead at runtime.

---

#### Performance Comparison (60 sec)

**[Show comparison table]**

Let me summarize what we just saw with side-by-side comparison:

```
Traditional Approach:
• Cold Start: 3-5 minutes
• Update Time: 90 seconds
• Memory Peak: 4 GB (2x spike!)
• GC Pause: 200-500ms
• Update Downtime: ~90 seconds
• DB Load with 10 nodes: 10x per cycle

Hollow Approach:
• Cold Start: 10-15 seconds (20x faster)
• Update Time: 4 milliseconds (22,500x faster)
• Memory Peak: 2.01 GB (flat, no spike)
• GC Pause: <5ms (50x better)
• Update Downtime: 0 seconds
• DB Load: 1x per cycle (90% reduction)
```

**[Show memory graphs side-by-side]** Traditional: sawtooth pattern. Hollow: flat line.

Dramatic improvements across ALL operational metrics. This is the operational win.

---

#### Demo Summary (15 sec)

Simple code, powerful results:
- Producer and consumer **completely decoupled**
- Blob store **flexible**—S3, NFS, filesystem, HTTP
- Type-safe APIs **generated automatically**
- **16x compression** on deltas in our demo
- **Zero downtime** updates in milliseconds

---

## Section VI: Production & When to Use (2 minutes)

### Production Essentials (45 sec)

#### Blob Cleanup

First thing: storage management.

Every producer cycle writes a new snapshot. If you're publishing every 10 minutes, that's 144 snapshots per day. For a 10 MB dataset like our demo, that's manageable, but it adds up.

You configure cleanup directly on the producer:

```java
HollowProducer.BlobStorageCleaner cleaner =
    new HollowFilesystemBlobStorageCleaner(publishDir, 10);
```

That `10` parameter means "keep the last 10 snapshots." The cleaner runs automatically after each producer cycle.

**Important point**: The producer writes a new snapshot every cycle because new consumers need a complete dataset to start from. But existing consumers only download the small deltas.

---

#### Schema Evolution

Data models change. How do you handle it?

**Adding fields**—Backward compatible. Producer can start publishing new fields immediately. Old consumers ignore them.

**Removing fields**—Requires coordination:
1. First: Deploy all consumers with updated schema (field removed)
2. Then: Update producer to stop publishing that field
3. Never remove from producer first—you'll break old consumers

**Field type changes**—Breaking change. Consider it a new field instead (e.g., `priceV2`), migrate consumers, then eventually remove old field.

**[Emphasis]** Schema evolution is your biggest operational risk. Plan changes carefully and deploy in the right order.

---

### When to Use (75 sec)

#### The Sweet Spot

This pattern works best when you have:

**Dataset size: 100 MB to 50 GB**—Sweet spot is 1 to 10 gigabytes. Below 100 MB, traditional libraries work fine. Above 50 GB, consider distributed caching or sharding.

**Read-heavy access patterns**—100:1 read-to-write ratio or higher. Updates every few minutes to hours, not seconds.

**Operational pain**—Slow cold starts, memory spikes during refresh, GC pauses, multiple application instances.

**Real-world examples:** Product catalogs, user permissions, content metadata, pricing tables, configuration data, reference data like geographic info or taxonomies.

---

#### When NOT to Use

**Under 50 MB**—Traditional cache libraries work great. Overhead isn't justified.

**Over 100 GB**—Too large for single-node memory.

**Real-time consistency**—Hollow is eventually consistent, minute-level cadence. Need sub-second updates? Not the right pattern.

**Write-heavy workloads**—Hollow is optimized for reads.

**Single instance**—Benefits are less compelling with just one instance.

**[Emphasis]** This is NOT a silver bullet. It's transformative for specific characteristics: GB-scale, read-heavy, minute-level update cadence. Use the right tool for your specific problem.

---

#### Quick Decision Aid

Ask yourself these five questions:
1. Is my dataset between 100 MB and 50 GB?
2. Am I experiencing cold start pain?
3. Do cache reloads cause memory or GC issues?
4. Can I tolerate minute-level update delays?
5. Do I need microsecond read latency?

If you answered yes to 4-5 of these, you're a strong candidate. 2-3 yes, worth considering. 0-1 yes, stick with traditional approaches.

---

## Section VII: Conclusion (1 minute)

### Key Takeaways (45 sec)

Let me wrap up with the key takeaways:

**First:** Local caches are still the right choice for GB-scale data with microsecond latency requirements. The pattern we've discussed doesn't replace local caching—it makes it operationally viable.

**Second:** Versioned datasets solve the operational problems. Cold starts 20x faster. Updates in milliseconds, not minutes. No memory spikes. No GC pauses. Zero downtime.

**Third:** Decoupling is the key innovation. Producer separate from consumer. Blob store provides flexibility—S3, NFS, filesystem, HTTP. Each component does one thing well.

**Fourth:** Hollow provides a production-proven implementation. It's been running at Netflix since 2016. Open source, type-safe APIs generated automatically, simple code with powerful operational benefits.

---

### Getting Started (15 sec)

Resources:

Documentation: **hollow.how**—excellent getting started guide.

GitHub: **github.com/Netflix/hollow**—Apache 2.0 licensed, active community.

You can have a working prototype in an afternoon. Production-ready implementation within a week.

---

### Final Thought

Local in-memory caching at GB scale doesn't have to come with operational headaches. By rethinking caches as versioned datasets, we can have both performance and operational simplicity—when the use case fits.

Remember Phil Karlton's quote? We've tackled cache invalidation by versioning it away. Now you just need to figure out what to name your variables.

Good luck with that!

I'm happy to take questions.

---

## Q&A Preparation (5 minutes)

### Anticipated Questions:

**Q: "How does this compare to just using ConcurrentHashMap?"**

Good question. ConcurrentHashMap works for smaller datasets, but lacks several things you need at GB scale:
- Efficient update mechanism (you still face full reload issues)
- Memory management (no pooling, GC pressure remains)
- Distribution mechanism (each instance implements separately)
- Tooling and observability (versioning, rollback, etc.)

Hollow provides all of these specifically optimized for GB-scale data.

---

**Q: "What about schema evolution? How do you handle breaking changes?"**

Hollow handles schema evolution, but you need to be careful. This is your biggest operational risk.

Adding fields is backward compatible—producer can add fields immediately, old consumers ignore them, deploy new consumers when ready.

Removing fields requires coordination—deploy all consumers with the field removed FIRST, then update producer to stop publishing it. Never remove from producer first.

Type changes are breaking—treat as a new field (e.g., priceV2), migrate consumers to new field, remove old field later.

Plan carefully, deploy in the right order, test thoroughly.

---

**Q: "What if the producer fails or gets stuck?"**

Consumers continue serving the last good version. No disruption to running applications. Data becomes stale, but service stays up. Queries keep working normally.

Once producer recovers, consumers catch up automatically.

Best practices: Monitor producer health, alert on version staleness (e.g., if data is more than 30 minutes old), consider producer redundancy for critical data.

---

**Q: "Can consumers load only a subset of data?"**

Yes! Hollow supports filtering at multiple levels.

Type-level filtering: Load only certain object types. Example: Load movies but not reviews.

Record-level filtering: Custom logic to filter records. Example: Load only products for specific regions, or only active users.

This is very useful for reducing memory footprint, multi-tenant scenarios, or regional deployments.

---

**Q: "How do you handle multi-datacenter or multi-region deployments?"**

Several approaches work well:

Option 1: Replicate blob store—S3 cross-region replication is automatic. Each region reads from local S3 bucket. Producer publishes to one region.

Option 2: Regional producers—each region has its own producer publishing to regional blob store. Good for data sovereignty requirements.

Option 3: CDN—put CDN in front of blob store. Consumers pull from CDN edge. Works well for HTTP-based blob stores.

Consumers just poll their regional or closest endpoint.

---

**Q: "What's the performance overhead of versioning?"**

Minimal—benefits far outweigh costs at GB scale:

Disk space: Snapshots are compressed, about 50% of RAM size. 10 MB in memory becomes ~5 MB on disk. With cleanup, very manageable.

Network: Initial load is one snapshot download. Updates are only deltas, 16 to 500 times smaller. With 10 instances, that's massive network savings.

CPU: Delta application is very efficient, milliseconds. Memory pooling reduces allocation overhead.

Memory: Compact encoding, optimized layout. Often smaller than equivalent Java objects. No duplicate strings, efficient primitives.

At GB scale, the operational benefits massively outweigh these minimal costs.

---

**Q: "What about consistency guarantees between consumers?"**

Eventually consistent model.

Guarantees: All consumers eventually see same version. Updates are atomic within each consumer. No torn reads.

Non-guarantees: Exact timing not guaranteed. Consumers may be at different versions temporarily. Update lag typically seconds to minutes.

If you need immediate consistency, linearizability, or strong consistency—wrong pattern. Use a database or distributed cache.

This pattern is for read-heavy workloads where minute-level eventual consistency is acceptable and operational simplicity matters more than perfect consistency.

---

**Last Updated:** 2025-01-14
**File:** `/Users/vranganathan/vish/vish-profile/speaking/hollow-talk/talk-script-revised.md`
