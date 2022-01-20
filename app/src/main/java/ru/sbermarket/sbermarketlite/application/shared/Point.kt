package ru.sbermarket.sbermarketlite.application.shared

import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json

interface Point {
    val lat: Float
    val lon: Float

    companion object {
        fun make(lat : Float, lon : Float) = object : Point {
            override val lat = lat
            override val lon = lon

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Point) return false
                return other.lat == this.lat && other.lon == this.lon
            }

            override fun hashCode(): Int {
                return lat.hashCode() + lon.hashCode()
            }
        }
    }
}
