# Game

This project includes a framework and some implementations for running a game server.

- [**Core framework**](./core): interfaces *Game* (definition of a game), *Player* (definition of a player), *PlayerConfiguration* (definitions for multiple players) and *GameManager* (allows running games with players).
- [**Tic Tac Toe**](./tictactoe) Example game implementation. Game for two players that place `O` or `X` alternatively on a 3x3 board.
- [**Tai Pan**](./taipan) Example game implementation. Trick-taking card game for four players playing in two teams.

### Getting started

Build the project with `./gradlew build`. This will also run the tests.

Run the tests with `./gradlew test`.
