package org.vish.hollowdemo.producer

import com.netflix.hollow.api.producer.HollowProducer
import io.github.vichu.hollow.aws.announcer.HollowDynamoDBAnnouncer
import io.github.vichu.hollow.aws.config.HollowAwsConfig
import io.github.vichu.hollow.aws.publisher.HollowS3Publisher
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.vish.hollowdemo.model.Movie
import java.time.LocalDateTime

@Service
@Profile("producer")
class MovieProducer(
    private val movieDataGenerator: MovieDataGenerator
) {
    private val logger = LoggerFactory.getLogger(MovieProducer::class.java)
    private lateinit var producer: HollowProducer
    private lateinit var awsConfig: HollowAwsConfig
    private var currentMovies: List<Movie> = emptyList()
    private var cycleCount = 0L

    @PostConstruct
    fun initialize() {
        awsConfig = HollowAwsConfig.builder().build()

        producer = HollowProducer.withPublisher(HollowS3Publisher.create(awsConfig))
            .withAnnouncer(HollowDynamoDBAnnouncer.create(awsConfig))
            .build()

        logger.info("MovieProducer initialized. Publishing to S3 bucket: ${awsConfig.bucket}, DynamoDB table: ${awsConfig.dynamoDbTable}")

        runProductionCycle()
    }

    @Scheduled(fixedDelay = 420000, initialDelay = 420000) // Every 7 minutes
    fun runProductionCycle() {
        try {
            val startTime = System.currentTimeMillis()
            cycleCount++

            logger.info("========================================")
            logger.info("Starting producer cycle #$cycleCount at ${LocalDateTime.now()}")

            currentMovies = if (currentMovies.isEmpty()) {
                logger.info("Generating initial dataset...")
                movieDataGenerator.generateInitialDataset()
            } else {
                logger.info("Generating updated dataset...")
                movieDataGenerator.generateUpdatedDataset(currentMovies)
            }

            logger.info("Dataset prepared: ${currentMovies.size} movies")

            producer.runCycle { writeState ->
                currentMovies.forEach { movie ->
                    writeState.add(movie)
                }
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("Producer cycle #$cycleCount completed in ${duration}ms")
            logger.info("Published ${currentMovies.size} movies to s3://${awsConfig.bucket}/${awsConfig.keyPrefix}")
            logger.info("========================================")

        } catch (e: Exception) {
            logger.error("Error during producer cycle", e)
        }
    }

    fun getCurrentStats(): Map<String, Any> {
        return mapOf(
            "cycleCount" to cycleCount,
            "movieCount" to currentMovies.size,
            "s3Bucket" to awsConfig.bucket,
            "dynamoDbTable" to awsConfig.dynamoDbTable,
            "datasetId" to awsConfig.datasetId
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
