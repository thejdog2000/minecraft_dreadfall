# Dreadfall

Dreadfall is a Fabric mod for Minecraft 26.1.2 that makes vanilla hostile mobs configurable for private survival challenge servers.

It is inspired by old apocalypse-style mob plugins, but built for a small Fabric modpack/server setup. The current focus is vanilla mobs, replayable survival maps, escalating pressure, and destructive pursuit behavior.

## Features

- Split YAML configuration instead of one huge config file.
- Overworld spawn configuration for hostile mobs, including ghasts by default.
- Per-mob health, speed, damage, follow range, and detection range.
- Configurable mob equipment and drop chances.
- Zombies can be configured to survive daylight.
- Skeleton arrows explode on impact by default.
- Creeper fuse and explosion radius can be configured.
- Ghast fireball explosion power can be configured.
- Zombies can break configured blocks while chasing a player and stuck.
- Zombies can place configured blocks while chasing a player and stuck.
- Debug logging for config loading, mob setup, projectile explosions, block breaking, and block placing.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Fabric API 0.149.1+26.1.2
- Java 25 or newer

## Download

For normal use, download the latest jar from this repository's GitHub Releases page.

For local development builds, run:

```sh
./gradlew build --no-daemon
```

The mod jar is generated at:

```text
build/libs/dreadfall-0.1.0.jar
```

Use the normal jar, not the `-sources.jar`.

## Installation

### Singleplayer

Singleplayer works because Minecraft runs an internal server.

1. Install a Fabric Loader profile for Minecraft 26.1.2.
2. Install Fabric API for Minecraft 26.1.2.
3. Put `dreadfall-0.1.0.jar` in your Minecraft `mods` folder.
4. Launch the Fabric profile.
5. Create or open a world.

On macOS, the config files generate under:

```text
~/Library/Application Support/minecraft/config/dreadfall/
```

### Dedicated Server

1. Create a Fabric 26.1.2 server.
2. Put Fabric API and `dreadfall-0.1.0.jar` in the server `mods/` folder.
3. Start the server once.
4. Accept the Minecraft EULA.
5. Restart the server.

Configs generate under:

```text
config/dreadfall/
```

For this first phase, clients should not need Dreadfall installed when joining a dedicated server, because the mod currently adds server-side behavior rather than new blocks, items, entities, or client assets.

## Configuration

Dreadfall creates three config files:

- `config/dreadfall/overworld_settings.yml`
- `config/dreadfall/nightmare_settings.yml`
- `config/dreadfall/mobs_settings.yml`

Existing config files are not overwritten when the mod updates. If defaults change in a new release, compare your config against the bundled defaults or delete your local Dreadfall config folder to regenerate fresh defaults.

Useful defaults:

- Ghasts are enabled for Overworld spawning.
- Skeleton explosive arrows are enabled.
- Zombies do not burn in daylight.
- Zombies can break and place blocks only while pursuing a player and stuck.
- Bedrock, obsidian, command blocks, portals, and similar critical blocks are protected by default.

## Commands

Commands require Minecraft's built-in gamemaster command permission.

- `/dreadfall status`
- `/dreadfall reload`
- `/ma status`
- `/ma reload`

Note: biome spawn table changes are registered at startup. Config reload updates parsed config for runtime systems, but Overworld spawn changes may require a game/server restart.

## Verification

Enable debug logging in `mobs_settings.yml`:

```yaml
global:
  debug:
    enabled: true
```

Then start a world or server and check the log for:

```text
Loaded Dreadfall configs
Registered overworld spawn for minecraft:ghast
```

Useful in-game checks:

```mcfunction
/summon ghast ~ ~10 ~
/summon skeleton ~ ~ ~
/summon zombie ~ ~ ~
```

Expected behavior:

- Ghasts can appear in Overworld spawn tables.
- Skeleton arrows explode on impact.
- Zombies can be configured with armor/weapons.
- Zombies do not burn in daylight by default.
- Zombies attempt block breaking/placing only while targeting a player and not getting closer.

## Known Limitations

- Block breaking and placing still need more live multiplayer playtesting.
- Skeleton daylight behavior is parsed but not yet overridden.
- Overworld spawn config changes may require restart.
- Custom mobs are not supported yet.
- Nightmare scaling is not implemented yet.
- Block placement is intentionally simple and not a full pathfinding rewrite.
- Region/claim protection integration is intentionally out of scope for the first private-server phase.

## Development

Build:

```sh
./gradlew build --no-daemon
```

Run a local Fabric dev server:

```sh
./gradlew runServer --no-daemon
```

The local dev server uses the ignored `run/` directory.

## Release Process

See [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md).

## License

MIT

