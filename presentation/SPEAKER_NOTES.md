# Cache Me If You Can: Decentralize Your Distributed Caches With Hollow
## Speaker Notes — Full Narrative

**Conference:** Linux Foundation Open Source Summit North America 2026
**Speaker:** Viswanathan Ranganathan (Vish), Netflix
**Duration:** 40 minutes
**Format:** Log walkthrough (pre-captured logs shown on screen, narrated)

---

## Key Takeaways (speaker only — not on slides)

- **KT1:** Distributed caching is not the solution for every problem — and local caching's failure is a coordination failure. Every node independently deciding when to reload is the root cause, not the cache size or the TTL.
- **KT2:** Hollow separates data preparation from data serving. One producer publishes. N consumers hold a local copy and receive only deltas.
- **KT3:** You can wire Hollow to real AWS infrastructure today with 4 lines of config.

---

## Section 1 — The Story (5 min | ~10 slides)

**Opening (verbatim):** "Let me tell you about a problem that sounds simple until you actually have to solve it."

We were building a service that needed to handle about 1 million requests per second. Each request needed one thing: account metadata. Who is this user, what are they entitled to?

The accounts service had an API for this. Easy. But 1M req/sec hitting that API directly — even cached on the server side — felt like a lot of load for what was conceptually a trivial lookup. We didn't want to make the accounts service our bottleneck.

We thought about a distributed cache. Redis. But that felt like over-engineering for this use case, and we didn't want the operational burden — another cluster to scale, monitor, and get paged for at 2am.

The obvious solution: paginate through the accounts list endpoint at startup, preload everything into memory, then fetch individual entries from the API as they expire. In-memory for the hot path, API for misses. Simple enough in theory.

But when we mapped it out — bulk preload at bootstrap, individual fetches at runtime, coordinated expiry across nodes — we were looking at two separate code paths for the same data, forever. That felt wrong before we even wrote a line of it.

**Closing (verbatim):** "We had the right instinct — in-memory is correct. We just had no clean way to get there. And when I looked at what it would take to build it properly, I kept thinking: there has to be a better model."

---

## Self-Introduction (1 min | ~2 slides)

**Opening (verbatim):** "Before I go further — I'm Vish. I work at Netflix. And I've been obsessed with this problem for longer than I'd like to admit."

The story I just told you is real. That was me, at a previous company, building systems at scale and hitting exactly that wall.

I eventually found Hollow, fell in love with the model, and couldn't understand why adoption wasn't higher. So I built hollow-infra-adapters to close the gap — and that's what this talk is really about.

I'm not here to sell you a product. The library is open source, Apache 2.0 licensed.

**Closing (verbatim):** "Okay. Let me show you exactly why the approach most teams take breaks down."

---

## Section 2 — Why the Obvious Approach Doesn't Hold Up (8 min | ~16 slides)

**Opening (verbatim):** "Let me walk through why the preload-and-refresh model falls apart — even before you build it."

### Problem A — Two code paths, one problem

Preload at bootstrap via pagination. Refresh individual entries at runtime via the API. These are two completely different mechanisms for fetching the same data.

You now own both. Test both. Operate both. When something breaks, you debug both.

The complexity isn't in the code — it's in the model. You've split a single concern into two separate systems that both need to be correct simultaneously.

### Problem B — The runtime fetch still creates a thundering herd

Individual entries expire. One node fetches from the accounts service. Fine.

But entries that were loaded at the same time expire at the same time — across every node. Suddenly many nodes are fetching the same accounts simultaneously.

This is the **cache stampede** — coordinated expiry, coordinated reload, coordinated load spike.

You can jitter TTLs. You can use probabilistic early expiry. These are patches on a broken model.

### Problem C — Bootstrap is expensive and thundering herd shows up there too

Every new node paginates through the full accounts list to warm up. Multiple round trips before the node is ready to serve.

In a rolling deploy, every node does this simultaneously. Coordinated burst of read load on the accounts service that scales with your fleet size.

This is the **thundering herd** — not one node, but all of them, every deploy, in lockstep.

**Closing (verbatim):** "The root problem is that every node is independently deciding when to reload, how much to reload, and from where. That's three decisions made N times across your fleet with no coordination. Split that responsibility and the whole thing gets simpler."

---

## Section 3 — The Insight (3 min | ~6 slides)

**Opening (verbatim):** "What if you separated data preparation from data serving entirely?"

One process — the producer — owns the dataset. It decides when data has changed. It prepares the new state. It publishes it.

N processes — the consumers — hold a local copy. They don't decide when to reload. They get told. Push, not poll.

Consumers never touch the source system again after the first load. They receive only what changed.

This is not a new idea — it's how git works. Clone once. Pull only the diff. The producer is the remote. The consumer is your local repo.

**Closing (verbatim):** "That's the model Netflix built Hollow on. And once you see it, you can't unsee it."

---

## Section 4 — How Hollow Works (8 min | ~16 slides)

**Opening (verbatim):** "Hollow is not a caching library. It's a data distribution system."

**The producer** owns the dataset. Runs on a schedule or event-driven. Serializes the full dataset each cycle.

**Snapshot** = the full dataset serialized. Published once on first cycle. Like a git clone — the full state, one time.

**Delta** = only what changed since the last version. Like a git pull — only the diff.

Real number from the demo: snapshot for 50K records is ~10MB. Delta is ~650KB. **93% smaller.** Every cycle after the first is a delta.

**The consumer** starts with a snapshot. After that, it applies deltas. The dataset stays current without ever touching the source system again.

**Double-buffer model:** new state is built in a shadow buffer. When ready, an atomic pointer swap makes it live. Zero downtime. Zero lock contention for readers.

**Announcer / AnnouncementWatcher:** producer writes a version number to a coordination channel when a cycle completes. Consumers poll that channel (every 5s by default). When a new version appears, they fetch and apply the delta.

Four interfaces. That's the entire contract: Publisher, Announcer, BlobRetriever, AnnouncementWatcher.

**What else you get for free:**

- **Type-safe generated APIs:** Hollow generates a custom, type-safe API from your data model. IDE completion, compile-time safety.
- **Memory-optimized encoding:** Compact binary encoding for minimal heap footprint. Memory pooled and reused during updates — delta application doesn't spike GC.
- **Consumer pinning:** Pin a consumer to a specific version. If a bad dataset is published, consumers hold at the last known good version.
- **History server:** Built-in, browser-accessible history server for visual data inspection and diffing across versions.
- **Zero third-party dependencies:** The core Hollow jar has no external dependencies.

**Closing (verbatim):** "Hollow ships with one implementation of those four interfaces out of the box: the filesystem. Great for local development. Not useful in production. That's the gap."

---

## Section 5 — The Gap (2 min | ~4 slides)

**Opening (verbatim):** "This is the moment where most teams stop."

You read the Hollow docs. You understand the model. You want to run it on AWS — S3 for blobs, DynamoDB for announcements.

You need to implement four interfaces. It's not hard, but it's not nothing. AWS SDK wiring, error handling, retry logic, multi-dataset organization.

Every team that evaluates Hollow either writes this themselves — or goes back to Redis because it's easier to get started.

That adoption tax is the reason a genuinely good tool has lower adoption than it deserves.

**Closing (verbatim):** "I got tired of seeing that happen. So I built the bridge."

---

## Section 6 — The Adapters + Code (5 min | ~10 slides)

**Opening (verbatim):** "It's called hollow-infra-adapters. It's open source. Here's the entire producer setup."

Show the code slide — producer on the left, consumer on the right. Walk through both.

Config comes from environment variables. `HollowAwsConfig.fromEnvironment()` — bucket name, table name, region, dataset ID.

Infrastructure: 1 S3 bucket, 1 DynamoDB table. Terraform module is in the repo. ~$0.05/month.

Multiple datasets: same bucket, same table, different `datasetId`. S3 key prefix handles separation automatically. No Terraform changes for a second dataset.

The library handles: blob upload, version announcement, polling, delta application, retry logic. You handle your data model.

**Closing (verbatim):** "That's the entire integration surface. Let me show you what this looks like at runtime."

---

## Section 7 — The Log Walkthrough (5 min | ~10 slides)

**Opening (verbatim):** "This is a real run against real AWS. 50,000 movies."

### Beat 1 — Producer publishes

```
Dataset prepared: 50000 movies
Producer cycle #1 completed in 1426ms
SNAPSHOT COMPLETED IN 26ms
```

50K records. Full snapshot written to S3 in 26ms. Version announced to DynamoDB.

### Beat 2 — Consumer cold start

```
SNAPSHOT COMPLETED IN 2ms
✅ Update successful: v-9223372036854775808 -> v20251113053031001
```

Consumer polls DynamoDB, sees a version, fetches snapshot from S3. 2ms to apply 50K records into memory. That's the double-buffer doing its job.

### Beat 3 — Delta cycles

```
🔄 Update started: v20251113053031001 -> v20251113053231002
DELTA COMPLETED IN 3ms
✅ Update successful

🔄 Update started: ...
DELTA COMPLETED IN 1ms

🔄 Update started: ...
DELTA COMPLETED IN 0ms
```

Producer runs again. Consumer picks up the delta. 0ms. No round trips to the source system. No thundering herd. No stale window.

**Closing (verbatim):** "Every node gets the same delta at the same time, applied in under a millisecond. The coordination problem is gone — not patched, structurally eliminated."

---

## Section 8 — When To Use It + CTA (4 min | ~8 slides)

**Opening (verbatim):** "Hollow is the right tool when three things are true."

**Use it when:**

1. Your dataset is **read-heavy**. Read millions of times, written rarely. Account metadata, feature flags, product catalogs, entitlements.
2. The dataset is **bounded**. Fits in memory — tens of MB to a few GB. Not a replacement for your operational database.
3. **Staleness of a few seconds is acceptable**. Hollow is eventually consistent on the delta polling interval (5s default).

**Skip it when:**

- You need sub-second consistency.
- The dataset is unbounded or scales with user-generated content.
- You have random write/read patterns — use a database.

**CTA:**

1. `github.com/vichu/hollow-infra-adapters` — the library, Terraform included.
2. Netflix Hollow repo — the engine, well-documented.
3. Come find me after. I want to hear what dataset you're thinking about.

**Closing (verbatim):** "Every deployment should be boring. Your upstream services shouldn't know you exist. Hollow gets you there. Thank you."
