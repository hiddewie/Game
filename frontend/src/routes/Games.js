import {useCallback, useEffect, useState} from "react";
import {Link} from "react-router-dom";

function App() {
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState([]);

  const fetchData = useCallback(async () => {
    try {
      const games = await (await fetch("http://localhost:3000/games")).json()
      setIsLoaded(true)
      setItems(games)
    } catch (error) {
      setIsLoaded(true);
      setError(error);
    }
  }, [])

  useEffect(() => {
    fetchData();
  }, [fetchData])

  if (error) {
    return <p>{error}</p>
  }

  return (<div>
    <p>
      loaded: {isLoaded ? 'true' : 'false'}
    </p>
    <ul>
      {items.map(item => <li key={item.slug}><Link to={`/open/${item.slug}`}>{item.slug}</Link></li>)}
    </ul>
  </div>);
}

export default App;
