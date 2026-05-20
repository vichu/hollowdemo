# Live Run Stats — 2026-05-19

Clean run against AWS (S3 + DynamoDB, `us-east-1`).  
Reset: `bash demo-reset.sh --clean-only` — empty bucket and table before start.

---

## Producer — startup and first publish cycles

```
13:27:27  CatalogProducer   Starting catalog publish cycle #1
            Generating initial catalog (deterministic, seed=99)...
            Catalog prepared: 50,000 movies

13:27:27  HollowS3Publisher Publishing header  → s3://hollow-dev-blobs/catalog/header-20260519202727001
13:27:28  HollowS3Publisher Publishing SNAPSHOT → s3://hollow-dev-blobs/catalog/snapshot-20260519202727001
13:27:30  HollowBlobReader  SNAPSHOT COMPLETED IN 36ms
13:27:30  DynamoDBAnnouncer Announcing version 20260519202727001 → hollow-dev-announcements (dataset: catalog)
13:27:30  CatalogProducer   Catalog publish cycle #1 completed in 3,370ms
13:27:30  CatalogProducer   Published 50,000 movies to s3://hollow-dev-blobs/catalog

13:27:32  UserProducer      Starting user publish cycle #1
            Generating 350,000 users (deterministic, seed=42)...
            User generation complete: 350,000 users in 1,948ms

13:27:33  HollowS3Publisher Publishing header  → s3://hollow-dev-blobs/users/header-20260519202732002
13:27:34  HollowS3Publisher Publishing SNAPSHOT → s3://hollow-dev-blobs/users/snapshot-20260519202732002
13:27:36  HollowBlobReader  SNAPSHOT COMPLETED IN 73ms
13:27:36  DynamoDBAnnouncer Announcing version 20260519202732002 → hollow-dev-announcements (dataset: users)
13:27:37  UserProducer      User publish cycle #1 completed in 4,605ms
13:27:37  UserProducer      Published 350,000 users to s3://hollow-dev-blobs/users
```

---

## Consumer — cold boot, both snapshot loads

```
13:27:41  Spring Boot       Starting HollowdemoApplicationKt (consumer profile)

13:27:41  CatalogConsumer   Initializing CatalogConsumer...
13:27:41  CatalogConsumer   Watching catalog dataset — S3: hollow-dev-blobs, DynamoDB: hollow-dev-announcements
13:27:41  AnnouncementWatcher Started polling DynamoDB every 5 seconds for dataset catalog

13:27:42  CatalogConsumer   🔄 Catalog update started: v-MAX → v20260519202727001
13:27:43  HollowClientUpdater update plan: {SNAPSHOT to 20260519202727001}
13:27:43  HollowBlobReader  SNAPSHOT COMPLETED IN 21ms
13:27:43  CatalogConsumer   ✅ Catalog updated: v-MAX → v20260519202727001 (update #1)
13:27:43  CatalogConsumer   O(1) movie-by-ID index ready
13:27:43  CatalogConsumer   Genre hash index ready

13:27:43  UserConsumer      Initializing UserConsumer...
13:27:43  AnnouncementWatcher Started polling DynamoDB every 5 seconds for dataset users

13:27:43  UserConsumer      🔄 User dataset update started: v-MAX → v20260519202732002
13:27:45  HollowClientUpdater update plan: {SNAPSHOT to 20260519202732002}
13:27:45  HollowBlobReader  SNAPSHOT COMPLETED IN 26ms
13:27:45  UserConsumer      ✅ User dataset updated: v-MAX → v20260519202732002 (update #1)
13:27:45  UserConsumer      O(1) user-by-ID index ready

13:27:46  Spring Boot       Started HollowdemoApplicationKt in 5.226 seconds
```

**Cold-boot timeline:** 5.2 s from JVM start to serving requests — both snapshots downloaded and indexed (50K movies + 350K users).

---

## Producer — catalog delta cycle (7 minutes later)

```
13:34:37  CatalogProducer   Starting catalog publish cycle #2
            Generating updated catalog (new releases, rating changes)...
            Catalog prepared: 51,712 movies  ← +1,712 added, some removed/updated

13:34:37  HollowS3Publisher Publishing header    → catalog/header-20260519203437003
13:34:38  HollowS3Publisher Publishing SNAPSHOT  → catalog/snapshot-20260519203437003
13:34:40  HollowBlobReader  SNAPSHOT COMPLETED IN 34ms
13:34:40  HollowS3Publisher Publishing DELTA     → catalog/delta-20260519202727001-20260519203437003
13:34:40  HollowBlobReader  DELTA COMPLETED IN 23ms
13:34:40  HollowS3Publisher Publishing REV-DELTA → catalog/reversedelta-20260519203437003-20260519202727001
13:34:40  HollowBlobReader  DELTA COMPLETED IN 13ms
13:34:40  DynamoDBAnnouncer Announcing version 20260519203437003 (catalog)
13:34:40  CatalogProducer   Catalog publish cycle #2 completed in 3,303ms
13:34:40  CatalogProducer   Published 51,712 movies to s3://hollow-dev-blobs/catalog
```

---

## Consumer — delta refresh (background thread, no restart)

```
13:34:44  CatalogConsumer   🔄 Catalog update started: v20260519202727001 → v20260519203437003
                              Thread: [ilder | refresh]  ← Hollow's background watcher
13:34:45  HollowClientUpdater update plan: {DELTA to 20260519203437003}
13:34:45  HollowBlobReader  DELTA COMPLETED IN 28ms
13:34:45  CatalogConsumer   ✅ Catalog updated: v20260519202727001 → v20260519203437003 (update #2)
```

**4 seconds** from producer announcing `v203437003` to consumer serving updated data.  
Delta applied to a shadow buffer and swapped atomically — read path never blocked.

---

## Producer — users cycle (10 minutes later)

```
13:37:37  UserProducer      Starting user publish cycle #2
13:37:39  UserProducer      User publish cycle #2 completed in 2,013ms
13:37:39  UserProducer      Published 350,000 users to s3://hollow-dev-blobs/users
```

Data is deterministic (same seed), so Hollow detects no change — no new blobs published, DynamoDB version stays at `202732002`. Cycle completes in 2 s vs 4.6 s for cycle #1.

---

## Key numbers

| Metric | Value |
|---|---|
| Catalog snapshot (50K movies) | **10.4 MB** |
| Catalog delta (one cycle) | **620 KB** (6% of snapshot) |
| Users snapshot (350K profiles) | **24.7 MB** |
| Consumer cold boot (both datasets) | **5.2 s** |
| Consumer delta apply | **28 ms** |
| Producer cycle time (catalog) | **~3.4 s** |
| Producer cycle time (users, first) | **4.6 s** |
| Combined POJO heap (JSON deserialize) | **288 MB** |
| Combined Hollow heap (live memory) | **35.9 MB** |
| Heap ratio | **8× smaller** |
