package com.github.checketts.micrometerspringone2019

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.core.instrument.logging.LoggingRegistryConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Duration

private val Int.minutes get() = Duration.ofMinutes(this.toLong())
data class Chore(val name: String, val duration: Duration, val group: String = "home")

val chores = listOf(
        Chore("Mow front lawn", 20.minutes, "yard"),
        Chore("Mow back lawn", 10.minutes, "yard"),
        Chore("Gather the laundry", 7.minutes, "laundry"),
        Chore("Wash the laundry", 3.minutes, "laundry"),
        Chore("Sort/Fold the laundry", 50.minutes, "laundry"),
        Chore("Was the dishes", 10.minutes, "kitchen"),
        Chore("Find my phone charger", Duration.ofNanos(5))
)

fun main() {
    val meterRegistry = Metrics.globalRegistry
    val simple = SimpleMeterRegistry().apply { meterRegistry.add(this) }
    val config = object: LoggingRegistryConfig {
        override fun get(key: String)=null
        override fun logInactive()= true
        override fun step()= Duration.ofSeconds(5)
    }
    val loggingRegistry = LoggingMeterRegistry(config, Clock.SYSTEM).apply {
        meterRegistry.add(this) }

//    meterRegistry.config().meterFilter(MeterFilter.deny({it.name == "chore.completed"}))
//    meterRegistry.config().meterFilter(MeterFilter.maximumAllowableMetrics(2))
    meterRegistry.config().meterFilter(object: MeterFilter {
        override fun map(id: Meter.Id): Meter.Id {
            if(id.name == "chore.duration") {
                return id.replaceTags(id.tags.map { if(it.key == "group" && it.value == "laundry") it else Tag.of(it.key,"other") })
            } else {
                return id
            }
        }
    })
    meterRegistry.config().commonTags("team", "spring")


    addGauge(meterRegistry)
    for (chore in chores) {
        println("Doing ${chore.name}")
        meterRegistry.counter("chore.completed").increment()
        meterRegistry.timer("chore.duration", Tags.of("group", chore.group)).record(chore.duration)
    }

    for (meter in simple.meters) {
        println("${meter.id} ${meter.measure()}")
    }

    System.gc()
    (1..100).forEach {
        Thread.sleep(1000)
        println("Waiting $it")
    }

}

fun addGauge(meterRegistry: MeterRegistry) {
    val choresList = chores.map { it }
    meterRegistry.gauge("chore.size.weak", choresList, { it.size.toDouble()})
    meterRegistry.gauge("chore.size.lambda", "", { choresList.size.toDouble()})
    Gauge.builder("chore.size.strong", choresList, {it.size.toDouble()}).strongReference(true).register(meterRegistry)
}
