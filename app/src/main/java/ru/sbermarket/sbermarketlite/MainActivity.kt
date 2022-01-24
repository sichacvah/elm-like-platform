package ru.sbermarket.sbermarketlite

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import ru.sbermarket.sbermarketlite.application.App
import ru.sbermarket.sbermarketlite.ui.theme.SbermarketLiteTheme


@Composable
fun Root(setupBackPressedCallback: (() -> Unit) -> Unit) {
    MainApp.platform?.let { platform ->
        platform.Compose(
            init = { App.init(platform) },
            update = { msg, model -> App.update(platform, msg, model) },
            onDestinationChange = {
                Log.e("DEST", it.toString())
                App.Msg.DestinationChanged(it)
            },
            onBackMsg = { App.Msg.Back },
            setupBackPressedCallback = setupBackPressedCallback
        ) { model, dispatch ->
           App.View(model, dispatch)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private var onBackPressedH: () -> Unit = {}

    init {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedH()
            }
        })
    }

    private fun setupOnBackPressed(cb: () -> Unit) {
        onBackPressedH = cb
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SbermarketLiteTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(this::setupOnBackPressed)
                }
            }
        }
    }
}

