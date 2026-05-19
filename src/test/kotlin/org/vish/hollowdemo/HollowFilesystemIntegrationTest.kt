package org.vish.hollowdemo

import com.netflix.hollow.api.consumer.HollowConsumer
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever
import com.netflix.hollow.api.producer.HollowProducer
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.vish.hollowdemo.api.Movie
import org.vish.hollowdemo.api.MovieAPI
import org.vish.hollowdemo.api.users.User
import org.vish.hollowdemo.api.users.UserAPI
import org.vish.hollowdemo.producer.CatalogDataGenerator
import org.vish.hollowdemo.producer.UserDataGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID.nameUUIDFromBytes

/**
 * End-to-end tests using Hollow's built-in filesystem publisher/retriever.
 *
 * No AWS credentials, no mocking — the same producer/consumer code that runs
 * against S3+DynamoDB in production runs here against a local temp directory.
 * That is the point: Hollow's abstraction makes the storage layer swappable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Hollow Filesystem Integration")
class HollowFilesystemIntegrationTest {

    companion object {
        // Small counts keep tests fast while still exercising the full pipeline
        const val CATALOG_SIZE = 200
        const val USER_SIZE = 100
    }

    private val catalogDir: Path = Files.createTempDirectory("hollow-catalog-test")
    private val usersDir: Path = Files.createTempDirectory("hollow-users-test")

    private var catalogVersion = 0L
    private var usersVersion = 0L

    // ── Setup ────────────────────────────────────────────────────────────────

    @BeforeAll
    fun publishDatasets() {
        // Catalog dataset — published independently by CatalogProducer in production
        val catalogProducer = HollowProducer
            .withPublisher(HollowFilesystemPublisher(catalogDir))
            .withAnnouncer(HollowFilesystemAnnouncer(catalogDir))
            .build()

        catalogVersion = catalogProducer.runCycle { writeState ->
            CatalogDataGenerator(CATALOG_SIZE)
                .generateInitialDataset()
                .forEach { movie -> writeState.add(movie) }
        }

        // Users dataset — published independently by UserProducer in production
        val userProducer = HollowProducer
            .withPublisher(HollowFilesystemPublisher(usersDir))
            .withAnnouncer(HollowFilesystemAnnouncer(usersDir))
            .build()

        usersVersion = userProducer.runCycle { writeState ->
            UserDataGenerator(userCount = USER_SIZE, maxMovieId = CATALOG_SIZE.toLong())
                .generateUsers()
                .forEach { user -> writeState.add(user) }
        }
    }

    // Helpers that mirror what CatalogConsumer / UserConsumer do at startup
    private fun catalogConsumer(): HollowConsumer = HollowConsumer
        .withBlobRetriever(HollowFilesystemBlobRetriever(catalogDir))
        .withGeneratedAPIClass(MovieAPI::class.java)
        .build()
        .also { it.triggerRefreshTo(catalogVersion) }

    private fun usersConsumer(): HollowConsumer = HollowConsumer
        .withBlobRetriever(HollowFilesystemBlobRetriever(usersDir))
        .withGeneratedAPIClass(UserAPI::class.java)
        .build()
        .also { it.triggerRefreshTo(usersVersion) }

    // ── Snapshot tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("catalog snapshot: contains exactly CATALOG_SIZE movies")
    fun catalogSnapshotCount() {
        val api = catalogConsumer().getAPI(MovieAPI::class.java)
        assertEquals(CATALOG_SIZE, api.allMovie.toList().size)
    }

    @Test
    @DisplayName("users snapshot: contains exactly USER_SIZE users")
    fun usersSnapshotCount() {
        val api = usersConsumer().getAPI(UserAPI::class.java)
        assertEquals(USER_SIZE, api.allUser.toList().size)
    }

    // ── Index tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("O(1) movie lookup: primary key index finds movie by ID")
    fun moviePrimaryKeyLookup() {
        val consumer = catalogConsumer()
        val movieIndex = Movie.uniqueIndex(consumer)

        val movie = movieIndex.findMatch(1L)
        assertTrue(movie != null, "Movie with ID 1 should exist")
        assertTrue(movie!!.title?.value?.isNotBlank() == true)
        assertTrue(movie.rating in 5.0..10.0)
    }

    @Test
    @DisplayName("O(1) user lookup: primary key index finds user by UUID")
    fun userPrimaryKeyLookup() {
        val consumer = usersConsumer()
        val userIndex = User.uniqueIndex(consumer)

        // The first UUID is deterministic — derived from "user-1" with nameUUIDFromBytes
        val expectedId = nameUUIDFromBytes("user-1".toByteArray()).toString()
        val user = userIndex.findMatch(expectedId)

        assertTrue(user != null, "User with deterministic UUID should exist")
        assertTrue(user!!.firstName?.value?.isNotBlank() == true)
        assertTrue(user.planName?.value in listOf("AD_PLAN", "STANDARD", "PREMIUM"))
    }

    // ── Cross-dataset join ───────────────────────────────────────────────────

    @Test
    @DisplayName("cross-dataset join: recently-watched IDs resolve to full Movie objects across two independent snapshots")
    fun crossDatasetJoin() {
        // Two separate HollowConsumers — each reads from its own snapshot
        val catalogC = catalogConsumer()
        val usersC = usersConsumer()

        val movieIndex = Movie.uniqueIndex(catalogC)
        val userIndex = User.uniqueIndex(usersC)

        // Pick the first user (deterministic — seed=42 always gives the same user-1)
        val firstUserId = nameUUIDFromBytes("user-1".toByteArray()).toString()
        val hollowUser = userIndex.findMatch(firstUserId)!!
        val movieIds = hollowUser.recentlyWatchedMovieIds?.map { it.value } ?: emptyList()

        assertTrue(movieIds.isNotEmpty(), "User should have recently-watched movies")

        // The join: resolve each movie ID against the catalog snapshot
        // maxMovieId was set to CATALOG_SIZE, so every ID is guaranteed to be in range
        val resolvedMovies = movieIds.mapNotNull { movieIndex.findMatch(it) }

        assertEquals(
            movieIds.size,
            resolvedMovies.size,
            "Every recently-watched ID should resolve — IDs are bounded by CATALOG_SIZE"
        )

        // Verify the resolved movies have real content
        resolvedMovies.forEach { movie ->
            assertTrue(movie.title?.value?.isNotBlank() == true)
            assertTrue(movie.id in 1L..CATALOG_SIZE.toLong())
        }
    }

    // ── Determinism tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("determinism: CatalogDataGenerator always produces the same dataset from seed=99")
    fun catalogGenerationIsDeterministic() {
        val movies1 = CatalogDataGenerator(CATALOG_SIZE).generateInitialDataset()
        val movies2 = CatalogDataGenerator(CATALOG_SIZE).generateInitialDataset()

        assertEquals(movies1.size, movies2.size)
        assertEquals(movies1[0], movies2[0], "First movie must be identical across runs")
        assertEquals(movies1, movies2, "Entire catalog must be identical across runs")
    }

    @Test
    @DisplayName("determinism: UserDataGenerator always produces the same dataset from seed=42")
    fun userGenerationIsDeterministic() {
        val gen = UserDataGenerator(userCount = USER_SIZE, maxMovieId = CATALOG_SIZE.toLong())
        val users1 = gen.generateUsers()
        val users2 = gen.generateUsers()

        assertEquals(users1.size, users2.size)
        assertEquals(users1[0], users2[0], "First user must be identical across runs")
        assertEquals(users1, users2, "Entire user list must be identical across runs")
    }

    // ── Delta tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("delta: second producer cycle publishes a smaller delta instead of a full snapshot")
    fun secondCycleProducesDelta() {
        val dir = Files.createTempDirectory("hollow-delta-test")
        val gen = CatalogDataGenerator(CATALOG_SIZE)
        val producer = HollowProducer
            .withPublisher(HollowFilesystemPublisher(dir))
            .withAnnouncer(HollowFilesystemAnnouncer(dir))
            .build()

        // Cycle 1 — full snapshot
        val initialMovies = gen.generateInitialDataset()
        val v1 = producer.runCycle { ws -> initialMovies.forEach { ws.add(it) } }

        // Cycle 2 — only changed records are in the delta
        val updatedMovies = gen.generateUpdatedDataset(initialMovies)
        val v2 = producer.runCycle { ws -> updatedMovies.forEach { ws.add(it) } }

        assertTrue(v2 > v1, "Version must increment after each cycle")

        // The delta (v2 - v1) file must be smaller than the full snapshot (v1)
        val snapshotSize = Files.list(dir)
            .filter { it.fileName.toString().contains("snapshot") }
            .mapToLong { Files.size(it) }
            .max().orElse(0)

        val deltaSize = Files.list(dir)
            .filter { it.fileName.toString().contains("delta") }
            .mapToLong { Files.size(it) }
            .max().orElse(0)

        assertTrue(deltaSize < snapshotSize,
            "Delta ($deltaSize bytes) should be smaller than the full snapshot ($snapshotSize bytes)")

        // Consumer can load the updated state from the delta
        val consumer = HollowConsumer
            .withBlobRetriever(HollowFilesystemBlobRetriever(dir))
            .withGeneratedAPIClass(MovieAPI::class.java)
            .build()
        consumer.triggerRefreshTo(v2)

        val api = consumer.getAPI(MovieAPI::class.java)
        assertEquals(updatedMovies.size, api.allMovie.toList().size)
    }
}
