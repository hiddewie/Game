import {useEffect, useRef, useState} from "react";
import {useParams} from "react-router";
import TicTacToe from "./games/TicTacToe";
import TaiPan  from "./games/TaiPan";

function App() {
  const {gameSlug, instanceId, playerSlotId} = useParams();
  const [connected, setConnected] = useState(false);
  const [gameState, setGameState] = useState(null);
  const webSocket = useRef(null);

  useEffect(() => {
    webSocket.current = new WebSocket(`ws://localhost:8080/interaction/${instanceId}/${playerSlotId}`);
    webSocket.current.onopen = function (evt) {
      console.info('open', evt)
      setConnected(true)
    };
    webSocket.current.onclose = function (evt) {
      console.info('close', evt)
      setConnected(false)
    };
    webSocket.current.onmessage = function (evt) {
      const data = JSON.parse(evt.data)
      const {event, state} = data
      console.info('message', 'event',event,'state', state)
      setGameState(state)
    };
    webSocket.current.onerror = function (evt) {
      console.info('error', evt)
    };

    return () => {
      console.info('component closes webSocket')
      webSocket.current?.close()
      webSocket.current = null
      setConnected(false)
    }
  }, [instanceId, playerSlotId])

  const dispatchAction = (action) => {
    webSocket.current?.send(JSON.stringify({action}))
    console.info('action', action)
  }

  return (<div>
    <p>Game {gameSlug} instance {instanceId} player {playerSlotId}</p>
    <p>{connected ? 'websocket connected' : 'websocket disconnected'}</p>
    {gameSlug === 'tic-tac-toe' ?
      <TicTacToe state={gameState} dispatchAction={dispatchAction} /> : null}
    {gameSlug === 'tai-pan' ?
      <TaiPan state={gameState} dispatchAction={dispatchAction} /> : null}
  </div>);
}

export default App;
