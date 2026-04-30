# Hollow Demo - Versioned Dataset Distribution

Demo application showcasing [Netflix Hollow](https://hollow.how), an open-source library that enables efficient distribution of GB-scale datasets using a Git-like snapshot + delta model.

This demo uses real AWS infrastructure (S3 + DynamoDB) via the [hollow-aws](https://github.com/vichu/hollow-infra-adapters) library.

---

## 📋 What This Demo Shows

1. **Producer**: Generates a rich movie catalog (50,000 movies) and publishes versioned snapshots + deltas to S3
2. **Efficient Updates**: Snapshots are ~10 MB, but deltas are only ~650 KB — showcasing massive transfer savings
3. **AWS-backed distribution**: Producer announces versions via DynamoDB; consumers poll and auto-apply deltas
4. **Auto-Scheduling**: Producer runs every 7 minutes, simulating real-world updates
5. **Force Publish**: Manual trigger endpoint to publish on demand without waiting

---

## 🏗️ Prerequisites

- Docker and Docker Compose
- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.0
- AWS account with an IAM user (not root) and `AdministratorAccess` policy
- AWS CLI configured: `aws configure --profile vish-personal-aws`

---

## 🚀 Demo Runbook

### Step 1 — Provision AWS Infrastructure

From the [hollow-infra-adapters](https://github.com/vichu/hollow-infra-adapters) repo:

```bash
cd /path/to/hollow-infra-adapters/terraform/aws

cat > terraform.tfvars <<EOF
namespace   = "hollow"
environment = "demo"
region      = "us-east-1"

tags = {
  Project = "hollowdemo"
  Owner   = "vish"
}
EOF

terraform init && terraform apply
```

This creates 11 resources in ~60 seconds:
- **S3 bucket**: `hollow-demo-blobs` — stores snapshots and deltas
- **DynamoDB table**: `hollow-demo-announcements` — stores latest version per dataset
- **IAM roles**: `hollow-demo-hollow-producer` and `hollow-demo-hollow-consumer`

### Step 2 — Configure Credentials

Create a `.env` file in this project (gitignored):

```bash
cat > .env <<EOF
HOLLOW_AWS_REGION=us-east-1
HOLLOW_AWS_BUCKET=hollow-demo-blobs
HOLLOW_AWS_DYNAMODB_TABLE=hollow-demo-announcements
HOLLOW_AWS_DATASET_ID=movies
AWS_ACCESS_KEY_ID=<your-iam-user-access-key>
AWS_SECRET_ACCESS_KEY=<your-iam-user-secret-key>
EOF
```

### Step 3 — Start the Demo

```bash
./demo-reset.sh
```

This builds the containers and starts both producer and consumer fresh.

### Step 4 — Access the APIs

**Producer (port 9080):**
```bash
curl http://localhost:9080/producer/stats
curl -X POST http://localhost:9080/producer/publish   # Force a new cycle
```

**Consumer (port 9081):**
```bash
curl http://localhost:9081/movies
curl http://localhost:9081/movies/1
curl http://localhost:9081/movies/search?title=Glass
curl http://localhost:9081/movies/genre/Action
curl http://localhost:9081/movies/stats
```

### Step 5 — Verify in AWS

```bash
# S3 — see snapshots and deltas
aws s3 ls s3://hollow-demo-blobs/movies/ --human-readable

# DynamoDB — see the latest announced version
aws dynamodb get-item \
  --table-name hollow-demo-announcements \
  --key '{"dataset_id": {"S": "movies"}}' \
  --region us-east-1
```

---

## 🔄 Resetting Between Demo Runs

To start completely fresh while keeping AWS infrastructure intact:

```bash
./demo-reset.sh
```

This stops containers, wipes S3 blobs and the DynamoDB version record, then rebuilds and restarts everything.

---

## 🧹 Full Teardown (After Demo)

To tear down everything including AWS infrastructure:

```bash
./demo-reset.sh --teardown
```

This stops containers, then destroys all 11 Terraform resources.

> **Note on S3 versioning**: The Terraform module enables S3 bucket versioning, which means `aws s3 rm` only creates delete markers — it does not remove the underlying object versions. Terraform will refuse to delete a bucket that still has versions in it. The `--teardown` flag handles this automatically: it uses `s3api delete-objects` to purge all object versions and delete markers before handing off to `terraform destroy`.

### Summary of `demo-reset.sh` flags

| Command | What it does |
|---|---|
| `./demo-reset.sh` | Stop → clear data → rebuild → start |
| `./demo-reset.sh --clean-only` | Stop → clear data (don't restart) |
| `./demo-reset.sh --teardown` | Stop → purge all S3 versions → destroy Terraform infra |

---

## 🎬 Dataset

The demo generates 50,000 movies with rich metadata including title, genre, rating, release year, duration, description, director, cast, writers, production company, budget, box office, tags, age rating, and awards.

Each cycle simulates real-world changes: ~1.5% removals, ~4% updates (rating changes), ~5% new additions — net growth of ~3.4% per cycle.

---

## 🏗️ Project Structure

```
src/main/
├── java/org/vish/hollowdemo/
│   ├── api/                        # Generated Hollow Consumer API (do not edit!)
│   │   ├── MovieAPI.java           # Main API class
│   │   ├── Movie.java              # Type-safe Movie wrapper
│   │   └── ...                     # Other generated classes
│   └── codegen/
│       └── GenerateMovieAPI.java   # API generator utility
│
└── kotlin/org/vish/hollowdemo/
    ├── model/
    │   └── Movie.kt                # Data model with @HollowPrimaryKey
    ├── producer/
    │   ├── MovieDataGenerator.kt   # Generates sample movie data
    │   ├── MovieProducer.kt        # Publishes snapshots/deltas to S3
    │   └── ProducerController.kt   # REST endpoints for producer
    ├── consumer/
    │   ├── MovieConsumer.kt        # Loads from S3, watches DynamoDB for updates
    │   ├── MovieController.kt      # REST API endpoints
    │   └── GenreQuery.kt           # Hash index query bean
    └── HollowdemoApplication.kt    # Spring Boot entry point
```

---

## 🎯 Key Hollow Concepts Demonstrated

### 1. Producer Pattern

The producer adds all movies every cycle — Hollow automatically calculates the diff:

```kotlin
producer.runCycle { writeState ->
    currentMovies.forEach { movie ->
        writeState.add(movie)  // Add ALL movies (not just changes)
    }
}
// Hollow diffs against the previous state and creates a delta automatically
```

### 2. AWS-backed Publisher and Announcer

```kotlin
val config = HollowAwsConfig.builder().build()  // reads from env vars

HollowProducer.withPublisher(HollowS3Publisher.create(config))
    .withAnnouncer(HollowDynamoDBAnnouncer.create(config))
    .build()
```

Snapshots and deltas land in `s3://hollow-demo-blobs/movies/`. Each new version is announced atomically in DynamoDB.

### 3. Consumer with Generated API

```kotlin
val consumer = HollowConsumer.withBlobRetriever(HollowS3BlobRetriever.create(config))
    .withAnnouncementWatcher(HollowDynamoDBAnnouncementWatcher.create(config))
    .withGeneratedAPIClass(MovieAPI::class.java)
    .build()

// Fast O(1) lookup by ID using primary key index
val movieIndex = Movie.uniqueIndex(consumer)
val movie = movieIndex.findMatch(id)
```

The announcement watcher polls DynamoDB every 5 seconds and triggers an async delta refresh when a new version is detected.

### 4. Versioned Snapshots

Each producer cycle creates:
- **Snapshot**: Complete dataset for cold starts (`snapshot-<version>`)
- **Delta**: Only changes since the previous version (`delta-<from>-<to>`)
- **Reverse Delta**: For rollback support

---

## 📊 Performance Metrics

| Metric | Value |
|--------|-------|
| Initial dataset | 50,000 movies with rich metadata |
| Snapshot size | ~10 MB (compressed binary format) |
| Delta size | ~650 KB (~3.4% growth per cycle) |
| Compression ratio | ~17x smaller than snapshot |
| Publish cycle time | ~1–1.5 seconds |
| Update frequency | Every 7 minutes (configurable) |

**Why deltas are so much smaller:**

In each update cycle only ~10% of records change. A delta contains only those changes — unchanged records are not transmitted at all.

For 100 consumer instances: 1 GB full reload → 65 MB delta = **94% reduction in data transfer**.

---

## 💡 The Big Idea

Traditional caches force painful trade-offs:
- **Full reloads**: 2x memory spikes, GC pauses, minutes of downtime
- **Incremental updates**: Complex CDC pipelines to build and maintain

Hollow treats datasets like Git treats code:
- **Snapshot**: Complete state at a point in time
- **Delta**: Only the changes between versions
- **Immutable**: Versions never mutate in place
- **Efficient**: Transfer and apply only what changed

Perfect for: 1 GB–10 GB datasets, read-heavy workloads, many application instances.

---

## 🔗 Resources

- **Hollow Documentation**: https://hollow.how
- **Hollow GitHub**: https://github.com/Netflix/hollow
- **hollow-aws library**: https://github.com/vichu/hollow-infra-adapters

---

**Built with**: Spring Boot 3.5.7 • Kotlin 1.9.25 • Hollow 7.14.39 • hollow-aws 0.1.0 • Java 21
