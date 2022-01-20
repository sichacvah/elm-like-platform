package ru.sbermarket.sbermarketlite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.Platform
import ru.sbermarket.platform.PlatformWithInit
import ru.sbermarket.platform.modules.DestinationService
import ru.sbermarket.platform.modules.DestinationToMsg


@Composable
fun <Model, Msg> PlatformWithInit.Compose(
    init: Platform.() -> Pair<Model, Effect<Msg>>,
    update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
    onDestinationChange: DestinationToMsg<Msg>,
    view: @Composable (model: Model, dispatch: Dispatch<Msg>) -> Unit
) {
    val runtime = remember {
        this.init(
            init = init,
            update = update,
            onDestinationChange = onDestinationChange
        )
    }

    val state = runtime.stateFlow.collectAsState(null)

    state.value?.let { model ->
        view(model, runtime::dispatch)
    }

    DisposableEffect(Unit) {
        onDispose {
            runtime.cancel()
        }
    }
}