package org.vish.hollowdemo.producer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.vish.hollowdemo.model.User

@Component
class UserDataGenerator(
    @Value("\${hollow.users.user-count:350000}") val userCount: Int,
    // Keep IDs within the catalog range so consumer-side joins always resolve
    @Value("\${hollow.catalog.movie-count:50000}") val maxMovieId: Long
) {
    companion object {
        private const val SEED = 42L
    }

    private val plans = listOf("AD_PLAN", "STANDARD", "PREMIUM")

    fun generateUsers(count: Int = userCount): List<User> {
        val random = java.util.Random(SEED)
        val faker = net.datafaker.Faker(java.util.Locale.US, random)

        return (1..count).map { i ->
            val watchedCount = 5 + random.nextInt(11)  // 5–15 movies per user
            User(
                userId = java.util.UUID.nameUUIDFromBytes("user-$i".toByteArray()).toString(),
                firstName = faker.name().firstName(),
                lastName = faker.name().lastName(),
                userSince = generateDate(random),
                planName = plans[random.nextInt(plans.size)],
                recentlyWatchedMovieIds = List(watchedCount) {
                    1L + (random.nextLong() % maxMovieId).let { v -> if (v < 0) -v else v }
                }
            )
        }
    }

    private fun generateDate(random: java.util.Random): String {
        val year = 2015 + random.nextInt(10)
        val month = 1 + random.nextInt(12)
        val day = 1 + random.nextInt(28)
        return "%04d-%02d-%02d".format(year, month, day)
    }
}
