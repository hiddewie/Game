import {useEffect, useRef, useState} from "react";
import {useParams} from "react-router";

const emptyBoard = [[null, null, null], [null, null, null], [null, null, null]]

function TicTacToe(props) {
  const gameState = props.state?.board || emptyBoard
  const dispatchAction = props.dispatchAction;

  const play = (x, y) => {
    console.info('play', x, y)
    dispatchAction( {'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x, y}})
  }

  return (<div>
    <p>TicTacToe</p>
    <table>
      <tbody>{
        [...Array(3)].map((_, y) => (
          <tr key={y}>{
            [...Array(3)].map((_, x) => (
              <td key={x} onClick={() => play(x, y)}>{gameState[x][y] !== null ? (gameState[x][y]['@type'] === 'nl.hiddewieringa.game.tictactoe.Cross' ? 'X' : 'O') : '.'}</td>
            ))
          }</tr>
        ))
      }</tbody>
    </table>
  </div>);
}

export default TicTacToe;
