
function TaiPan(props) {
  const gameState = props.state
  const dispatchAction = props.dispatchAction;

  console.info(gameState)

  const callTaiPan = () => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.CallTaiPan'})
  }

  const fold = () => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.Fold'})
  }

  const requestNextCards = () => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.RequestNextCards'})
  }

  const passDragonTrick = (dragonPass) => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.PassDragonTrick', dragonPass})
  }

  const playCards = (cards, addons) => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.PlayCards', cards, addons})
  }

  const passCards = (left, middle, right) => {
    dispatchAction({'@type': 'nl.hiddewieringa.game.taipan.CardPass', left, middle, right})
  }

  return (<div>
    <p>TaiPan</p>
    {gameState ? gameState['@type'] : null}
    <div>
      <h5>Actions</h5>
      <button onClick={callTaiPan}>Call tai pan</button>
      <button onClick={fold}>Fold</button>
      <button onClick={requestNextCards}>Request next cards</button>
      <button onClick={passDragonTrick}>Pass dragon trick</button>
      <button onClick={playCards}>Play cards</button>
      <button onClick={passCards}>Pass cards</button>
    </div>
  </div>);
}

export default TaiPan;
