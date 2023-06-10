package wangdaye.com.geometricweather.db.repositories

import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.options.provider.WeatherSource
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.db.ObjectBox.boxStore
import wangdaye.com.geometricweather.db.converters.WeatherSourceConverter
import wangdaye.com.geometricweather.db.entities.WeatherEntity
import wangdaye.com.geometricweather.db.entities.WeatherEntity_
import wangdaye.com.geometricweather.db.generators.AlertEntityGenerator
import wangdaye.com.geometricweather.db.generators.MinutelyEntityGenerator
import wangdaye.com.geometricweather.db.generators.generate
import wangdaye.com.geometricweather.db.generators.generateEntityList

object WeatherEntityRepository {
    // insert.
    fun writeWeather(location: Location, weather: Weather) {
        boxStore.callInTxNoException {
            deleteWeather(location)
            insertWeatherEntity(generate(location, weather))
            DailyEntityRepository.insertDailyList(
                generate(
                    location.cityId,
                    location.weatherSource,
                    weather.dailyForecast
                )
            )
            HourlyEntityRepository.insertHourlyList(
                generateEntityList(
                    location.cityId,
                    location.weatherSource,
                    weather.hourlyForecast
                )
            )
            MinutelyEntityRepository.insertMinutelyList(
                MinutelyEntityGenerator.generate(
                    location.cityId,
                    location.weatherSource,
                    weather.minutelyForecast
                )
            )
            AlertEntityRepository.insertAlertList(
                AlertEntityGenerator.generate(
                    location.cityId,
                    location.weatherSource,
                    weather.alertList
                )
            )
            if (weather.dailyForecast.isNotEmpty() && weather.dailyForecast[0].day?.temperature != null && weather.dailyForecast[0].night?.temperature != null) {
                HistoryEntityRepository.insertHistoryEntity(
                    generate(
                        location.cityId, location.weatherSource, weather
                    )
                )
            }
            if (weather.yesterday != null) {
                HistoryEntityRepository.insertHistoryEntity(
                    generate(
                        location.cityId, location.weatherSource, weather.yesterday!!
                    )
                )
            }
            true
        }
    }

    fun insertWeatherEntity(entity: WeatherEntity) {
        boxStore.boxFor(WeatherEntity::class.java).put(entity)
    }

    // delete.
    fun deleteWeather(location: Location) {
        boxStore.callInTxNoException {
            deleteWeather(
                selectWeatherEntityList(
                    location.cityId,
                    location.weatherSource
                )
            )
            HistoryEntityRepository.deleteLocationHistoryEntity(
                HistoryEntityRepository.selectHistoryEntityList(
                    location.cityId,
                    location.weatherSource
                )
            )
            DailyEntityRepository.deleteDailyEntityList(
                DailyEntityRepository.selectDailyEntityList(
                    location.cityId,
                    location.weatherSource
                )
            )
            HourlyEntityRepository.deleteHourlyEntityList(
                HourlyEntityRepository.selectHourlyEntityList(
                    location.cityId,
                    location.weatherSource
                )
            )
            MinutelyEntityRepository.deleteMinutelyEntityList(
                MinutelyEntityRepository.selectMinutelyEntityList(
                    location.cityId,
                    location.weatherSource
                )
            )
            AlertEntityRepository.deleteAlertList(
                AlertEntityRepository.selectLocationAlertEntity(
                    location.cityId,
                    location.weatherSource
                )
            )
            true
        }
    }

    fun deleteWeather(entityList: List<WeatherEntity>) {
        boxStore.boxFor(WeatherEntity::class.java).remove(entityList)
    }

    // select.
    fun readWeather(location: Location): Weather? {
        val weatherEntity = selectWeatherEntity(location.cityId, location.weatherSource) ?: return null
        val historyEntity = HistoryEntityRepository.selectYesterdayHistoryEntity(
            location.cityId, location.weatherSource, weatherEntity.publishDate, location.timeZone
        )
        return generate(weatherEntity, historyEntity, boxStore)
    }

    fun selectWeatherEntity(cityId: String, source: WeatherSource): WeatherEntity? {
        val entityList = selectWeatherEntityList(cityId, source)
        return if (entityList.isEmpty()) null else entityList[0]
    }

    fun selectWeatherEntityList(cityId: String, source: WeatherSource): List<WeatherEntity> {
        val query = boxStore.boxFor(WeatherEntity::class.java)
            .query(
                WeatherEntity_.cityId.equal(cityId)
                    .and(
                        WeatherEntity_.weatherSource.equal(
                            WeatherSourceConverter().convertToDatabaseValue(source)
                        )
                    )
            ).build()
        val results = query.find()
        query.close()
        return results
    }
}