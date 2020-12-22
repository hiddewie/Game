
function TaiPan(props) {
  const gameState = props.state
  const dispatchAction = props.dispatchAction;

  console.info(gameState)

  // const play = (x, y) => {
  //   console.info('play', x, y)
  //   dispatchAction({'@type': 'nl.hiddewieringa.game.tictactoe.PlaceMarkLocation', location: {x, y}})
  // }

  return (<div>
    <p>TaiPan</p>
    {gameState ? gameState['@type'] : null}
  </div>);
}

export default TaiPan;
