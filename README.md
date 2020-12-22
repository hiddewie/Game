# Game

This project includes a framework and some implementations for running a game server.

- [**Core framework**](./core): interfaces *Game* (definition of a game), *Player* (definition of a player), *PlayerConfiguration* (definitions for multiple players) and *GameManager* (allows running games with players).
- [**Tic Tac Toe**](./tictactoe) Example game implementation. Game for two players that place `O` or `X` alternatively on a 3x3 board.
- [**Tai Pan**](./taipan) Example game implementation. Trick-taking card game for four players playing in two teams.
- [**Server**](./server) The web service that is the backend for playing games.

### Getting started

Build the project with `./gradlew build`. This will also run the tests.

Run the tests with `./gradlew test`.

### Features

- Fully reactive (Kotlin Coroutines) game library
- Run a game with a player composition
- Basic implementation for TicTacToe game
- Basic implementation for Tai Pan game
- Basic implementation of a game server that users can interact with

### Future (not yet implemented)

- ~~Add backend web service to play games~~
  - ~~Spring WebFlux with Coroutine support~~
  - No authentication or authorization, URLs should be short lived and unguessable, UUIDs.
  - A maximum number of active games.
  - APIs:
    - List of available games
    - For a game:
      - Open games
      - Create a new game
    - For a started game with enough players:
      - Current game state (subscription query, SSE?)
      - Player specific game state (subscription query, SSE?)
  
- Add frontend to allow users to play games.
  - URLs:
    - `/` Homepage, list of games
    - `/{game}` Homepage of a game, active games, games waiting for players, button to join existing game or set up new game.
    - `/{game}/{gameId}/join` Join an existing game, redirect to player page.
    - `/{game}/{gameId}/{playerId}` The page specific page where the game can be played

- Improve Game library
  - Games should manage some kind of state (game state and player specific state), and be more event driven internally, instead of imperative.

- CLI interface for playing games