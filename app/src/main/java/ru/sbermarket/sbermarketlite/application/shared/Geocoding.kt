package ru.sbermarket.sbermarketlite.application.shared

import ru.sbermarket.platform.*
import ru.sbermarket.platform.modules.Http
import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json.Decode.at
import ru.sbermarket.platform.modules.json.Json.Decode.convert
import ru.sbermarket.platform.modules.json.Json.Decode.field
import ru.sbermarket.platform.modules.json.Json.Decode.float
import ru.sbermarket.platform.modules.json.Json.Decode.index
import ru.sbermarket.platform.modules.json.Json.Decode.list
import ru.sbermarket.platform.modules.json.Json.Decode.maybe
import ru.sbermarket.platform.modules.json.Json.Decode.oneOf
import ru.sbermarket.platform.modules.json.Json.Decode.string
import ru.sbermarket.platform.modules.json.Json.Decode.success

data class Session(
    private val token: String?
) {
    fun <Msg> request(platform: Platform, params: Http.HttpTaskParams<Msg>): Effect<Msg> {
        return platform.Http.request(
            params = params.copy(
                headers = params.headers
            )
        )
    }
}



data class Geocoding(
    internal val apiKey: String = "QhIRUoC1dYf1prIgKmJgH5v6rxOGssFSPnCikBR8lJ4",
    internal val apiToken: String = "z4VZXi2mabFQNoXOh4P6tNMzyOTEilVKBAcgo67Qj8dMesEG8mYX_TK8mD583HsRptGA7oSAMVJ4ZrS7cx5LSA",
    internal val url: String = "https://discover.search.hereapi.com/v1"
)

data class  GeocodingAddress(
    val label: String? = null,
    val street: String? = null,
    val building: String? = null,
    val city: String? = null,
    val point: Point
) {
    companion object {
        fun decoder(): Decoder<List<GeocodingAddress>> {
            return field(
                "items",
                list(
                    convert(
                        maybe(at("address", "label", decoder = string())),
                        maybe(at("address", "street", decoder = string())),
                        maybe(at("address", "houseNumber", decoder = string())),
                        maybe(at("address", "city", decoder = string())),
                        at("position", "lat", decoder = float()),
                        at("position", "lng", decoder = float())
                    ) { label, street, building, city, lat, lon ->
                        GeocodingAddress(
                            label,
                            street,
                            building,
                            city,
                            point = Point.make(lat, lon)
                        )
                    }
                )
            )
        }
    }
}



fun pointDecoder(): Decoder<Point?> {
    return oneOf(
        index(
            0,
            convert(
                at("position", "lat", decoder = float()),
                at("position", "lng", decoder = float()),
                Point::make
            )
        ),
        success(null)
    )
}

fun <Msg> Geocoding.geocode(
    platform: Platform,
    point: Point? = null,
    address: String,
    toMsg: (Result<Http.Error, Point?>) -> Msg
): Effect<Msg> {
    val at = point?.let { p -> "&at=${p.lat},${p.lon}" } ?: ""
    val finalUrl = url.plus("/geocode?q=$address$at")
    return platform.Http.request(Http.HttpTaskParams(
        method = Http.Method.GET,
        url = finalUrl,
        expect = platform.Http.expectJson(
            decoder = pointDecoder(),
            toMsg = toMsg
        )
    ))
}



fun <Msg> Geocoding.reverseGeocode(
    platform: Platform,
    point: Point,
    limit: Int = 24,
    radius: Int = 200,
    toMsg: (Result<Http.Error, List<GeocodingAddress>>) -> Msg,
): Effect<Msg> {
    val finalUrl = url.plus("/revgeocode?in=circle:${point.lat.toBigDecimal()},${point.lon.toBigDecimal()};r=$radius&lang=ru&apiKey=${apiKey}&show=streetInfo&limit=$limit")
    return platform.Http.request(
        Http.HttpTaskParams(
            method = Http.Method.GET,
            url = finalUrl,
            expect = platform.Http.expectJson(
                toMsg = toMsg,
                decoder = GeocodingAddress.decoder()
            )
        )
    )
}