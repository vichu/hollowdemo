package org.vish.hollowdemo.consumer

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.vish.hollowdemo.model.Movie
import org.vish.hollowdemo.model.User

@RestController
@RequestMapping("/users")
@Profile("consumer")
class UserController(
    private val userConsumer: UserConsumer
) {

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: String): ResponseEntity<User> {
        val user = userConsumer.getUserById(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    /**
     * Cross-dataset join: user record lives in the "users" Hollow snapshot,
     * movie details live in the "catalog" Hollow snapshot.
     * Both are queried in-memory — no database involved.
     */
    @GetMapping("/{userId}/recently-watched")
    fun getRecentlyWatchedMovies(@PathVariable userId: String): ResponseEntity<List<Movie>> {
        val movies = userConsumer.getRecentlyWatchedMovies(userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(movies)
    }

    @GetMapping("/stats")
    fun getUserStats(): Map<String, Any> = userConsumer.getStats()
}
