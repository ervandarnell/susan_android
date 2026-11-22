# Session state — resume snapshot

Saved: project created (skeleton only, no code yet).
Project: `/home/ervan/Documents/Projects/susan_android`

Read `spec.md` first: it is the source of truth. This file records where the
implementation stands, how to build/verify it, and what is still open. Keep it
current at the end of every session so the next one can resume from it.

## What this project is

TODO: one short paragraph — what the program does and for whom.

## Layout

```
susan_android/
├── spec.md      requirements (source of truth)
├── state.md     this snapshot
└── .gitignore
```

## Build / run / test

TODO: fill in once the toolchain is chosen, e.g.

```bash
./gradlew assembleDebug        # build
./gradlew test                 # unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Behaviour implemented

Nothing yet. Map each `spec.md` line to where it is implemented as work lands.

## Verification status at save time

No tests yet.

## Known limitations / next steps

- Decide the stack (Kotlin + Jetpack Compose? Java? something else?).
- Fill in `spec.md`.
