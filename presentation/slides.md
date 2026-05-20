---
theme: dracula
title: "Cache Me If You Can: Decentralize Your Distributed Caches With Hollow"
info: |
  ## Cache Me If You Can: Decentralize Your Distributed Caches With Hollow
  Linux Foundation Open Source Summit North America 2026
  Speaker: Viswanathan Ranganathan (Vish)
class: text-center
transition: slide-left
mdc: true
duration: 40min
highlighter: shiki
fonts:
  sans: 'Verdana'
  serif: 'Verdana'
  mono: 'Verdana'
---

# Cache Me If You Can

## Decentralize Your Distributed Caches With Hollow

**Viswanathan Ranganathan (Vish)**

<img src="/images/osslf.png" alt="Open Source Summit NA 2026" style="height: 72px; margin: 2rem auto 0; display: block;" />

<!--
Welcome slide. Let it breathe while the audience settles.
-->

---
layout: center
---

# What Made Me Think Of This Talk?

<!--
Opening: "Let me tell you about a problem that sounds simple until you actually have to solve it."
-->

---
layout: center
class: text-center
---

# Account Metadata

A lookup given an account id.

**Don't make the accounts service your bottleneck.**

<!--
Beat 1+2: We were building a high-throughput service. Each request needed one thing: account metadata — a lookup given an account id. The accounts service had an API for this. Easy. But at that scale, hitting it directly felt like a lot of load for what was conceptually a trivial lookup. We didn't want to make it our bottleneck.
-->

---
layout: center
class: text-center
---

# What About Redis?

A new cluster to operationalize, manage, and maintain.

<!--
Beat 3: We thought about a distributed cache. Redis. But that felt like over-engineering — a new cluster that needs to be operationalized, managed, and maintained ongoing. Not worth it for what was conceptually a trivial lookup.
-->

---
layout: center
class: text-center
---

# The Obvious Plan

Preload at startup. Fetch on miss.

Simple enough in theory.

<!--
Beat 4: The obvious solution: paginate through the accounts list endpoint at startup, preload everything into memory, then fetch individual entries from the API as they expire. In-memory for the hot path, API for misses. Simple enough in theory.
-->

---
layout: center
class: text-center
---

# Two Code Paths. One Problem. Forever.

Bootstrap ↔ Runtime. Same data. Two mechanisms.

<!--
Beat 5: But when we mapped it out — bulk preload at bootstrap, individual fetches at runtime, coordinated expiry across nodes — we were looking at two separate code paths for the same data, forever. That felt wrong before we even wrote a line of it.

Closing (verbatim): "We had the right instinct — in-memory is correct. We just had no clean way to get there. And when I looked at what it would take to build it properly, I kept thinking: there has to be a better model."
-->

---
layout: center
class: text-center
---

<img src="/images/headshot.jpg" style="width: 180px; height: 210px; object-fit: cover; object-position: top center; clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%); filter: drop-shadow(0 0 14px rgba(189, 147, 249, 0.5)); display: block; margin: 0 auto 1.5rem;" />

## Viswanathan (Vish) Ranganathan

<div style="opacity: 0.7; margin-top: 0.25rem; margin-bottom: 1.75rem;">Senior Engineer · Netflix Delivery</div>

<div style="opacity: 0.6; font-size: 0.85rem; margin-bottom: 0.6rem; letter-spacing: 0.05em; text-transform: uppercase;">Come talk to me about</div>

<div style="display: flex; gap: 0.5rem; justify-content: center; flex-wrap: wrap; max-width: 560px; margin: 0 auto;">
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Java</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Scala</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Functional Programming</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Distributed Systems</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Streaming Systems</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Data Analytics</span>
  <span style="background: rgba(255,255,255,0.1); padding: 0.2rem 0.75rem; border-radius: 9999px; font-size: 0.85rem;">Open Source</span>
</div>

<!--
Opening (verbatim): "Before I go further — I'm Vish. I work at Netflix. And I've been obsessed with this problem for longer than I'd like to admit."
-->

---
layout: center
---

# Why The Obvious Approach Doesn't Hold Up

<!--
Opening (verbatim): "Let me walk through why the preload-and-refresh model falls apart — even before you build it."
-->

---
layout: center
---

## Problem A

### Two code paths, one problem

---
layout: center
class: text-center
---

# Preload vs. Refresh

**At bootstrap:** paginate through accounts list

**At runtime:** fetch individual entries from the API

Two systems to test, operate, and debug. **Forever.**

<img src="/images/account-service-load.png" style="max-height: 260px; margin: 1.5rem auto 0; display: block;" />

<!--
Problem A: Preload at bootstrap via pagination. Refresh individual entries at runtime via the API. Two different mechanisms for the same data — test both, operate both, debug both. The complexity isn't in the code; it's in the model. You've split a single concern into two systems that both need to be correct simultaneously.
-->

---
layout: center
---

## Problem B

### The runtime fetch still creates a thundering herd

Individual entries expire. One node fetches. Fine.

But entries loaded at the same time expire at the same time — **across every node, simultaneously.**

<!--
Problem B - Beat 1+2: Individual entries expire. One node fetches from the accounts service — fine. But entries that were loaded at the same time expire at the same time, across every node. Suddenly many nodes are fetching the same accounts simultaneously.
-->

---
layout: center
---

# Cache Stampede

<img src="/images/cache-stampede.png" style="max-height: 420px; width: auto;" />

<!--
Problem B - Beat 3: This is the cache stampede — coordinated expiry, coordinated reload, coordinated load spike.
-->

---
layout: center
class: text-center
---

# Patches on a Broken Model

<div style="font-size: 2rem; font-weight: 700; margin-top: 2rem; color: #ff5555;">The model is still broken.</div>

<!--
Problem B - Beat 4: You can jitter TTLs. You can use probabilistic early expiry. These are patches on a broken model.
-->

---
layout: center
class: text-center
---

# One Producer. N Consumers.

<div style="font-size: 2.5rem; font-weight: 700; margin-top: 1.5rem;">Push, not poll.</div>

<div style="margin-top: 1rem; opacity: 0.8; font-size: 0.95rem;">Consumers never touch the source again. No pagination. No expiry timers. No stampede.</div>

<!--
What if you separated data preparation from data serving entirely? One process — the producer — owns the dataset. It decides when data has changed, prepares the new state, and publishes it. N consumers hold a local copy. They don't decide when to reload. They get told. Push, not poll. Consumers never touch the source system again after the first load — they receive only what changed.
-->

---
layout: center
---

# This Is Just Git

Clone once. Pull only the diff.

**Producer** = the remote

**Consumer** = your local repo

<!--
This is not a new idea — it's how git works. Clone once. Pull only the diff. The producer is the remote. The consumer is your local repo.
-->

---
layout: center
class: text-center
---

# Hollow: A Data Distribution System

<div style="margin-top: 1rem; opacity: 0.65; font-size: 0.9rem;">github.com/Netflix/hollow</div>

<!--
Opening (verbatim): "Hollow is not a caching library. It's a data distribution system. Let me show you what that means in code."
-->

---
layout: center
---

# Four Interfaces. That's the Contract.

<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem 3rem; margin-top: 1.5rem; font-size: 0.9rem;">
<div><strong>Publisher</strong> — writes snapshots + deltas to blob storage</div>
<div><strong>Announcer</strong> — writes the new version to a coordination store</div>
<div><strong>BlobRetriever</strong> — reads blobs from storage on the consumer side</div>
<div><strong>AnnouncementWatcher</strong> — polls the coordination store for new versions</div>
</div>

<img src="/images/four-interfaces.png" style="max-height: 260px; width: auto; margin: 1.5rem auto 0; display: block;" />

<!--
Four interfaces — that's the entire contract Hollow defines. Implement these four and Hollow handles the rest: delta computation, double-buffered state engine, type-safe index. The diagram shows Producer on the left, Consumer on the right, blob storage and a coordination store in the middle.
-->

---
layout: center
---

# The Architecture

<img src="/images/hollow-architecture.png" style="max-height: 420px; width: auto;" />

<!--
That's the full picture. Producer publishes to S3 and announces to DynamoDB. Consumer polls DynamoDB and fetches from S3. The delta keeps every node in sync without touching the source system again.
-->

---
layout: center
---

# The Example

Two independent datasets. One Spring Boot service.

| Dataset | Records | Update cadence |
|---|---|---|
| **Catalog** | 50,000 movies | every 7 min |
| **Users** | 350,000 profiles | every 10 min |

One query endpoint: `GET /users/{userId}/recently-watched`

<!--
The demo runs two Hollow datasets against real AWS — each with its own producer and consumer, S3 prefix, and DynamoDB entry. The interesting endpoint joins both datasets in memory at query time: look up the user, look up each watched movie ID in the catalog, return enriched results. No database. No API call.
-->

---
layout: center
---

# Define Your Schema

```kotlin
// 50,000 records · updates every 7 min
@HollowPrimaryKey(fields = ["id"])
data class Movie(
    val id: Long, val title: String, val genre: String,
    val rating: Double, val releaseYear: Int, val duration: Int,
    val director: String, val cast: List<String>,
    val tags: List<String>, // ...18 fields total
)

// 350,000 profiles · updates every 10 min
@HollowPrimaryKey(fields = ["userId"])
data class User(
    val userId: String, val firstName: String, val lastName: String,
    val planName: String,
    val recentlyWatchedMovieIds: List<Long>
)
```

<!--
Schema: Two annotations drive everything — @HollowPrimaryKey on each data class. That's the only Hollow-specific code in your model. The annotation tells Hollow how to build the O(1) primary key index. Two datasets, two cadences, fully independent.
-->

---
layout: center
---

# Generate the Client

<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; align-items: start; font-size: 0.78rem;">

<div>

```groovy
task generateHollowAPI(type: JavaExec) {
    group = 'hollow'
    mainClass = 'org.vish.hollowdemo.codegen.GenerateMovieAPI'
    classpath = sourceSets.main.runtimeClasspath
    dependsOn classes
}
task generateUserAPI(type: JavaExec) {
    group = 'hollow'
    mainClass = 'org.vish.hollowdemo.codegen.GenerateUserAPI'
    classpath = sourceSets.main.runtimeClasspath
    dependsOn classes
}
task generateAllAPIs {
    group = 'hollow'
    dependsOn generateHollowAPI, generateUserAPI
}
```

```bash
./gradlew generateAllAPIs
```
```
→ MovieAPI.java
→ UserAPI.java
```

</div>

<div>

```java
public class GenerateMovieAPI {
  public static void main(String[] args)
      throws IOException {
    HollowWriteStateEngine writeEngine =
        new HollowWriteStateEngine();
    HollowObjectMapper mapper =
        new HollowObjectMapper(writeEngine);

    mapper.initializeTypeState(Movie.class);

    HollowAPIGenerator generator =
        new HollowAPIGenerator.Builder()
            .withAPIClassname("MovieAPI")
            .withPackageName("org.vish.hollowdemo.api")
            .withDataModel(writeEngine)
            .build();

    generator.generateFiles("src/main/java");
  }
}
```

</div>
</div>

<!--
Codegen: The Gradle task points at a plain Java main class. That class initializes a write state engine, registers your data model type, then runs HollowAPIGenerator. One task per dataset, one convenience wrapper. Hollow inspects your annotated data classes and generates a type-safe Java API — complete with primary key index. IDE completion, compile-time safety, no reflection at query time.
-->

---
layout: center
---

# Wire the Producer

```kotlin
val localDir = Paths.get("data/catalog")

val producer = HollowProducer
    .withPublisher(HollowFilesystemPublisher(localDir))
    .withAnnouncer(HollowFilesystemAnnouncer(localDir))
    .build()
```

<!--
Producer wiring: filesystem publisher and announcer — both write to the same local directory. Snapshots, deltas, and the version announcement all land on disk. Same contract as the AWS adapters; swap the implementations and you're in production.
-->

---
layout: center
---

# Produce a Cycle

```kotlin
producer.runCycle { writeState ->
    currentMovies.forEach { movie -> writeState.add(movie) }
    // Hollow computes: snapshot · delta · reverse-delta
}
```

<!--
runCycle: You hand Hollow the full current state. It diffs against the previous state and produces three artifacts: a new snapshot, a forward delta, and a reverse delta for rollback. Your code is just: for each record, writeState.add(it).
-->

---
layout: center
---

# What Gets Published

```
HollowS3Publisher  Publishing SNAPSHOT  → catalog/snapshot-20260519202727001
HollowS3Publisher  Publishing DELTA     → catalog/delta-...-20260519203437003
HollowS3Publisher  Publishing REV-DELTA → catalog/reversedelta-...
DynamoDBAnnouncer  Announcing version 20260519203437003 → hollow-dev-announcements
```

<div style="margin-top: 1.5rem; opacity: 0.7; font-size: 0.9rem;">Cycle #2 · delta: 620 KB · snapshot: 10.4 MB · total cycle time: 3.4 s</div>

<!--
Three artifacts per cycle: snapshot (full state, for new consumers), delta (what changed, for existing consumers), reverse-delta (for rollback). Then a single version write to DynamoDB — that's what consumers poll.
-->

---
layout: center
---

# Wire the Consumer

```kotlin
val localDir = Paths.get("data/catalog")

val consumer = HollowConsumer
    .withBlobRetriever(HollowFilesystemBlobRetriever(localDir))
    .withAnnouncementWatcher(HollowFilesystemAnnouncementWatcher(localDir))
    .withGeneratedAPIClass(MovieAPI::class.java)
    .build()

consumer.triggerRefresh()  // reads snapshot from disk, blocks until ready
val movieIndex = Movie.uniqueIndex(consumer)
```

<!--
Consumer wiring: symmetric to the producer — same local directory, same filesystem implementations. triggerRefresh() reads the snapshot from disk and blocks until the full dataset is loaded and indexed. After that call returns, every read is a local memory lookup. The watcher then polls the announcement file and keeps it current automatically in the background.
-->

---
layout: center
---

# Double-Buffer Model

<img src="/images/double-buffer.png" style="max-height: 420px; width: auto;" />

<!--
How Hollow Works: new state is built in a shadow buffer. When ready, an atomic pointer swap makes it live. Zero downtime. Zero lock contention for readers.
-->

---
layout: center
class: text-center
---

# What You Get For Free

**Type-safe generated APIs**

**Memory-optimized encoding**

**Consumer pinning**

<!--
What else you get for free: Type-safe generated APIs: Hollow generates a custom, type-safe API from your data model. IDE completion, compile-time safety. Memory-optimized encoding: compact binary encoding for minimal heap footprint. Memory pooled and reused during updates — delta application doesn't spike GC. Consumer pinning: pin a consumer to a specific version. If a bad dataset is published, consumers hold at the last known good version. History server: built-in, browser-accessible history server for visual data inspection and diffing across versions. Zero third-party dependencies: the core Hollow jar has no external dependencies.

Closing (verbatim): "Hollow ships with one implementation of those four interfaces out of the box: the filesystem. Great for local development. Not useful in production. That's the gap."
-->

---
layout: center
---

# The Gap

<!--
Opening (verbatim): "This is the moment where most teams stop."
-->

---
layout: center
class: text-center
---

# You Want AWS. Hollow Ships Filesystem.

S3 for blobs. DynamoDB for announcements.

<!--
Gap - Beat 1: You read the Hollow docs. You understand the model. You want to run it on AWS — S3 for blobs, DynamoDB for announcements.
-->

---
layout: center
class: text-center
---

# Four Interfaces to Implement

Not hard. **But not nothing.**

<!--
Gap - Beat 2: You need to implement four interfaces. It's not hard, but it's not nothing. AWS SDK wiring, error handling, retry logic, multi-dataset organization.
-->

---
layout: center
class: text-center
---

# The Fork in the Road

<img src="/images/fork-in-the-road.png" style="max-height: 320px; margin: 1rem auto 1.25rem; display: block;" />

Write it yourself. Or go back to Redis.

<!--
Gap - Beat 3+4: Every team that evaluates Hollow either writes this themselves — or goes back to Redis because it's easier to get started. That adoption tax is the reason a genuinely good tool has lower adoption than it deserves.

Closing (verbatim): "I got tired of seeing that happen. So I built the bridge."
-->

---
layout: center
class: text-center
---

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 98 96" style="width: 72px; height: 72px; fill: currentColor; margin: 0 auto 1.5rem; display: block;">
  <path fill-rule="evenodd" clip-rule="evenodd" d="M48.854 0C21.839 0 0 22 0 49.217c0 21.756 13.993 40.172 33.405 46.69 2.427.49 3.316-1.059 3.316-2.362 0-1.141-.08-5.052-.08-9.127-13.59 2.934-16.42-5.867-16.42-5.867-2.184-5.704-5.42-7.17-5.42-7.17-4.448-3.015.324-3.015.324-3.015 4.934.326 7.523 5.052 7.523 5.052 4.367 7.496 11.404 5.378 14.235 4.074.404-3.178 1.699-5.378 3.074-6.6-10.839-1.141-22.243-5.378-22.243-24.283 0-5.378 1.94-9.778 5.014-13.2-.485-1.222-2.184-6.275.486-13.038 0 0 4.125-1.304 13.426 5.052a46.97 46.97 0 0 1 12.214-1.63c4.125 0 8.33.571 12.213 1.63 9.302-6.356 13.427-5.052 13.427-5.052 2.67 6.763.97 11.816.485 13.038 3.155 3.422 5.015 7.822 5.015 13.2 0 18.905-11.404 23.06-22.324 24.283 1.78 1.548 3.316 4.481 3.316 9.126 0 6.6-.08 11.897-.08 13.526 0 1.304.89 2.853 3.316 2.364 19.412-6.52 33.405-24.935 33.405-46.691C97.707 22 75.788 0 48.854 0z"/>
</svg>

# hollow-infra-adapters

<!--
Opening (verbatim): "It's called hollow-infra-adapters. It's open source. Here's the entire producer setup."
-->

---
layout: center
---

# Producer + Consumer Setup

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

**Producer**

```java
HollowProducer producer = HollowProducer
  .withPublisher(
    HollowS3Publisher.create(config))
  .withAnnouncer(
    HollowDynamoDBAnnouncer.create(config))
  .build();

producer.runCycle(state ->
  state.add(myData));
```

</div>

<div>

**Consumer**

```java
HollowConsumer consumer = HollowConsumer
  .withBlobRetriever(
    HollowS3BlobRetriever.create(config))
  .withAnnouncementWatcher(
    HollowDynamoDBAnnouncementWatcher
      .create(config))
  .build();
```

</div>

</div>

<!--
The code slide. Four lines of config. That's the entire integration surface.
-->

---
layout: center
---

# Config: 4 Environment Variables

```java
HollowAwsConfig config = HollowAwsConfig.fromEnvironment();
```

<!--
Adapters - Beat 1: Config comes from environment variables. HollowAwsConfig.fromEnvironment() — bucket name, table name, region, dataset ID.
-->

---
layout: center
class: text-center
---

# Multiple Datasets: No Terraform Changes

Same bucket. Same table. Different `datasetId`.

<!--
Adapters - Beat 3: Multiple datasets: same bucket, same table, different datasetId. S3 key prefix handles separation automatically. No Terraform changes for a second dataset.
-->

---
layout: center
class: text-center
---

# What The Library Handles

**You handle your data model.**

<!--
Adapters - Beat 4: The library handles: blob upload, version announcement, polling, delta application, retry logic. You handle your data model.

Closing (verbatim): "That's the entire integration surface. Let me show you what this looks like at runtime."
-->

---
layout: center
---

# The Log Walkthrough

<!--
Opening (verbatim): "This is a real run against real AWS. Two datasets — 50,000 movies, 350,000 user profiles."
-->

---
layout: center
---

# Catalog Snapshot

```
13:27:27  CatalogProducer     Generating initial catalog (seed=99)...
13:27:27  CatalogProducer     Catalog prepared: 50,000 movies
13:27:28  HollowS3Publisher   Publishing SNAPSHOT → catalog/snapshot-20260519202727001
13:27:30  DynamoDBAnnouncer   Announcing version 20260519202727001
13:27:30  CatalogProducer     Cycle #1 completed in 3,370ms
```

<div style="font-size: 4rem; font-weight: 900; margin-top: 1.5rem;">3.4s</div>
<div style="opacity: 0.6; margin-top: 0.25rem;">50,000 movies · snapshot: 10.4 MB</div>

<!--
Beat 1 narration: 50K records. Deterministic seed — same input, same dataset, every time. Full snapshot written to S3 in 3 seconds. 10.4 MB. Version announced to DynamoDB.
-->

---
layout: center
---

# Users Snapshot

```
13:27:32  UserProducer        Generating 350,000 users (seed=42)...
13:27:33  UserProducer        User generation complete: 350,000 users in 1,948ms
13:27:34  HollowS3Publisher   Publishing SNAPSHOT → users/snapshot-20260519202732002
13:27:36  DynamoDBAnnouncer   Announcing version 20260519202732002
13:27:37  UserProducer        Cycle #1 completed in 4,605ms
```

<div style="font-size: 4rem; font-weight: 900; margin-top: 1.5rem;">4.6s</div>
<div style="opacity: 0.6; margin-top: 0.25rem;">350,000 profiles · snapshot: 24.7 MB</div>

<!--
Beat 2 narration: 350K user profiles. Bigger dataset, takes 4.5 seconds. But this only runs once. Every update after this is a delta.
-->

---
layout: center
---

# Consumer Cold Start

```
13:27:42  CatalogConsumer     🔄 Catalog update started: v-MAX → v20260519202727001
13:27:43  CatalogConsumer     ✅ Catalog updated (update #1) · O(1) index ready
13:27:43  UserConsumer        🔄 User dataset update started: v-MAX → v20260519202732002
13:27:45  UserConsumer        ✅ User dataset updated (update #1) · O(1) index ready
13:27:46  Spring Boot         Started HollowdemoApplicationKt in 5.226 seconds
```

<div style="font-size: 4rem; font-weight: 900; margin-top: 1.5rem;">5.2s</div>
<div style="opacity: 0.6; margin-top: 0.25rem;">cold boot · 50K movies + 350K users · both indexed</div>

<!--
Beat 3 narration: Consumer starts cold. Pulls both snapshots from S3. Catalog: 1.3s. Users: 2.4s. Full Spring startup with both datasets: 5.7 seconds. After that, no full reload — ever.
-->

---
layout: center
---

# Delta Cycle

```
13:34:44  CatalogConsumer     🔄 Catalog update started: v20260519202727001 → v20260519203437003
13:34:45  HollowClientUpdater update plan: {DELTA to 20260519203437003}
13:34:45  HollowBlobReader    DELTA COMPLETED IN 28ms
13:34:45  CatalogConsumer     ✅ Catalog updated: v...202727001 → v...203437003 (update #2)
```

<div style="font-size: 4rem; font-weight: 900; margin-top: 1.5rem;">28ms</div>
<div style="opacity: 0.6; margin-top: 0.25rem;">delta · 620 KB · background thread · read path never blocked</div>

<!--
Beat 4 narration: 7 minutes later, producer runs. Consumer sees the new version in DynamoDB, fetches 620 KB instead of 10.4 MB. Applied in 28ms. No restart. No round trip to the source system. No thundering herd.

Closing (verbatim): "Every node in your fleet gets the same 620 KB. Applied in under 50ms. The coordination problem is gone — not patched, structurally eliminated."
-->

---
layout: center
class: text-center
---

# The Cross-Dataset Join

```http
GET /users/{userId}/recently-watched
```

<div style="margin-top: 1.5rem; text-align: left; max-width: 500px; margin: 1.5rem auto 0; font-size: 0.9rem; line-height: 2.2;">
1. Look up user in <code>users</code> dataset &nbsp;→&nbsp; <strong>O(1), local memory</strong><br/>
2. For each watched ID, look up <code>catalog</code> &nbsp;→&nbsp; <strong>O(1), local memory</strong><br/>
3. Return enriched results &nbsp;→&nbsp; <strong>no DB · no API · no network</strong>
</div>

<!--
Cross-dataset join: Two independent datasets, two different update cadences, joined in memory at query time. No database join. No API fan-out. No cache warming. The user profile is always current. The movie catalog is always current. They just happen to be consistent within their own delta window. This is the pattern Netflix uses at scale.
-->

---
layout: center
---

# When To Use It

<!--
Opening (verbatim): "Hollow is the right tool when three things are true."
-->

---
layout: center
class: text-center
---

# Use Hollow When

**Read-heavy.** Millions of reads, rare writes.

**Bounded.** Fits in memory.

**Eventually consistent is fine.**

<!--
CTA - Use when: Your dataset is read-heavy. Read millions of times, written rarely. Account metadata, feature flags, product catalogs, entitlements. The dataset is bounded. Fits in memory — tens of MB to a few GB. Not a replacement for your operational database. Staleness of a few seconds is acceptable. Hollow is eventually consistent on the delta polling interval (5s default).
-->

---
layout: center
class: text-center
---

# Skip Hollow When

Sub-second consistency required.

Dataset is unbounded or user-generated.

Random write/read patterns — **use a database.**

<!--
CTA - Skip when: You need sub-second consistency. The dataset is unbounded or scales with user-generated content. You have random write/read patterns — use a database.
-->

---
layout: center
class: text-center
---

# Get Started

`github.com/vichu/hollow-infra-adapters`

Come find me after.

<!--
CTA - Links: github.com/vichy/hollow-infra-adapters — the library, Terraform included. Netflix Hollow repo — the engine, well-documented. Come find me after. I want to hear what dataset you're thinking about.
-->

---
layout: center
class: text-center
---

# Thank You

**Every deployment should be boring.**

---
layout: center
class: text-center
---

**Vish Ranganathan**

`github.com/vichu/hollow-infra-adapters`

<!--
Closing (verbatim): "Every deployment should be boring. Your upstream services shouldn't know you exist. Hollow gets you there. Thank you."
-->
