package ru.sbermarket.sbermarketlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import ru.sbermarket.sbermarketlite.application.shared.SharedState
import ru.sbermarket.sbermarketlite.application.shared.View
import ru.sbermarket.sbermarketlite.ui.theme.SbermarketLiteTheme


@Composable
fun Root() {
    MainApp.platform?.let { platform ->
        val sharedState = SharedState.provideFeature(platform)

        platform.Compose(
            init = { sharedState.init() },
            update = { msg, model -> sharedState.update(msg, model) }
        ) { model, dispatch ->
            SharedState.View(model, dispatch)
        }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SbermarketLiteTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root()
                }
            }
        }
    }
}

