import {useEffect, useState} from "react";
import {useParams} from "react-router";

function App() {
  const {gameId, instanceId} = useParams();
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState([]);

  useEffect(() => {
    const websocket = new WebSocket( `ws://localhost:8080/interaction/${instanceId}`);
    websocket.onopen = function (evt) {
      console.info('open', evt)
      websocket.send(JSON.stringify({action: {'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x: 2, y: 2}}}));
      websocket.send(JSON.stringify({action: {'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x: 2, y: 1}}}));
      websocket.send(JSON.stringify({action: {'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x: 1, y: 2}}}));
    };
    websocket.onclose = function (evt) {
      console.info('close', evt)
    };
    websocket.onmessage = function (evt) {
      console.info('message', evt)
    };
    websocket.onerror = function (evt) {
      console.info('error', evt)
    };

    return () => {
      console.info('component closes websocket')
      websocket.close()
    }
  }, [])

  if (error) {
      return <p>{error}</p>
  }

  return (<div>
    <p>play</p>
    <table>
      <tbody>
        <tr>
          <td>1</td>
          <td>1</td>
          <td>1</td>
        </tr>
        <tr>
          <td>1</td>
          <td>1</td>
          <td>1</td>
        </tr>
        <tr>
          <td>1</td>
          <td>1</td>
          <td>1</td>
        </tr>
      </tbody>
    </table>
  </div>);
}

export default App;
