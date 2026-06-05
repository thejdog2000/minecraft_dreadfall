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

## Configuration

On first server start, Dreadfall creates:

- `config/dreadfall/overworld_settings.yml`
- `config/dreadfall/nightmare_settings.yml`
- `config/dreadfall/mobs_settings.yml`

The config loader currently validates YAML shape, supported vanilla mob ids, identifier format, required block strength tiers, protected block defaults, and the phase-one rule that explode-on-death stays disabled.

## Commands

Admin commands require Minecraft's built-in gamemaster command permission.

- `/dreadfall status`
- `/dreadfall reload`
- `/ma status`
- `/ma reload`
