import {useEffect, useState} from "react";
import {useParams} from "react-router";
import {useRef} from "react";

function App() {
  const {gameId, instanceId, playerSlotId} = useParams();
  const [error, setError] = useState(null);
  const [gameState, setGameState] = useState([[null, null, null], [null, null, null], [null, null, null]]);
  const webSocket = useRef(null);

  useEffect(() => {
    webSocket.current =  new WebSocket( `ws://localhost:8080/interaction/${instanceId}/${playerSlotId}`);
    webSocket.current.onopen = function (evt) {
      console.info('open', evt)
    };
    webSocket.current.onclose = function (evt) {
      console.info('close', evt)
    };
    webSocket.current.onmessage = function (evt) {
      const data = JSON.parse(evt.data)
      const {event, state} = data
      console.info('message', {event, state})
      setGameState(state.board)
    };
    webSocket.current.onerror = function (evt) {
      console.info('error', evt)
    };

    return () => {
      console.info('component closes webSocket')
      webSocket.current?.close()
    }
  }, [])

  const play = (x,y) => {
    console.info('play', x, y)
    webSocket.current?.send(JSON.stringify({action: {'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x, y}}}))
  }

  if (error) {
      return <p>{error}</p>
  }

  return (<div>
    <p>Game {gameId} instance {instanceId} player {playerSlotId}</p>
    <table>
      <tbody>
      {
        [...Array(3)].map((_, y) => (
          <tr key={y}>
            {
              [...Array(3)].map((_, x) => (
                <td key={x} onClick={() => play(x,y)}>-> {gameState[x][y] !== null ? 'v' : 'x'}</td>
              ))
            }
          </tr>
        ))
      }
      </tbody>
    </table>
  </div>);
}

export default App;
