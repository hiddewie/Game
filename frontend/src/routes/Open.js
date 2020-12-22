import {useCallback, useEffect, useState} from "react";
import {useParams} from "react-router";
import {Link} from "react-router-dom";

function App(props) {
  const {gameSlug} = useParams();
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState([]);

  const fetchData = useCallback(async () => {
    try {
      const games = await (await fetch(`http://localhost:3000/games/${gameSlug}/open`)).json()
      setIsLoaded(true)
      setItems(games)
    } catch (error) {
      setIsLoaded(true);
      setError(error);
    }
  }, [gameSlug])

  const startGame = async () => {
    const startedGame = await (await fetch(`http://localhost:3000/games/${gameSlug}/start`, {method: 'POST'})).json()
    console.info(`start ${JSON.stringify(startedGame)}`)
    await fetchData();
  }

  useEffect(() => {
    fetchData();
  }, [fetchData])

  if (error) {
    return <p>{error}</p>
  }

  const listItems = (<ul>
    {items.map(item => (<li key={item.id}>
      <ul>
        {item.playerSlotIds.map(playerSlotId => <li key={playerSlotId}><Link to={`/play/${gameSlug}/${item.id}/${playerSlotId}`}>{item.id} &mdash; {playerSlotId}</Link></li>)}
      </ul>
    </li>))}
  </ul>)

  return (<div>
    <p>
      loaded: {isLoaded ? 'true' : 'false'}
    </p>
    {listItems}
    <button onClick={startGame}>
      START
    </button>
  </div>);
}

export default App;
