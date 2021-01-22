package nl.hiddewieringa.game.frontend

import react.dom.*
import kotlinx.browser.document
import react.child

fun main() {
    render(document.getElementById("root")) {
        child(AppComponent)
    }
}