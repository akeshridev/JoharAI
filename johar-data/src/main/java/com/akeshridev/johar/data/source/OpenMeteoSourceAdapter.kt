package com.akeshridev.johar.data.source

import com.akeshridev.johar.data.remote.HttpTextFetcher
import com.akeshridev.johar.domain.crawl.CrawlSeed
import com.akeshridev.johar.domain.entity.EntityType
import com.akeshridev.johar.domain.source.FactValue
import com.akeshridev.johar.domain.source.Freshness
import com.akeshridev.johar.domain.source.KnowledgeDomain
import com.akeshridev.johar.domain.source.SourceFact
import org.json.JSONObject

internal class OpenMeteoSourceAdapter(
    private val fetcher: HttpTextFetcher,
) : CrawlSourceAdapter {
    override val id: String = "open_meteo"

    override fun supports(seed: CrawlSeed): Boolean =
        seed.latitude != null && seed.longitude != null && seed.entityType in SUPPORTED_TYPES

    override fun crawl(seed: CrawlSeed): SourceResult? {
        val ownerId = requireNotNull(seed.entityId)
        val latitude = seed.latitude ?: return null
        val longitude = seed.longitude ?: return null
        val sourceUrl = "$API?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,precipitation,rain,weather_code,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code,sunrise,sunset" +
            "&timezone=auto&forecast_days=7"
        val raw = fetcher.get(sourceUrl)
        val root = JSONObject(raw)
        val current = root.optJSONObject("current")
        val currentUnits = root.optJSONObject("current_units")
        val daily = root.optJSONObject("daily")
        val dailyUnits = root.optJSONObject("daily_units")
        val retrievedAt = System.currentTimeMillis()
        val facts = mutableListOf<SourceFact>()

        current?.let { values ->
            addNumberFact(facts, ownerId, sourceUrl, retrievedAt, values, currentUnits, "temperature_2m")
            addNumberFact(facts, ownerId, sourceUrl, retrievedAt, values, currentUnits, "precipitation")
            addNumberFact(facts, ownerId, sourceUrl, retrievedAt, values, currentUnits, "rain")
            addNumberFact(facts, ownerId, sourceUrl, retrievedAt, values, currentUnits, "weather_code")
            addNumberFact(facts, ownerId, sourceUrl, retrievedAt, values, currentUnits, "wind_speed_10m")
            values.optString("time")
                .takeIf(String::isNotBlank)
                ?.let { time ->
                    facts += fact(
                        ownerId,
                        sourceUrl,
                        retrievedAt,
                        "weather.current.time",
                        FactValue.Text(time),
                        values.toString(),
                    )
                }
        }

        if (daily != null) {
            val time = daily.optJSONArray("time")
            for (index in 0 until minOf(time?.length() ?: 0, FORECAST_DAYS)) {
                val date = time?.optString(index).orEmpty()
                if (date.isBlank()) continue
                addDailyNumber(facts, ownerId, sourceUrl, retrievedAt, daily, dailyUnits, index, date, "temperature_2m_max")
                addDailyNumber(facts, ownerId, sourceUrl, retrievedAt, daily, dailyUnits, index, date, "temperature_2m_min")
                addDailyNumber(facts, ownerId, sourceUrl, retrievedAt, daily, dailyUnits, index, date, "precipitation_probability_max")
                addDailyNumber(facts, ownerId, sourceUrl, retrievedAt, daily, dailyUnits, index, date, "weather_code")
                listOf("sunrise", "sunset").forEach { key ->
                    daily.optJSONArray(key)?.optString(index)
                        ?.takeIf(String::isNotBlank)
                        ?.let { value ->
                            facts += fact(
                                ownerId,
                                sourceUrl,
                                retrievedAt,
                                "weather.$date.$key",
                                FactValue.Text(value),
                                "Open-Meteo daily forecast for $date: $key=$value",
                            )
                        }
                }
            }
        }

        return SourceResult(
            sourceUrl = sourceUrl,
            publisher = PUBLISHER,
            rawContent = raw,
            facts = facts,
        )
    }

    private fun addNumberFact(
        facts: MutableList<SourceFact>,
        ownerId: String,
        sourceUrl: String,
        retrievedAt: Long,
        values: JSONObject,
        units: JSONObject?,
        key: String,
    ) {
        if (!values.has(key) || values.isNull(key)) return
        val number = values.optDouble(key, Double.NaN)
        if (number.isNaN()) return
        facts += fact(
            ownerId,
            sourceUrl,
            retrievedAt,
            "weather.current.$key",
            FactValue.Number(number, units?.optString(key)?.takeIf(String::isNotBlank)),
            "Open-Meteo current: $key=$number",
        )
    }

    private fun addDailyNumber(
        facts: MutableList<SourceFact>,
        ownerId: String,
        sourceUrl: String,
        retrievedAt: Long,
        daily: JSONObject,
        units: JSONObject?,
        index: Int,
        date: String,
        key: String,
    ) {
        val array = daily.optJSONArray(key) ?: return
        if (array.isNull(index)) return
        val number = array.optDouble(index, Double.NaN)
        if (number.isNaN()) return
        facts += fact(
            ownerId,
            sourceUrl,
            retrievedAt,
            "weather.$date.$key",
            FactValue.Number(number, units?.optString(key)?.takeIf(String::isNotBlank)),
            "Open-Meteo daily forecast for $date: $key=$number",
        )
    }

    private fun fact(
        ownerId: String,
        sourceUrl: String,
        retrievedAt: Long,
        field: String,
        value: FactValue,
        evidence: String,
    ): SourceFact = SourceFact(
        entityId = ownerId,
        sourceUrl = sourceUrl,
        publisher = PUBLISHER,
        retrievedAtEpochMillis = retrievedAt,
        domain = KnowledgeDomain.WEATHER_SEASON_CONTEXT,
        field = field,
        value = value,
        evidenceText = evidence,
        freshness = Freshness.LIVE,
    )

    companion object {
        private const val PUBLISHER = "Open-Meteo"
        private const val API = "https://api.open-meteo.com/v1/forecast"
        private const val FORECAST_DAYS = 7

        private val SUPPORTED_TYPES = setOf(
            EntityType.PLACE,
            EntityType.TOURIST_ATTRACTION,
            EntityType.NATURAL_FEATURE,
            EntityType.VILLAGE,
            EntityType.TOWN,
            EntityType.CITY,
        )
    }
}
