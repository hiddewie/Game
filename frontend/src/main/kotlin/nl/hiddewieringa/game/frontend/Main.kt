package nl.hiddewieringa.game.frontend

import kotlinx.browser.document
import react.child
import react.dom.div
import react.dom.render

fun main() {
    render(document.getElementById("root")) {
        div("uk-container uk-container-expand uk-padding") {
            child(AppComponent)
        }
    }
}