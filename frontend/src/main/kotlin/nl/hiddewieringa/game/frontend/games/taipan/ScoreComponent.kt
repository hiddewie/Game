package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.card.*
import react.RProps
import react.dom.div
import react.dom.tbody
import react.dom.td
import react.dom.tr
import react.functionalComponent
import styled.*

external interface ScoreProps : RProps {
    var points: Map<TwoTeamTeamId, Int>
    var playerId: TwoTeamPlayerId
    var round: Int?
    var trick: Int?
}

val ScoreComponent = functionalComponent<ScoreProps> { props ->
    div {
        styledTable {
            css {
                margin = "0 auto"
                put("font-variant", "small-caps")
            }

            tbody {
                tr {
                    styledTd {
                        css {
                            textAlign = TextAlign.right
                            paddingRight = 1.rem
                        }
                        +"you"
                    }
                    td {
                        +"them"
                    }
                }
                styledTr {
                    css {
                        fontSize = (1.5).rem
                    }
                    styledTd {
                        css {
                            textAlign = TextAlign.right
                            paddingRight = 1.rem
                        }
                        +props.points[props.playerId.team].toString()
                    }
                    td {
                        +props.points[props.playerId.team.otherTeam()].toString()
                    }
                }
            }
        }

        styledDiv {
            css {
                textAlign = TextAlign.center
                put("font-variant", "small-caps")
            }
            if (props.round != null && props.trick != null) {
                +"Round ${props.round} ${Typography.mdash} Trick ${props.trick}"
            }
        }
    }
}
