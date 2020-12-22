import {useEffect, useState} from "react";
import {useParams} from "react-router";
import {Link} from "react-router-dom";

function App(props) {
  const {gameId} = useParams();
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState([]);

  const fetchData = async () => {
    try {
      const games = await (await fetch(`http://localhost:3000/games/${gameId}/open`)).json()
      setIsLoaded(true)
      setItems(games)
    } catch (error) {
      setIsLoaded(true);
      setError(error);
    }
  }

  const startGame = async () => {
    const startedGame = await (await fetch(`http://localhost:3000/games/${gameId}/start`, {method: 'POST'})).json()
    console.info(`start ${JSON.stringify(startedGame)}`)
    await fetchData();
  }

  useEffect(() => {
    fetchData();
  }, [])

  if (error) {
    return <p>{error}</p>
  }

  return (<div>
    <p>
      loaded: {isLoaded ? 'true' : 'false'}
    </p>
    <ul>
      {items.map(item => <li><Link to={`/play/${gameId}/${item.id}`}>{item.id}</Link></li>)}
    </ul>
    <button onClick={startGame}>
      START
    </button>
  </div>);
}

export default App;
