package nl.hiddewieringa.game.frontend

import kotlinx.browser.document
import kotlinx.css.*
import kotlinx.html.classes
import react.child
import react.dom.render
import styled.css
import styled.styledDiv

fun main() {
    render(document.getElementById("root")) {
        styledDiv {
            attrs.classes = setOf("uk-container", "uk-container-expand", "uk-padding")
            css {
                height = 100.pct
                width = 100.pct
                boxSizing = BoxSizing.borderBox
            }

            child(AppComponent)
        }
    }
}