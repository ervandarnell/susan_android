# Session state — resume snapshot

Project: `/home/ervan/Documents/Projects/susan_android`

## What this project is
An Android application for the abstract board game Susan, played on a hexagonal grid with radius 5.

## Build / run / test
```bash
./gradlew assembleDebug        # build
./gradlew test                 # unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Behaviour implemented
- Hexagonal board (radius 5, flat-top orientation)
- Two-player turn-based gameplay (White and Black stones)
- Stone placement mode and stone sliding via touch drag
- Win condition detection and game-over dialogs
- Reset and New Game functionality
- Zoom and pan support on HexBoardView
- Multi-module structure (`:app`, `:core`)

## Verification status at save time
- Build verified successfully (`./gradlew assembleDebug` passed).

## Known limitations / next steps
- Unit and UI test coverage can be expanded.
