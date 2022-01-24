package ru.sbermarket.sbermarketlite.application.features.select_store

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.next

object SelectStore {

    sealed interface Msg {}

    sealed interface Model {
        object None : Model
    }

    fun init(): Pair<Model, Effect<Msg>> {
        return next(Model.None)
    }

    fun update(msg: Msg, model: Model): Pair<Model, Effect<Msg>> {
        return next(model)
    }

    @Composable
    fun View(model: Model, dispatch: Dispatch<Msg>) {
        Text(text = "SELECT_STORE")
    }
}