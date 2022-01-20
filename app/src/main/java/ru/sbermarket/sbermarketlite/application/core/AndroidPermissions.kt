package ru.sbermarket.sbermarketlite.application.core

import android.content.Context
import androidx.activity.contextaware.ContextAware
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPermissions(val context: Context) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun requestPermission(permission: String): Boolean {
        val activity = context.findActivity() as AppCompatActivity

        return suspendCancellableCoroutine { continuation ->
            val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                continuation.resume(granted) {}
            }
            launcher.launch(permission)

            continuation.invokeOnCancellation {
                launcher.unregister()
            }
        }

    }

}