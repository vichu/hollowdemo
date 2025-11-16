package org.vish.hollowdemo.producer

import com.netflix.hollow.api.producer.HollowProducer
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.vish.hollowdemo.model.Movie
import java.io.File
import java.time.LocalDateTime

@Service
@Profile("producer")
class MovieProducer(
    private val movieDataGenerator: MovieDataGenerator
) {
    private val logger = LoggerFactory.getLogger(MovieProducer::class.java)
    private val hollowDataPath = File(System.getenv("HOLLOW_DATA_PATH") ?: "./hollow-data")
    private lateinit var producer: HollowProducer
    private var currentMovies: List<Movie> = emptyList()
    private var cycleCount = 0L

    @PostConstruct
    fun initialize() {
        // Ensure hollow data directory exists
        hollowDataPath.mkdirs()

        // Initialize Hollow producer with filesystem publisher
        producer = HollowProducer.withPublisher(HollowFilesystemPublisher(hollowDataPath.toPath()))
            .withAnnouncer(HollowFilesystemAnnouncer(hollowDataPath.toPath()))
            .build()
        

        logger.info("MovieProducer initialized. Data will be published to: ${hollowDataPath.absolutePath}")

        // Run first cycle immediately
        runProductionCycle()
    }

    @Scheduled(fixedDelay = 420000, initialDelay = 420000) // Every 7 minutes
    fun runProductionCycle() {
        try {
            val startTime = System.currentTimeMillis()
            cycleCount++

            logger.info("========================================")
            logger.info("Starting producer cycle #$cycleCount at ${LocalDateTime.now()}")

            // Generate or update dataset
            currentMovies = if (currentMovies.isEmpty()) {
                logger.info("Generating initial dataset...")
                movieDataGenerator.generateInitialDataset()  // Uses default: 50,000
            } else {
                logger.info("Generating updated dataset...")
                movieDataGenerator.generateUpdatedDataset(currentMovies)
            }

            logger.info("Dataset prepared: ${currentMovies.size} movies")

            // Run Hollow cycle - this automatically calculates diffs and creates deltas
            producer.runCycle { writeState ->
                currentMovies.forEach { movie ->
                    writeState.add(movie)
                }
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("Producer cycle #$cycleCount completed in ${duration}ms")
            logger.info("Published ${currentMovies.size} movies to Hollow")
            logger.info("Snapshot and delta available at: ${hollowDataPath.absolutePath}")
            logger.info("========================================")

        } catch (e: Exception) {
            logger.error("Error during producer cycle", e)
        }
    }

    fun getCurrentStats(): Map<String, Any> {
        return mapOf(
            "cycleCount" to cycleCount,
            "movieCount" to currentMovies.size,
            "dataPath" to hollowDataPath.absolutePath
        )
    }

    fun forcePublish(): Map<String, Any> {
        logger.info("Force publish triggered manually")
        runProductionCycle()
        return mapOf(
            "status" to "success",
            "message" to "Production cycle triggered successfully",
            "cycleCount" to cycleCount,
            "movieCount" to currentMovies.size
        )
    }
}
