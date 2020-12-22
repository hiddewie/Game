import React, {lazy, Suspense} from "react";
import {BrowserRouter as Router, Route, Switch} from 'react-router-dom';

const Games = lazy(() => import('./routes/Games'));
const Open = lazy(() => import('./routes/Open'));
const Play = lazy(() => import('./routes/Play'));

function App() {
  return (<Router>
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Route exact path="/" component={Games}/>
        <Route path="/open/:gameSlug" component={Open}/>
        <Route path="/play/:gameSlug/:instanceId/:playerSlotId" component={Play}/>
      </Switch>
    </Suspense>
  </Router>);
}

export default App;
