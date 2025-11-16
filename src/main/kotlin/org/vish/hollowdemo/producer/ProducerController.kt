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
    private val movieProducer: MovieProducer
) {

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        return movieProducer.getCurrentStats()
    }

    @PostMapping("/publish")
    fun forcePublish(): Map<String, Any> {
        return movieProducer.forcePublish()
    }
}
