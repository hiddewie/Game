package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.boxShadow
import kotlinx.html.classes
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.TaiPanStatus
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.nextPlayer
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv

external interface PartialPlayerProps : RProps {
    var playerId: TwoTeamPlayerId
    var taiPanned: TaiPanStatus?
    var shouldPlay: Boolean
}

external interface HiddenPlayerProps : PartialPlayerProps {
    var numberOfCards: Int
}

external interface PlayerProps : PartialPlayerProps {
    var cards: CardSet
}

val HiddenPlayerComponent = functionalComponent<HiddenPlayerProps> { props ->
    styledDiv {
        css {
            display = Display.inlineBlock
        }

        val taiPannedStatus = when (props.taiPanned) {
            null -> ""
            TaiPanStatus.NORMAL -> "★"
            TaiPanStatus.GREAT -> "★★"
        }
        +"Player ${props.playerId}: Tai pan: $taiPannedStatus. ${if (props.shouldPlay) "Should play" else "Should not play"}"

        child(EmptyCardListComponent) {
            attrs.count = props.numberOfCards
        }
    }
}

val PlayerComponent = functionalComponent<PlayerProps> { props ->
    styledDiv {
        css {
            display = Display.inlineBlock
        }

        val taiPannedStatus = when (props.taiPanned) {
            null -> ""
            TaiPanStatus.NORMAL -> "★"
            TaiPanStatus.GREAT -> "★★"
        }
        +"Tai pan: $taiPannedStatus. ${if (props.shouldPlay) "Should play" else "Should not play"}"
        child(CardListComponent) {
            attrs.cards = props.cards
            attrs.showHover = props.shouldPlay
        }
    }
}
