package nl.hiddewieringa.game.test.taipan.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runTest(block: suspend CoroutineScope.() -> Unit): dynamic = GlobalScope.promise { block() }
