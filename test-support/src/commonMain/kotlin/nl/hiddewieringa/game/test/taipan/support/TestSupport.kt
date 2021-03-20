package nl.hiddewieringa.game.test.taipan.support

import kotlinx.coroutines.CoroutineScope

expect fun runTest(block: suspend CoroutineScope.() -> Unit)
