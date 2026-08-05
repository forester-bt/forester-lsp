# Forester LSP

Language Server Protocol server for the [Forester](https://github.com/besok/forester) behavior tree language.

## Requirements

- Java 17+

## Build

```bash
.\gradlew.bat build
```

## Run

Stdio mode (for editor integration):

```bash
.\gradlew.bat run
```

Socket mode:

```bash
.\gradlew.bat run --args="--socket --port 5007"
```

## Features

- Full-text document synchronization
- Completion provider (stub)
- ANTLR4-based parsing for `.tree` files

## License

BSD 3-Clause
