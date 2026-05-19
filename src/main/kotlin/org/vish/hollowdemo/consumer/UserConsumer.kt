package org.vish.hollowdemo.consumer

import com.netflix.hollow.api.consumer.HollowConsumer
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex
import io.github.vichu.hollow.aws.config.HollowAwsConfig
import io.github.vichu.hollow.aws.retriever.HollowS3BlobRetriever
import io.github.vichu.hollow.aws.watcher.HollowDynamoDBAnnouncementWatcher
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.vish.hollowdemo.api.users.UserAPI
import java.time.LocalDateTime

/**
 * Reads the users Hollow dataset (datasetId="users", updated hourly).
 * The recently-watched join is performed against CatalogConsumer's in-memory index —
 * two separate Hollow snapshots joined without a database.
 */
@Service
@Profile("consumer")
class UserConsumer(
    private val catalogConsumer: CatalogConsumer
) {
    private val logger = LoggerFactory.getLogger(UserConsumer::class.java)
    private lateinit var consumer: HollowConsumer
    private lateinit var api: UserAPI
    private lateinit var awsConfig: HollowAwsConfig

    private lateinit var userIndex: UniqueKeyIndex<org.vish.hollowdemo.api.users.User, String>

    private var lastUpdateTime: LocalDateTime? = null
    private var updateCount = 0L

    @PostConstruct
    fun initialize() {
        logger.info("========================================")
        logger.info("Initializing UserConsumer...")

        awsConfig = HollowAwsConfig.builder().datasetId("users").build()
        logger.info("Watching users dataset — S3: ${awsConfig.bucket}, DynamoDB: ${awsConfig.dynamoDbTable}")

        val blobRetriever = HollowS3BlobRetriever.create(awsConfig)
        val announcementWatcher = HollowDynamoDBAnnouncementWatcher.create(awsConfig)

        consumer = HollowConsumer.withBlobRetriever(blobRetriever)
            .withAnnouncementWatcher(announcementWatcher)
            .withGeneratedAPIClass(UserAPI::class.java)
            .withRefreshListener(object : HollowConsumer.RefreshListener {
                override fun refreshStarted(currentVersion: Long, requestedVersion: Long) {
                    logger.info("🔄 User dataset update started: v$currentVersion -> v$requestedVersion")
                }

                override fun refreshSuccessful(beforeVersion: Long, afterVersion: Long, requestedVersion: Long) {
                    updateCount++
                    lastUpdateTime = LocalDateTime.now()
                    logger.info("========================================")
                    logger.info("✅ User dataset updated: v$beforeVersion -> v$afterVersion (update #$updateCount)")
                    logger.info("========================================")
                }

                override fun refreshFailed(beforeVersion: Long, afterVersion: Long, requestedVersion: Long, failureCause: Throwable?) {
                    logger.error("❌ User dataset update failed: v$beforeVersion -> v$afterVersion", failureCause)
                }

                override fun snapshotUpdateOccurred(api: com.netflix.hollow.api.custom.HollowAPI?, stateEngine: com.netflix.hollow.core.read.engine.HollowReadStateEngine?, version: Long) {}
                override fun deltaUpdateOccurred(api: com.netflix.hollow.api.custom.HollowAPI?, stateEngine: com.netflix.hollow.core.read.engine.HollowReadStateEngine?, version: Long) {}
                override fun blobLoaded(transition: HollowConsumer.Blob?) {}
            })
            .build()

        consumer.triggerRefresh()

        api = consumer.getAPI(UserAPI::class.java)
        userIndex = org.vish.hollowdemo.api.users.User.uniqueIndex(consumer)

        lastUpdateTime = LocalDateTime.now()

        logger.info("UserConsumer initialized — version: ${consumer.currentVersionId}")
        logger.info("O(1) user-by-ID index ready (${consumer.currentVersionId} version)")
        logger.info("========================================")
    }

    private fun convertToModel(hollowUser: org.vish.hollowdemo.api.users.User): org.vish.hollowdemo.model.User =
        org.vish.hollowdemo.model.User(
            userId = hollowUser.userId?.value ?: "",
            firstName = hollowUser.firstName?.value ?: "",
            lastName = hollowUser.lastName?.value ?: "",
            userSince = hollowUser.userSince?.value ?: "",
            planName = hollowUser.planName?.value ?: "",
            recentlyWatchedMovieIds = hollowUser.recentlyWatchedMovieIds?.map { it.value } ?: emptyList()
        )

    fun getUserById(userId: String): org.vish.hollowdemo.model.User? =
        userIndex.findMatch(userId)?.let { convertToModel(it) }

    /**
     * In-memory cross-dataset join: resolve each movie ID from the users snapshot
     * against the catalog snapshot — no database, no network call.
     */
    fun getRecentlyWatchedMovies(userId: String): List<org.vish.hollowdemo.model.Movie>? {
        val user = getUserById(userId) ?: return null
        return user.recentlyWatchedMovieIds.mapNotNull { movieId ->
            catalogConsumer.getMovieById(movieId)
        }
    }

    fun getStats(): Map<String, Any> {
        val planBreakdown = api.allUser
            .groupingBy { it.planName?.value ?: "UNKNOWN" }
            .eachCount()
        return mapOf(
            "totalUsers" to api.allUser.toList().size,
            "planBreakdown" to planBreakdown,
            "currentVersion" to consumer.currentVersionId,
            "lastUpdateTime" to (lastUpdateTime?.toString() ?: "N/A"),
            "updateCount" to updateCount,
            "datasetId" to awsConfig.datasetId
        )
    }
}
