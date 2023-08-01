package org.breezyweather.sources.atmoaura

import org.breezyweather.common.basic.models.weather.AirQuality
import org.breezyweather.common.basic.wrappers.AirQualityWrapper
import org.breezyweather.common.basic.wrappers.SecondaryWeatherWrapper
import org.breezyweather.common.exceptions.SecondaryWeatherException
import org.breezyweather.sources.atmoaura.json.AtmoAuraPointResult
import java.util.Date

fun convert(result: AtmoAuraPointResult): SecondaryWeatherWrapper {
    if (result.polluants == null) {
        throw SecondaryWeatherException()
    }

    val airQualityHourly = mutableMapOf<Date, AirQuality>()
    result.polluants.getOrNull(0)?.horaires?.forEach {
        airQualityHourly[it.datetimeEcheance] = getAirQuality(it.datetimeEcheance, result)
    }

    return SecondaryWeatherWrapper(
        airQuality = AirQualityWrapper(
            hourlyForecast = airQualityHourly
        )
    )
}

private fun getAirQuality(requestedDate: Date, aqiAtmoAuraResult: AtmoAuraPointResult): AirQuality {
    var pm25: Float? = null
    var pm10: Float? = null
    var so2: Float? = null
    var no2: Float? = null
    var o3: Float? = null

    aqiAtmoAuraResult.polluants
        ?.filter { p -> p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate } != null }
        ?.forEach { p -> when (p.polluant) {
                "o3" -> o3 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "no2" -> no2 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "pm2.5" -> pm25 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "pm10" -> pm10 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "so2" -> so2 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
            }
        }

    return AirQuality(
        pM25 = pm25,
        pM10 = pm10,
        sO2 = so2,
        nO2 = no2,
        o3 = o3
    )
}