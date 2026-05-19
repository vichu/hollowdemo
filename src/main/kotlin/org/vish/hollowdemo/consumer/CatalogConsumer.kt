package org.vish.hollowdemo.consumer

import com.netflix.hollow.api.consumer.HollowConsumer
import com.netflix.hollow.api.consumer.index.HashIndex
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex
import io.github.vichu.hollow.aws.config.HollowAwsConfig
import io.github.vichu.hollow.aws.retriever.HollowS3BlobRetriever
import io.github.vichu.hollow.aws.watcher.HollowDynamoDBAnnouncementWatcher
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.vish.hollowdemo.api.MovieAPI
import java.time.LocalDateTime

@Service
@Profile("consumer")
class CatalogConsumer {
    private val logger = LoggerFactory.getLogger(CatalogConsumer::class.java)
    private lateinit var consumer: HollowConsumer
    private lateinit var api: MovieAPI
    private lateinit var awsConfig: HollowAwsConfig

    private lateinit var movieIndex: UniqueKeyIndex<org.vish.hollowdemo.api.Movie, Long>
    private lateinit var genreIndex: HashIndex<org.vish.hollowdemo.api.Movie, GenreQuery>

    private var lastUpdateTime: LocalDateTime? = null
    private var updateCount = 0L

    @PostConstruct
    fun initialize() {
        logger.info("========================================")
        logger.info("Initializing CatalogConsumer...")

        awsConfig = HollowAwsConfig.builder().datasetId("catalog").build()
        logger.info("Watching catalog dataset — S3: ${awsConfig.bucket}, DynamoDB: ${awsConfig.dynamoDbTable}")

        val blobRetriever = HollowS3BlobRetriever.create(awsConfig)
        val announcementWatcher = HollowDynamoDBAnnouncementWatcher.create(awsConfig)

        consumer = HollowConsumer.withBlobRetriever(blobRetriever)
            .withAnnouncementWatcher(announcementWatcher)
            .withGeneratedAPIClass(MovieAPI::class.java)
            .withRefreshListener(object : HollowConsumer.RefreshListener {
                override fun refreshStarted(currentVersion: Long, requestedVersion: Long) {
                    logger.info("🔄 Catalog update started: v$currentVersion -> v$requestedVersion")
                }

                override fun refreshSuccessful(beforeVersion: Long, afterVersion: Long, requestedVersion: Long) {
                    updateCount++
                    lastUpdateTime = LocalDateTime.now()
                    logger.info("========================================")
                    logger.info("✅ Catalog updated: v$beforeVersion -> v$afterVersion (update #$updateCount)")
                    logger.info("========================================")
                }

                override fun refreshFailed(beforeVersion: Long, afterVersion: Long, requestedVersion: Long, failureCause: Throwable?) {
                    logger.error("❌ Catalog update failed: v$beforeVersion -> v$afterVersion", failureCause)
                }

                override fun snapshotUpdateOccurred(api: com.netflix.hollow.api.custom.HollowAPI?, stateEngine: com.netflix.hollow.core.read.engine.HollowReadStateEngine?, version: Long) {}
                override fun deltaUpdateOccurred(api: com.netflix.hollow.api.custom.HollowAPI?, stateEngine: com.netflix.hollow.core.read.engine.HollowReadStateEngine?, version: Long) {}
                override fun blobLoaded(transition: HollowConsumer.Blob?) {}
            })
            .build()

        consumer.triggerRefresh()

        api = consumer.getAPI(MovieAPI::class.java)
        movieIndex = org.vish.hollowdemo.api.Movie.uniqueIndex(consumer)
        genreIndex = HashIndex.from(consumer, org.vish.hollowdemo.api.Movie::class.java)
            .usingBean(GenreQuery::class.java)
        consumer.addRefreshListener(genreIndex)

        lastUpdateTime = LocalDateTime.now()

        logger.info("CatalogConsumer initialized — version: ${consumer.currentVersionId}")
        logger.info("O(1) movie-by-ID index ready")
        logger.info("Genre hash index ready")
        logger.info("========================================")
    }

    private fun convertToModel(hollowMovie: org.vish.hollowdemo.api.Movie): org.vish.hollowdemo.model.Movie =
        org.vish.hollowdemo.model.Movie(
            id = hollowMovie.id,
            title = hollowMovie.title?.value ?: "",
            genre = hollowMovie.genre?.value ?: "",
            rating = hollowMovie.rating,
            releaseYear = hollowMovie.releaseYear,
            duration = hollowMovie.duration,
            description = hollowMovie.description?.value ?: "",
            director = hollowMovie.director?.value ?: "",
            cast = hollowMovie.cast?.map { it.value ?: "" } ?: emptyList(),
            writers = hollowMovie.writers?.map { it.value ?: "" } ?: emptyList(),
            productionCompany = hollowMovie.productionCompany?.value ?: "",
            country = hollowMovie.country?.value ?: "",
            language = hollowMovie.language?.value ?: "",
            budget = hollowMovie.budget,
            boxOffice = hollowMovie.boxOffice,
            tags = hollowMovie.tags?.map { it.value ?: "" } ?: emptyList(),
            ageRating = hollowMovie.ageRating?.value ?: "",
            awards = hollowMovie.awards?.value ?: ""
        )

    fun getAllMovies(): List<org.vish.hollowdemo.model.Movie> =
        api.allMovie.map { convertToModel(it) }.toList()

    fun getMovieById(id: Long): org.vish.hollowdemo.model.Movie? =
        movieIndex.findMatch(id)?.let { convertToModel(it) }

    fun searchByTitle(titleFragment: String): List<org.vish.hollowdemo.model.Movie> =
        getAllMovies().filter { it.title.contains(titleFragment, ignoreCase = true) }

    fun getMoviesByGenre(genre: String): List<org.vish.hollowdemo.model.Movie> =
        genreIndex.findMatches(GenreQuery(genre)).map { convertToModel(it) }.toList()

    fun getStats(): Map<String, Any> {
        val movies = getAllMovies()
        return mapOf(
            "totalMovies" to movies.size,
            "currentVersion" to consumer.currentVersionId,
            "lastUpdateTime" to (lastUpdateTime?.toString() ?: "N/A"),
            "updateCount" to updateCount,
            "genreBreakdown" to movies.groupingBy { it.genre }.eachCount(),
            "averageRating" to String.format("%.2f", movies.map { it.rating }.average()),
            "s3Bucket" to awsConfig.bucket,
            "dynamoDbTable" to awsConfig.dynamoDbTable,
            "datasetId" to awsConfig.datasetId
        )
    }
}
