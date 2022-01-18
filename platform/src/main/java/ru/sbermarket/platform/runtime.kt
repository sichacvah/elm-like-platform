package ru.sbermarket.platform



import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

interface Cancelable {
    fun cancel()
}

interface Runtime<Model, Msg> : Cancelable {
    fun dispatch(msg: Msg)
}

interface FlowRuntime<Model, Msg> : Runtime<Model, Msg> {
    val stateFlow: Flow<Model>
}


@OptIn(ExperimentalCoroutinesApi::class)
@JvmOverloads
fun <Model, Msg> flowRuntime(
    platform: Platform,
    init: Platform.() -> Pair<Model, Effect<Msg>>,
    update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
    runtimeContext: CoroutineContext = Dispatchers.Default,
    modelListenerContext: CoroutineContext = Dispatchers.Main,
    effectContext: CoroutineContext = Dispatchers.Default
): FlowRuntime<Model, Msg> {


    val sharedFlow = MutableStateFlow<Model?>(null)

    val runtime = RuntimeImpl(
        platform = platform,
        init = init,
        update = update,
        runtimeContext = runtimeContext,
        effectContext = effectContext,
        modelListenerContext = modelListenerContext,
        modelListener = {
            sharedFlow.value = it
        }
    )

    return object : FlowRuntime<Model, Msg> {
        override val stateFlow: Flow<Model>
            get() = sharedFlow.filter {
                it != null
            } as Flow<Model>

        override fun dispatch(msg: Msg) {
            runtime.dispatch(msg)
        }

        override fun cancel() {
            runtime.cancel()
        }
    }


}



@JvmOverloads
fun <Model, Msg> runtime(
    platform: Platform,
    init: Platform.() -> Pair<Model, Effect<Msg>>,
    update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
    modelListener: (Model) -> Unit,
    runtimeContext: CoroutineContext = Dispatchers.Default,
    modelListenerContext: CoroutineContext = Dispatchers.Main,
    effectContext: CoroutineContext = Dispatchers.Default
): Runtime<Model, Msg> {
    return RuntimeImpl(
        platform = platform,
        init = init,
        update = update,
        runtimeContext = runtimeContext,
        effectContext = effectContext,
        modelListenerContext = modelListenerContext,
        modelListener = modelListener
    )
}



private class RuntimeImpl<Model, Msg>(
    private val platform: Platform,
    init: Platform.() -> Pair<Model, Effect<Msg>>,
    private val update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
    private var modelListener: (Model) -> Unit,
    private val modelListenerContext: CoroutineContext,
    private val runtimeContext: CoroutineContext,
    private val effectContext: CoroutineContext
) : CoroutineScope, Runtime<Model, Msg> {

    private var state: Model

    val job: Job = SupervisorJob()

    fun clear() {
        modelListener = {}
    }

    override val coroutineContext: CoroutineContext
        get() = runtimeContext + job


    init {
        val initNext = platform.init()
        state = initNext.first
        step(initNext)
    }

    override fun dispatch(msg: Msg) {
        if (isActive) {
            launch(runtimeContext) {
                step(platform.update(msg, state))
            }
        }
    }

    private fun step(next: Pair<Model, Effect<Msg>>) {
        val (nextState, effect) = next
        state = nextState
        launch(modelListenerContext) { modelListener(state) }
        launch(effectContext) { effect.invoke(this, ::dispatch) }
    }

    override fun cancel() {
        job.cancel()
    }
}