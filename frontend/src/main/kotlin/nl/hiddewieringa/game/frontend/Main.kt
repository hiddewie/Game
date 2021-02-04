package nl.hiddewieringa.game.frontend

import kotlinx.browser.document
import react.child
import react.dom.a
import react.dom.div
import react.dom.nav
import react.dom.render

fun main() {
    render(document.getElementById("root")) {

        nav("uk-navbar-container uk-navbar uk-margin") {
            div("uk-navbar-left") {
                a("#", null, "uk-navbar-item uk-logo") {
                    +"Games"
                }
            }
        }

        div("uk-container") {
            child(AppComponent)
        }
    }
}