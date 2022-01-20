package ru.sbermarket.sbermarketlite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.mapTo

@Composable
fun <M1, M2> Dispatch<M1>.rememberMapTo(f: (M2) -> M1): Dispatch<M2> {
    return remember {
        mapTo(f)
    }
}