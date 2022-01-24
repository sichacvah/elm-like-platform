package ru.sbermarket.sbermarketlite.application.features.select_address

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapviewlite.MapStyle
import com.here.sdk.mapviewlite.MapViewLite
import kotlinx.coroutines.*
import ru.sbermarket.platform.*
import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json
import ru.sbermarket.platform.modules.json.Json.Decode.field
import ru.sbermarket.platform.modules.json.Json.Decode.float
import ru.sbermarket.platform.modules.json.Json.Decode.nullable
import ru.sbermarket.platform.modules.json.Json.Decode.string
import ru.sbermarket.platform.modules.Http
import ru.sbermarket.platform.modules.Http.HttpTaskParams
import ru.sbermarket.platform.modules.json.Json.Decode.boolean
import ru.sbermarket.platform.modules.Http.Method
import ru.sbermarket.platform.modules.message
import ru.sbermarket.sbermarketlite.application.shared.*

sealed interface AddressSelected
object Not : AddressSelected
data class ValidAddress(
    override val lat: Float,
    override val lon: Float,
    val city : String,
    val street: String,
    val building: String
): AddressSelected, Point

fun Address.toSelected(): AddressSelected {
    if (city == null || street == null || building == null) return Not
    return ValidAddress(
        lat = lat,
        lon = lon,
        city = city,
        street = street,
        building = building
    )
}

internal fun Point.toGeoCoords(angle: Double = 0.0): GeoCoordinates =
    GeoCoordinates(lat.toDouble(), lon.toDouble(), angle)


fun <T> debounce(
    waitMs: Long = 300L,
    scope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}


@Composable
fun AddressMapView(modifier: Modifier = Modifier, point: Point, onMove: (Point) -> Unit) {
    val scope = rememberCoroutineScope()
    val handleChange = remember {
        debounce(1000L, scope, onMove)
    }
    AndroidView(
        update = {
        },
        factory = { context ->
            MapViewLite(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                onCreate(null)

                camera.addObserver {
                    handleChange(Point.make(it.target.latitude.toFloat(), it.target.longitude.toFloat()))
                }
                mapScene.loadScene(MapStyle.NORMAL_DAY) { errorCode ->
                    if (errorCode == null) {
                        camera.target = point.toGeoCoords()

                        camera.zoomLevel = 14.0
                    } else {
                        Log.d("MapView", "onLoadScene failed: $errorCode")
                    }
                }
            }
        },
        modifier = modifier
    )
}



fun Point.toAddress(): Address {
    return Address(lat = lat, lon = lon)
}

fun Address.valid(): Boolean {
    return city != null && building != null && street != null
}

fun ValidAddress.toQueryParams(): String {
    return "lat=$lat&lon=$lon&city=$city&building=$building&street=$street"
}

data class Address(
    override val lat: Float,
    override val lon: Float,
    val city: String? = null,
    val building: String? = null,
    val street: String? = null
): Point {
    companion object {
        fun decoder(): Decoder<Address> {
            return Json.Decode.convert(
                field("lat", float()),
                field("lon", float()),
                field("city", nullable(string())),
                field("building", nullable(string())),
                field("street", nullable(string())),
                ::Address
            )
        }
    }
}

fun Address.concatAddress(): String {
    val addressParts = listOfNotNull(city, street, building)
    return addressParts.joinToString(", ")
}

fun GeocodingAddress.toAddress(): Address {
    return Address(
        lat = point.lat,
        lon = point.lon,
        city = city,
        building = building,
        street = street
    )
}

@Composable
fun AddressWithButtonView(
    modifier: Modifier = Modifier,
    address: String? = null,
    deliveryAvailability: SelectAddress.AddressState.DeliveryAvailability,
    onPress: () -> Unit,
) {
    val finalAddress = address ?: "неопределен"

    Column(modifier = modifier) {
        Text(text = finalAddress, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        when (deliveryAvailability) {
            is SelectAddress.AddressState.DeliveryAvailability.Available -> {
                Button(onClick = onPress, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Привезти сюда", textAlign = TextAlign.Center)
                }
            }
            is SelectAddress.AddressState.DeliveryAvailability.NotAvailable -> {
                Text(text="По этому адресу нет доставки", modifier = Modifier.fillMaxWidth())
            }
            is SelectAddress.AddressState.DeliveryAvailability.Error -> {
                Text(text=deliveryAvailability.httpError.message())
            }
        }
    }
}

@Composable
fun FoundView(model: SelectAddress.AddressState.Found, dispatch: Dispatch<SelectAddress.Msg>) {
    AddressWithButtonView(
        modifier = Modifier.fillMaxWidth(),
        address = model.address.concatAddress(),
        deliveryAvailability = model.deliveryAvailability
    ) {
        dispatch(SelectAddress.Msg.BringHere)
    }
}

@Composable
fun ValidatingAddressView(
    model: SelectAddress.AddressState.ValidatingAddress,
    dispatch: Dispatch<SelectAddress.Msg>
) {
    AddressWithButtonView(
        modifier = Modifier.fillMaxWidth(),
        deliveryAvailability = SelectAddress.AddressState.DeliveryAvailability.Available,
        address = model.address.concatAddress()
    ) {
        dispatch(SelectAddress.Msg.BringHere)
    }

}

@Composable
fun NothingView(
    model: SelectAddress.AddressState.Nothing,
    dispatch: Dispatch<SelectAddress.Msg>
) {
    AddressWithButtonView(
        modifier = Modifier.fillMaxWidth(),
        deliveryAvailability = SelectAddress.AddressState.DeliveryAvailability.Available,
    ) {
        dispatch(SelectAddress.Msg.BringHere)
    }
}

@Composable
fun GeocoderProcessingView(
    model: SelectAddress.AddressState.GeocoderProcessing,
    dispatch: Dispatch<SelectAddress.Msg>
) {
    AddressWithButtonView(
        address = "Загрузка...",
        modifier = Modifier.fillMaxWidth(),
        deliveryAvailability = SelectAddress.AddressState.DeliveryAvailability.Available,
    ) {
        dispatch(SelectAddress.Msg.BringHere)
    }
}

object SelectAddress {

    @Composable
    fun View(model: AddressState, dispatch: Dispatch<Msg>) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AddressMapView(point = model.point(), modifier = Modifier.fillMaxSize()) {
                dispatch(Msg.Search(it))
            }

            Pin(modifier = Modifier.align(Alignment.Center))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                when (model) {
                    is AddressState.Found -> FoundView(model, dispatch)
                    is AddressState.ValidatingAddress -> ValidatingAddressView(model, dispatch)
                    is AddressState.Nothing -> NothingView(model = model, dispatch = dispatch)
                    is AddressState.GeocoderProcessing -> GeocoderProcessingView(model = model, dispatch = dispatch)
                }
            }

        }

    }


    sealed class Msg {
        data class GeolocationResponse(val result: Address?) : Msg()
        data class DeliveryAvailabilityResponse(val result: Result<Http.Error, Boolean>) : Msg()
        data class Search(val point: Point) : Msg()
        object BringHere : Msg()
    }

    sealed class AddressState {
        data class Nothing(val point: Point): AddressState()
        data class ValidatingAddress(val address: Address): AddressState()
        data class GeocoderProcessing(val prevAddress: Address, val point: Point): AddressState()
        data class Found(
            val address: Address,
            val deliveryAvailability: DeliveryAvailability
        ) : AddressState()

        sealed class DeliveryAvailability {
            object Available : DeliveryAvailability()
            object NotAvailable : DeliveryAvailability()
            data class Error(val httpError: Http.Error) : DeliveryAvailability()
        }
    }



    private fun AddressState.address(): Address {
        return when (this) {
            is AddressState.Nothing -> point.toAddress()
            is AddressState.ValidatingAddress -> address
            is AddressState.GeocoderProcessing -> prevAddress
            is AddressState.Found -> address
        }
    }

    private fun AddressState.point(): Point {
        return when (this) {
            is AddressState.Nothing -> point
            is AddressState.ValidatingAddress -> address
            is AddressState.GeocoderProcessing -> point
            is AddressState.Found -> address
        }
    }

    private val defaultPoint = Point.make(55.751400f, 37.6189090f)

    private fun AddressState.search(point: Point): AddressState {
        return AddressState.GeocoderProcessing(this.address(), point)
    }

    private fun AddressState.handleGeocoder(platform: Platform, config: Config, address: Address?): Pair<AddressState, Effect<Msg>> {
        return address?.let {
            AddressState.ValidatingAddress(it) to platform.isDeliveryAvailableRequest(config, it, Msg::DeliveryAvailabilityResponse)
        } ?: AddressState.Nothing(point()) to none()
    }

    private fun AddressState.handleDeliveryAvailabilityResponse(result: Result<Http.Error, Boolean>): AddressState {
        return when (this) {
            is AddressState.ValidatingAddress -> {
                when (result) {
                    is Result.Error -> AddressState.Found(this.address, deliveryAvailability = AddressState.DeliveryAvailability.Error(result.error))
                    is Result.Success -> {
                        AddressState.Found(
                            this.address,
                            deliveryAvailability = if (result.result) AddressState.DeliveryAvailability.Available else AddressState.DeliveryAvailability.NotAvailable
                        )
                    }
                }
            }
            else -> this
        }
    }

    fun <A, B, C> Pair<A, B>.add(third: C): Triple<A, B, C> {
        return Triple(first, second, third)
    }

    fun update(
        platform: Platform,
        config: Config,
        msg: Msg,
        model: AddressState
    ): Triple<AddressState, Effect<Msg>, AddressSelected> {
        return when (msg) {
            is Msg.BringHere -> {
                val selected = model.address().toSelected()
                Log.e("SELECTED", selected.toString())
                Triple(model, platform.Nav.navigate("select-address"), model.address().toSelected())
            }
            is Msg.Search -> {
                if (msg.point == model.point()) {
                    Triple(model, none(), Not)
                } else {
                    val (next, effect) = model.search(msg.point) to config.geocoding.reverseGeocode<Msg>(platform, msg.point) {
                        val result = it.map { geocodingAddresses ->
                            geocodingAddresses.find { address ->
                                address.city != null && address.street != null
                            }?.toAddress()
                        }
                        Msg.GeolocationResponse(result.toNullable())
                    }
                    Triple(next, effect, Not)
                }
            }
            is Msg.GeolocationResponse -> model.handleGeocoder(platform, config, msg.result).add(Not)
            is Msg.DeliveryAvailabilityResponse -> Triple(model.handleDeliveryAvailabilityResponse(msg.result), none(), Not)
        }
    }

    fun init(
        platform: Platform,
        config: Config,
        initialAddress: Address? = null
    ): Pair<AddressState, Effect<Msg>> {
        return initialAddress?.let {
            AddressState.Nothing(it) to platform.isDeliveryAvailableRequest(config, it, Msg::DeliveryAvailabilityResponse)
        } ?: AddressState.Nothing(defaultPoint) to none()
    }

    fun <Msg> Platform.isDeliveryAvailableRequest(
        config: Config,
        point: Point,
        toMsg: (Result<Http.Error, Boolean>) -> Msg
    ): Effect<Msg> {
        return Http.request(
            HttpTaskParams(
                expect = Http.expectJson(
                    toMsg,
                    decoder = Json.Decode.at(listOf("delivery_availability", "available"), boolean())
                ),
                method = Method.GET,
                url = config.baseUrl.plus(
                    "/v2/delivery_availability?lat=${point.lat}&lon=${point.lon}"
                )
            )
        )
    }
}
