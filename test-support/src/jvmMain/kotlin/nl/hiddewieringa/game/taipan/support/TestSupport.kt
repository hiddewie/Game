package nl.hiddewieringa.game.taipan.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }