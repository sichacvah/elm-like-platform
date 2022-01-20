package ru.sbermarket.sbermarketlite

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import ru.sbermarket.sbermarketlite.application.App
import ru.sbermarket.sbermarketlite.ui.theme.SbermarketLiteTheme


@Composable
fun Root() {
    MainApp.platform?.let { platform ->
        platform.Compose(
            init = { App.init(platform) },
            update = { msg, model -> App.update(platform, msg, model) },
            onDestinationChange = {
                Log.e("DEST", it.toString())
                App.Msg.DestinationChanged(it)
            }


        ) { model, dispatch ->
           App.View(model, dispatch)
        }
    }
}

class MainActivity : AppCompatActivity() {

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

