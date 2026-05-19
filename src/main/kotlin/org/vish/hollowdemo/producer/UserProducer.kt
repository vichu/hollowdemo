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
import org.vish.hollowdemo.model.User
import java.time.LocalDateTime

@Service
@Profile("producer")
class UserProducer(
    private val userDataGenerator: UserDataGenerator
) {
    private val logger = LoggerFactory.getLogger(UserProducer::class.java)
    private lateinit var producer: HollowProducer
    private lateinit var awsConfig: HollowAwsConfig
    private var users: List<User> = emptyList()
    private var cycleCount = 0L

    @PostConstruct
    fun initialize() {
        awsConfig = HollowAwsConfig.builder().datasetId("users").build()

        producer = HollowProducer.withPublisher(HollowS3Publisher.create(awsConfig))
            .withAnnouncer(HollowDynamoDBAnnouncer.create(awsConfig))
            .build()

        logger.info("UserProducer initialized. Publishing to S3 bucket: ${awsConfig.bucket}, dataset: ${awsConfig.datasetId}")

        logger.info("Generating ${userDataGenerator.userCount} users (deterministic, seed=42)...")
        val genStart = System.currentTimeMillis()
        users = userDataGenerator.generateUsers()
        logger.info("User generation complete: ${users.size} users in ${System.currentTimeMillis() - genStart}ms")

        runPublishCycle()
    }

    // User profiles change far less often than the catalog — 10-minute refresh for demo
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    fun runPublishCycle() {
        try {
            val startTime = System.currentTimeMillis()
            cycleCount++

            logger.info("========================================")
            logger.info("Starting user publish cycle #$cycleCount at ${LocalDateTime.now()}")

            producer.runCycle { writeState ->
                users.forEach { user -> writeState.add(user) }
            }

            val duration = System.currentTimeMillis() - startTime
            logger.info("User publish cycle #$cycleCount completed in ${duration}ms")
            logger.info("Published ${users.size} users to s3://${awsConfig.bucket}/${awsConfig.keyPrefix}")
            logger.info("========================================")

        } catch (e: Exception) {
            logger.error("Error during user publish cycle", e)
        }
    }

    fun getCurrentStats(): Map<String, Any> = mapOf(
        "cycleCount" to cycleCount,
        "userCount" to users.size,
        "s3Bucket" to awsConfig.bucket,
        "dynamoDbTable" to awsConfig.dynamoDbTable,
        "datasetId" to awsConfig.datasetId
    )

    fun forcePublish(): Map<String, Any> {
        logger.info("User force-publish triggered manually")
        runPublishCycle()
        return mapOf(
            "status" to "success",
            "message" to "User publish cycle triggered",
            "cycleCount" to cycleCount,
            "userCount" to users.size
        )
    }
}
