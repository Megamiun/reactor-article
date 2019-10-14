package br.com.gabryel.reactorarticle

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.random.Random

fun main() {
    wrapped { `creating and executing a Mono Stream`() }
    wrapped { `creating and executing a Flux Stream`() }
    wrapped { `creating and executing a cold Mono Stream that has logic inside`() }
    wrapped { `creating and executing a hot Mono Stream that has logic inside`() }
    wrapped { `executing a hot Mono Stream and the cold stream that provides for it`() }
    wrapped { `executing a Mono Stream on another Thread`() }
    wrapped { `executing a failed Mono Stream`() }
}

private fun `creating and executing a Mono Stream`() {

    println("Creating Mono publisher justOne")
    val justOne = Mono.fromSupplier {
        println("Generating the number one")
        1
    }

    println("Subscribing to justOne")
    justOne
        .doOnSuccess { event -> printEvent(event) }
        .subscribe()
}

private fun `creating and executing a Flux Stream`() {

    println("Creating Flux publisher multipleNumbers")
    val multipleNumbers = Flux.just(1, 2, 3)
    val test = Mono.just(0)

    println("Subscribing to multipleNumbers")
    multipleNumbers
        .doOnNext { event -> printEvent(event) }
        .doOnComplete { println("Event completed!") }
        .subscribe()
}

private fun `creating and executing a cold Mono Stream that has logic inside`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Subscribing to randomNumber one time")
    randomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to randomNumber a second time")
    randomNumber.subscribe { event -> printEvent(event) }
}

private fun `creating and executing a hot Mono Stream that has logic inside`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Caching a random generated number")
    val cachedRandomNumber = randomNumber.cache()

    println("Subscribing to cachedRandomNumber one time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to cachedRandomNumber a second time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }
}

private fun `executing a hot Mono Stream and the cold stream that provides for it`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Caching a random generated number")
    val cachedRandomNumber = randomNumber.cache()

    println("Subscribing to cachedRandomNumber one time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to randomNumber one time")
    randomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to cachedRandomNumber a second time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }
}

private fun `executing a Mono Stream on another Thread`() {

    println("Creating Mono publisher originalStream")
    val originalStream = Mono.fromSupplier {
        println("Supplying on ${Thread.currentThread().name}")
        1
    }

    println("Declaring Subscription Thread on ${getThreadName()}")
    val subscribedOn = originalStream.subscribeOn(Schedulers.elastic())

    println("Declaring onNext on ${getThreadName()}")
    val onNexted = subscribedOn.doOnNext { event -> printThreaded(event, "doOnNext") }

    println("Declaring Publishing Thread on ${getThreadName()}")
    val publishedOn = onNexted.publishOn(Schedulers.newParallel("parallel"))

    println("Subscribing on ${getThreadName()}")
    val disposable = publishedOn.subscribe { event -> printThreaded(event, "subscribe") }

    while (!disposable.isDisposed) { }
}

private fun `executing a failed Mono Stream`() {

    println("Creating Mono publisher randomNumber")
    val throwingStream = Mono.fromSupplier<Int> {
        throw IllegalAccessException("You shall not pass")
    }

    println("Subscribing to a not guarded throwingStream one time")

    try {
        throwingStream.subscribe { event -> printEvent(event) }
    } catch (e: IllegalAccessException) {
        println("Captured an IllegalAccessException: `${e.message}`")
    } catch (e: UnsupportedOperationException) {
        println("Captured an UnsupportedOperationException: `${e.message}`")
    }

    println("Subscribing to a guarded throwingStream one time")

    throwingStream
        .doOnError { error -> println("We successfully guarded our flow from `${error.message}`") }
        .onErrorReturn(1)
        .subscribe { event -> printEvent(event) }
}

private fun wrapped(block: () -> Unit) {
    println()
    block()
}

private fun printThreaded(event: Any, details: String) {
    println("Receiving $details event $event on thread ${getThreadName()}")
}

private fun getThreadName() = Thread.currentThread().name

private fun printEvent(event: Any) {
    println("Event content is $event")
}