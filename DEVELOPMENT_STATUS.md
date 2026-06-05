# Development Status

Last updated: 2026-06-05

## Repository

- Remote: `git@github.com:thejdog2000/minecraft_dreadfall.git`
- Branch: `main`
- Current mod id: `dreadfall`
- Target: Minecraft 26.1.2, Fabric Loader 0.19.2

## Implemented

- Fabric 26.1.2 Gradle project scaffold.
- Gradle wrapper using Gradle 9.4.0.
- Buildable mod jar at `build/libs/dreadfall-0.1.0.jar`.
- Three generated YAML config files:
  - `config/dreadfall/overworld_settings.yml`
  - `config/dreadfall/nightmare_settings.yml`
  - `config/dreadfall/mobs_settings.yml`
- Config validation for:
  - YAML root shape.
  - vanilla hostile mob ids.
  - identifier formatting.
  - required block strength tiers.
  - required protected blocks such as bedrock and obsidian.
  - explode-on-death staying disabled.
- Commands:
  - `/dreadfall status`
  - `/dreadfall reload`
  - `/ma status`
  - `/ma reload`
- Mob spawn/load behavior:
  - applies configured health, speed, attack damage, and follow range.
  - uses `aggro.detection_range` as a follow-range floor.
  - applies configured equipment and drop chances.
  - skips entities loaded back from disk to avoid rerolling equipment on chunk load.
- Sunlight behavior:
  - zombie daylight burning can be disabled through config.
- Explosion tuning:
  - creeper fuse and explosion radius can be configured.
  - ghast-owned large fireball explosion power can be configured.
  - explode-on-death is intentionally not implemented.
- Overworld spawn injection:
  - configured mobs are added to Overworld biome spawn tables on startup.
  - biome allow/deny lists are supported.
- Block breaking foundation:
  - configured mobs only attempt block breaking while targeting a player.
  - mobs must be not getting closer to the player over the configured stuck interval.
  - block strength tiers control accumulated break time.
  - protected/unbreakable blocks are respected.
- Block placing foundation:
  - configured mobs only attempt block placing while targeting a player.
  - same stuck/not-closer condition as breaking.
  - placement uses configured allowed blocks, cooldown, and max attempts per stuck event.

## Verified

- `./gradlew build --no-daemon` succeeds.
- `./gradlew runServer --no-daemon` boots a local Fabric 26.1.2 server.
- Dreadfall loads and generates configs.
- `/dreadfall status` works from server console.
- `/summon zombie` works with the spawn/load hook active.
- `/summon creeper` works with creeper accessor mixin active.
- `/summon ghast` works with large fireball accessor mixin active.
- Overworld spawn registration logs and Fabric biome modification logs appear at startup.

## Known Limitations

- Block breaking and placing need real in-game validation with a player target behind a wall/gap.
- Skeleton daylight behavior is parsed but not yet overridden; zombie daylight behavior is implemented.
- Overworld spawn injection is startup-time only. Config reload updates parsed values, but biome spawn table changes require restart for now.
- Ghast fireball tuning is wired for ghast-owned fireballs, but needs live in-game combat validation.
- No custom mob support yet.
- No nightmare scaling runtime system yet.
- No block placement smart pathing beyond simple bridge/climb candidate blocks.
- No protection integration by design for phase one.

## Recommended Next Steps

1. Add a dev/test command that spawns a configured mob and reports applied attributes/equipment.
2. Validate zombie wall breaking in a live client session.
3. Validate zombie block placing across a small gap and toward a ledge.
4. Implement skeleton daylight override if needed.
5. Add first nightmare scaling pass for spawn intensity and behavior multipliers.
6. Add better debug logging toggles for stuck detection and block interaction decisions.

