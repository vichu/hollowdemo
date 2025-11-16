# Hollow Demo - Versioned Dataset Distribution

Demo application showcasing [Netflix Hollow](https://hollow.how), an open-source library that enables efficient distribution of GB-scale datasets using a Git-like snapshot + delta model.

## 🚀 Quick Start

```bash
# Start with Docker Compose (recommended)
docker compose up --build

# Access the APIs
curl http://localhost:9080/producer/stats  # Producer
curl http://localhost:9081/movies/stats    # Consumer

# Stop and cleanup
docker compose down -v
```

## 📋 What This Demo Shows

1. **Producer**: Generates rich movie catalog data (50,000 movies with full metadata) and publishes versioned snapshots + deltas
2. **Efficient Updates**: Snapshots are tens of MBs, but deltas are only a few hundred KB - showcasing massive savings
3. **Auto-Scheduling**: Producer runs every 7 minutes, simulating real-world updates
4. **Zero-Copy Updates**: In-place delta application with no memory spikes
5. **Force Publish**: Manual trigger endpoint to publish on demand without waiting

## 🎬 Dataset

The demo generates 50,000 Netflix movies with rich metadata including:
- *Glass Onion: A Knives Out Mystery*
- *The Irishman*
- *Don't Look Up*
- *The Kissing Booth 3: Ultimate Kissing Championship*
- *Army of Slightly Annoyed People*
- *Glass Onion 2: The Avocado*

**Rich Movie Metadata:**
- Title, genre, rating, release year, duration
- Full description (genre-specific templates)
- Director and cast (3-8 actors)
- Writers, production company, country, language
- Budget and box office revenue
- Tags, age rating, and awards
- Lists of cast members, writers, and tags

Each cycle simulates real-world changes: ~2% removals, ~4% updates (rating changes), ~3% new additions.

## 🏗️ Project Structure

```
src/main/
├── java/org/vish/hollowdemo/
│   ├── api/                        # Generated Hollow Consumer API (do not edit!)
│   │   ├── MovieAPI.java           # Main API class
│   │   ├── Movie.java              # Type-safe Movie wrapper
│   │   ├── HString.java            # String wrapper
│   │   └── ...                     # Other generated classes
│   └── codegen/
│       └── GenerateMovieAPI.java   # API generator utility
│
└── kotlin/org/vish/hollowdemo/
    ├── model/
    │   └── Movie.kt                # Data model with @HollowPrimaryKey
    ├── producer/                   # Producer side (publishes snapshots)
    │   ├── MovieDataGenerator.kt   # Generates sample Netflix movie data
    │   └── MovieProducer.kt        # Hollow producer - creates snapshots & deltas
    ├── consumer/                   # Consumer side (loads snapshots)
    │   ├── MovieConsumer.kt        # Hollow consumer - loads and queries data
    │   └── MovieController.kt      # REST API endpoints
    └── HollowdemoApplication.kt    # Spring Boot entry point

hollow-data/                        # Published Hollow blobs (created on run)
├── snapshot-*                      # Full dataset snapshots (8.5KB)
├── delta-*                         # Incremental changes (502 bytes!)
├── reversedelta-*                  # Rollback support
└── announced.version               # Current version pointer
```

## 🚀 Running the Demo

### Method 1: Docker Compose (Recommended)

The easiest way to run the demo is with Docker Compose, which handles all dependencies and runs producer and consumer as separate containers.

#### Prerequisites
- Docker and Docker Compose

#### Start the Services

```bash
# Build and start both producer and consumer
docker compose up --build
```

This will:
- Build the application with Java 21 and Gradle
- Start the **producer** on port **9080**
- Start the **consumer** on port **9081**
- Create a shared `./hollow-data` directory for data synchronization
- Both containers share the same local directory for Hollow data files

#### Access the APIs

**Producer API (port 9080):**
```bash
# Get producer stats
curl http://localhost:9080/producer/stats

# Force publish a new version
curl -X POST http://localhost:9080/producer/publish
```

**Consumer API (port 9081):**
```bash
# Get all movies
curl http://localhost:9081/movies

# Get movie by ID
curl http://localhost:9081/movies/1

# Search by title
curl http://localhost:9081/movies/search?title=Glass

# Get movies by genre
curl http://localhost:9081/movies/genre/Action

# Get consumer stats
curl http://localhost:9081/movies/stats
```

#### Monitor Logs

```bash
# View all logs
docker compose logs -f

# View producer logs only
docker compose logs -f producer

# View consumer logs only
docker compose logs -f consumer
```

#### Stop the Services

```bash
# Stop and remove containers (data persists in ./hollow-data)
docker compose down

# Stop and remove containers + delete local data
docker compose down && rm -rf ./hollow-data
```

---

### Method 2: Local Development

For local development and debugging, you can run the services directly with Gradle.

#### Prerequisites
- Java 21
- Gradle 8.14.3 (or use included wrapper)

#### Step 1: Generate Hollow Consumer API

Before running the demo, generate the type-safe consumer API:

```bash
./gradlew generateHollowAPI
```

This creates Java classes in `src/main/java/org/vish/hollowdemo/api/` including:
- `MovieAPI.java` - Main API for accessing movies
- `Movie.java` - Type-safe movie wrapper with getters
- `HString.java` - String wrapper
- Primary key index support

**Note**: In production, consumers typically live in a separate project and use the generated API as a dependency.

### Step 2: Build the Project

```bash
./gradlew clean build -x test
```

### Step 3: Run Producer & Consumer

#### Option 1: Separate Processes (Recommended for Demo)

Run producer and consumer in **separate terminals** to clearly show the producer-consumer pattern:

**Terminal 1 - Producer:**
```bash
./gradlew bootRun --args='--spring.profiles.active=producer'
```

Wait for the producer to create the initial snapshot (~5 seconds), then in a new terminal:

**Terminal 2 - Consumer:**
```bash
./gradlew bootRun --args='--spring.profiles.active=consumer' --args='--server.port=8081'
```

The consumer will:
- Load the snapshot created by the producer
- Start watching for updates
- Auto-apply deltas when the producer publishes new versions
- Expose REST API on port 8081

### Option 2: Combined Mode

Run both producer and consumer in a single process:

```bash
./gradlew bootRun --args='--spring.profiles.active=producer,consumer'
```

### What to Observe

**Producer Terminal - Cycle #1:**
```
[INFO] MovieProducer initialized. Data will be published to: ./hollow-data
[INFO] Starting producer cycle #1
[INFO] Generating initial dataset...
[INFO] Dataset prepared: 100 movies
[INFO] Producer cycle #1 completed in 22ms
[INFO] Published 100 movies to Hollow
```

**Consumer Terminal - Initial Load:**
```
[INFO] Initializing MovieConsumer...
[INFO] Watching for data at: ./hollow-data
[INFO] ✅ Update successful: v0 -> v20251109223219001
[INFO]    Now serving 100 movies (update #1)
[INFO] MovieConsumer initialized successfully
```

**Producer Terminal - Cycle #2 (after 2 minutes):**
```
[INFO] Starting producer cycle #2
[INFO] Generating updated dataset...
[INFO] Dataset prepared: 103 movies
[INFO] Producer cycle #2 completed in 25ms
```

**Consumer Terminal - Auto Delta Update:**
```
[INFO] 🔄 Update started: v20251109223219001 -> v20251109223419002
[INFO] ✅ Update successful: v20251109223219001 -> v20251109223419002
[INFO]    Now serving 103 movies (update #2)
```

### Query the Consumer API

Once the consumer is running, query it via REST (port 8081):

```bash
# Get all movies
curl http://localhost:8081/movies

# Get movie by ID
curl http://localhost:8081/movies/1

# Search by title
curl http://localhost:8081/movies/search?title=Glass

# Get movies by genre
curl http://localhost:8081/movies/genre/Action

# Get consumer stats
curl http://localhost:8081/movies/stats
```

**Sample Response (stats endpoint):**
```json
{
  "totalMovies": 103,
  "currentVersion": 20251109223419002,
  "lastUpdateTime": "2025-11-09T14:34:19.560",
  "updateCount": 2,
  "genreBreakdown": {
    "Action": 15,
    "Comedy": 12,
    "Drama": 18,
    ...
  },
  "averageRating": "7.42"
}
```

### Verify Snapshot & Delta Files

```bash
ls -lh ./hollow-data/

# You should see:
# snapshot-20251109223219001    (8.5K)  - First full snapshot
# snapshot-20251109223419002    (8.5K)  - Second snapshot (for new consumers)
# delta-20251109223219001-*     (502B)  - Delta between versions!
```

## 📦 Why is the Delta 17x Smaller?

**Snapshot (8.5 KB)** contains:
- All 100 movies
- Complete data for each: id, title, genre, rating, releaseYear, duration
- Efficient binary encoding (not JSON)
- Compressed format

**Delta (502 bytes)** contains only:
- **Removed**: 2 movies → Just their ordinal references
- **Added**: 3 new movies → Full data only for these
- **Modified**: 4 movies → Only changed fields (e.g., rating: 7.5 → 8.1)
- **Unchanged**: 95 movies → **Not included at all!**

### The Math

In a typical update cycle:
- **2% removed** (2 movies) = ~20 bytes (ordinal references)
- **4% modified** (4 movies) = ~200 bytes (only changed fields)
- **3% added** (3 movies) = ~250 bytes (new movie data)
- **Overhead** = ~32 bytes (version metadata, checksums)

**Total**: ~502 bytes vs 8,500 bytes = **~17x smaller**

### Why This Matters at Scale

For a real-world 2 GB product catalog with 10M products:

| Update Type | Traditional | Hollow Delta |
|-------------|-------------|--------------|
| Full reload | 2 GB | - |
| 0.18% change | 2 GB | ~4 MB |
| Network transfer | 2 GB per node | 4 MB per node |
| Memory spike | 4 GB (2x) | 2.01 GB (no spike) |
| Update time | 90 seconds | 3 seconds |

**For 10 application instances**: 20 GB transferred → 40 MB transferred (500x reduction!)

### Hollow's Encoding Efficiency

1. **Ordinal-based references**: Records referenced by integer ordinals, not IDs
2. **Type-specific encoding**: Optimized binary format per field type
3. **Shared string pooling**: Common strings stored once and referenced
4. **Delta transitions**: Only transmits state changes between versions
5. **No serialization overhead**: Direct memory-mapped format

This is why Hollow excels at GB-scale datasets where even "small" changes become expensive with traditional approaches.

## 🎯 Key Hollow Concepts Demonstrated

### 1. Producer Pattern
The `MovieProducer` runs independently from consumers:
```kotlin
producer.runCycle { writeState ->
    currentMovies.forEach { movie ->
        writeState.add(movie)  // Add ALL movies (not just changes)
    }
}
// Hollow automatically calculates diffs and creates deltas
```

### 2. Versioned Snapshots
Each cycle creates:
- **Snapshot**: Complete dataset for cold starts
- **Delta**: Only changes since previous version
- **Reverse Delta**: For rollback support

### 3. Filesystem Publisher
```kotlin
HollowProducer.withPublisher(HollowFilesystemPublisher(hollowDataPath.toPath()))
    .withAnnouncer(HollowFilesystemAnnouncer(hollowDataPath.toPath()))
    .build()
```
For production, swap with:
- `S3Publisher` for AWS S3
- `GCSPublisher` for Google Cloud Storage
- Custom implementations

## 📊 Performance Metrics

| Metric | Value |
|--------|-------|
| Initial dataset | 50,000 movies with rich metadata |
| Snapshot size | **10 MB** (compressed binary format) |
| Delta size | **624-668 KB** (~3.4% growth per cycle) |
| Reverse delta | 248-266 KB (for rollback support) |
| Compression ratio | **~17x smaller** (11 MB → 648 KB) |
| Data generation | 50,000 movies in ~0.5 seconds |
| Publish cycle time | ~0.95-1.4 seconds |
| Update frequency | Every 7 minutes (configurable) |
| Manual trigger | Available via REST endpoint |

**Growth Pattern (Dataset Continuously Grows):**
- Cycle 1: 50,000 movies → 10 MB snapshot
- Cycle 2: 51,712 movies (+3.4%) → 11 MB snapshot, 624 KB delta
- Cycle 3: 53,483 movies (+3.4%) → 11 MB snapshot, 648 KB delta
- Cycle 4: 55,315 movies (+3.4%) → 12 MB snapshot, 668 KB delta

**Why Deltas Grow with the Dataset:**
Each cycle simulates realistic catalog growth by **adding more content than it removes**:
- Removes ~1.5% (750 movies)
- Updates ~4% (2,000 movies)
- Adds ~5% (2,500+ movies)
- **Net growth: +3.4% per cycle**

**The Savings:**
- **For 100 consumer instances**: 1.1 GB full reload → 64.8 MB delta (94% reduction)
- Even with continuous growth, deltas remain **17x smaller** than snapshots

## 🔧 Configuration

### Producer Schedule
Adjust update frequency in `MovieProducer.kt`:
```kotlin
@Scheduled(fixedDelay = 420000, initialDelay = 420000) // 7 minutes
```

### Dataset Size
Change movie count in `MovieDataGenerator.kt`:
```kotlin
fun generateInitialDataset(count: Int = 50000)
```

### Force Publish Endpoint
Trigger a producer cycle immediately without waiting:

**Docker Compose:**
```bash
curl -X POST http://localhost:9080/producer/publish
```

**Local Development:**
```bash
curl -X POST http://localhost:8080/producer/publish
```

**Response:**
```json
{
  "status": "success",
  "message": "Production cycle triggered successfully",
  "cycleCount": 3,
  "movieCount": 50243
}
```

### Producer Stats
Check current producer status:

**Docker Compose:**
```bash
curl http://localhost:9080/producer/stats
```

**Local Development:**
```bash
curl http://localhost:8080/producer/stats
```

## 🎯 Key Hollow Concepts Demonstrated

### 1. Producer Pattern
The `MovieProducer` runs independently from consumers:
```kotlin
producer.runCycle { writeState ->
    currentMovies.forEach { movie ->
        writeState.add(movie)  // Add ALL movies (not just changes)
    }
}
// Hollow automatically calculates diffs and creates deltas
```

### 2. Consumer with Generated API
The `MovieConsumer` uses the generated type-safe API:
```kotlin
// Load snapshot and watch for updates
consumer = HollowConsumer.withBlobRetriever(blobRetriever)
    .withAnnouncementWatcher(announcementWatcher)
    .withGeneratedAPIClass(MovieAPI::class.java)
    .build()

// Get the API and create index
api = consumer.getAPI(MovieAPI::class.java)
movieIndex = org.vish.hollowdemo.api.Movie.uniqueIndex(consumer)

// Fast O(1) lookup by ID using primary key index
val movie = movieIndex.findMatch(id)
```

### 3. Separate Packages
- **Producer** (`org.vish.hollowdemo.producer`): Publishes snapshots
- **Consumer** (`org.vish.hollowdemo.consumer`): Consumes snapshots via generated API
- **API** (`org.vish.hollowdemo.api`): Generated type-safe classes

In production, these would typically be separate projects.

## 🧹 Cleanup

### Docker Compose Cleanup

#### Stop and Clean Up Containers

```bash
# Stop containers (data persists in ./hollow-data)
docker compose down

# Stop containers and remove local data directory
docker compose down && rm -rf ./hollow-data

# Remove everything including images (forces rebuild on next start)
docker compose down --rmi all && rm -rf ./hollow-data
```

#### View Data Files

Since data is stored locally, you can inspect the Hollow files directly:

```bash
# List all Hollow data files
ls -lh ./hollow-data/

# View current version
cat ./hollow-data/announced.version

# Check snapshot and delta sizes
du -h ./hollow-data/snapshot-*
du -h ./hollow-data/delta-*
```

---

### Local Development Cleanup

To reset the demo when running locally, use one of these methods:

#### Option 1: Shell Script (Interactive)
```bash
./cleanup-hollow-data.sh
```
This script will show you the current size and ask for confirmation before deleting.

#### Option 2: Gradle Task (Non-interactive)
```bash
./gradlew cleanupHollowData
```

#### Option 3: Manual
```bash
rm -rf ./hollow-data
```

After cleanup, restart the producer first, then the consumer.

---

### What Happens After Cleanup

**On restart:**
- Producer creates a new initial snapshot (version 1)
- All previous versions and deltas are gone
- Consumers will load the new snapshot from scratch

**Important Notes:**
- Data is stored in `./hollow-data` on your local machine
- The directory persists after `docker compose down` for easy inspection
- You can manually delete `./hollow-data` to reset the demo
- In production, deleting blob storage would cause consumer outages
- Always ensure consumers are stopped or have a migration plan before removing published data

## 🔗 Resources

- **Hollow Documentation**: http://hollow.how
- **GitHub**: https://github.com/Netflix/hollow
- **Netflix Tech Blog**: http://techblog.netflix.com/2016/12/netflixoss-announcing-hollow.html

## 💡 The Big Idea

Traditional caches force painful trade-offs:
- **Cold starts**: Minutes to warm up
- **Full reloads**: 2x memory spikes, GC pauses
- **Incremental updates**: Complex CDC infrastructure

Hollow treats datasets like Git treats code:
- **Snapshot**: Complete state at a point in time
- **Delta**: Only the changes
- **Immutable**: Versions never change
- **Efficient**: Transfer & apply only differences

Perfect for: 1GB-10GB datasets, read-heavy workloads, multiple application instances.

---

**Built with**: Spring Boot 3.5.7 • Kotlin 1.9.25 • Hollow 7.1.0 • Java 21
