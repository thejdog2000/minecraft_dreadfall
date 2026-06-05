# Dreadfall

Dreadfall is a Fabric mod for Minecraft 26.1.2 that will add configurable hostile mob escalation for private survival challenge servers.

The project is currently in Milestone 0: repository and build foundation.

## Requirements

- Java 25 or newer
- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer

## Development

Build the mod:

```sh
./gradlew build
```

The mod jar will be generated under `build/libs/`.

## Current Scope

- Fabric mod targeting Minecraft 26.1.2
- Vanilla hostile mobs first
- Split configuration files planned:
  - `overworld_settings.yml`
  - `nightmare_settings.yml`
  - `mobs_settings.yml`
- Configurable mega-aggro, sunlight behavior, overworld spawns, block breaking, block placing, and explosion tuning

