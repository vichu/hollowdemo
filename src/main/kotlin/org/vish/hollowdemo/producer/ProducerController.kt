package org.vish.hollowdemo.producer

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/producer")
@Profile("producer")
class ProducerController(
    private val catalogProducer: CatalogProducer,
    private val userProducer: UserProducer
) {

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> = mapOf(
        "catalog" to catalogProducer.getCurrentStats(),
        "users" to userProducer.getCurrentStats()
    )

    @PostMapping("/catalog/publish")
    fun forceCatalogPublish(): Map<String, Any> = catalogProducer.forcePublish()

    @PostMapping("/users/publish")
    fun forceUserPublish(): Map<String, Any> = userProducer.forcePublish()
}
