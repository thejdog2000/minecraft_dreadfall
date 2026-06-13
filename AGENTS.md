# Minecraft Fabric Mod Agent Playbook

Use this guide when starting the next Minecraft Fabric mod. It captures the practical lessons from Dreadfall: build the smallest working mod first, make validation commands part of milestone zero, and install/playtest the actual jar early.

## Default Target

- Target the exact Minecraft version requested by the user. Do not assume the latest version from memory.
- Prefer Fabric for fast iteration unless the user explicitly requires Forge.
- Use Java and Gradle versions compatible with the requested Minecraft/Fabric Loom stack.
- Keep the first milestone server-safe and singleplayer-safe. Singleplayer runs an integrated server and is enough for early validation.
- Build for vanilla mobs/blocks/entities first. Add custom mobs/entities only after the vanilla behavior loop is proven.

## Project Setup Checklist

1. Create the git repo before coding.
2. Add a remote as soon as the user provides one.
3. Commit after each playable milestone.
4. Keep generated/build/local install artifacts out of git:
   - `.gradle/`
   - `build/`
   - `run/`
   - `out/`
   - `install-kit/`
   - `.DS_Store`
5. Include a small release/install kit locally, but ignore it from git.
6. After each build, copy the jar into the real Minecraft `mods` folder for immediate playtest when the user is validating locally.

## Fabric Boot Milestone

Milestone zero should prove:

- Fabric Loader starts.
- Fabric API is present if required.
- The mod appears in the Minecraft log.
- A command from the mod works in singleplayer.
- Config files are generated under `config/<modid>/`.

Recommended first commands:

```mcfunction
/<modid> status
/<modid> reload
/<shortalias> status
/<shortalias> reload
```

The status command should show:

- Config directory path.
- Config filenames.
- Last loaded timestamp.
- Debug logging enabled/disabled.
- Counts of loaded major config entries.
- Active runtime profile if the mod has runtime modes.

## Config Design

Avoid one giant config file. Split by ownership and lookup path:

- `world_settings.yml` or dimension-specific settings for spawn/world behavior.
- `mobs_settings.yml` for entity behavior, equipment, attributes, projectiles, AI toggles.
- `mode_settings.yml` for escalation modes, nightmare mode, difficulty curves, timed phases.

Use stable, explicit IDs:

```yaml
mobs:
  minecraft:zombie:
    enabled: true
```

Prefer namespaced Minecraft IDs everywhere:

- `minecraft:zombie`
- `minecraft:ghast`
- `minecraft:stone`

Validate configs loudly on load. Bad config should fail with a useful message and keep the game from silently doing the wrong thing.

Support old config shapes as fallbacks when evolving config structure, but make new generated defaults clean.

## Runtime Validation Commands

Build debug/test commands early. They save hours.

Recommended:

```mcfunction
/<modid> status
/<modid> reload
/<modid> testspawn minecraft:zombie
/<modid> testspawn minecraft:ghast
```

Important Brigadier lesson: namespaced IDs contain `:`, so do not parse mob IDs with `StringArgumentType.word()`. Use `greedyString()` or a proper identifier argument/parser.

Command output should prove the runtime system is loaded, not just that the jar exists.

## Logging

Add debug logging from the beginning.

Useful startup logs:

- Minecraft version.
- Config directory.
- Number of loaded config entries.
- Which spawn/behavior systems registered.
- Which major defaults are active.

Useful runtime logs:

- When settings are applied to a mob.
- When special behavior is triggered.
- Which runtime profile is active, such as daytime/nighttime/nightmare.
- Spawn pulse counts and caps.

Keep debug configurable:

```yaml
global:
  debug:
    enabled: true
```

Default debug can be true during early development, but make it easy to turn off.

## Spawn Systems

Do not rely only on vanilla biome spawn-table injection when the goal is obvious gameplay pressure. Vanilla pacing, caps, light rules, player distance, biome constraints, and randomness can make a working feature look broken.

For gameplay modes that need reliable pressure, add an active server-side spawn director:

- Runs on server tick.
- Checks dimension.
- Skips Peaceful.
- Counts nearby hostile mobs.
- Uses per-player caps.
- Uses global caps.
- Spawns around players within configurable radius.
- Applies the same mob behavior config to actively spawned mobs.
- Logs spawn pulses when debug is enabled.

For day/night tuning, use explicit profiles:

```yaml
active_spawning:
  enabled: true
  min_spawn_radius: 22
  max_spawn_radius: 58

  daytime:
    spawn_pacing_multiplier: 0.25
    global_spawn_cap: 40
    per_player_mob_cap: 6
    pulse_interval_ticks: 240
    spawn_attempts_per_player: 2

  nighttime:
    spawn_pacing_multiplier: 2.5
    global_spawn_cap: 160
    per_player_mob_cap: 45
    pulse_interval_ticks: 60
    spawn_attempts_per_player: 12
```

Remember: `20` ticks is one second. A `pulse_interval_ticks` of `60` is every three seconds.

## Behavior Hooks

For mob behavior, prefer small focused systems:

- Spawn/load applier for attributes, equipment, projectile/explosion settings.
- Block breaker for stuck/pursuing behavior.
- Block placer for stuck/pursuing behavior.
- Spawn director for active pressure.
- Commands for reload/status/testspawn.

Mob behavior should generally only modify new spawns, not entities loaded from disk, unless the mod explicitly wants persistent retroactive changes.

## Local Playtest Workflow

For macOS local playtest:

1. Install Minecraft Launcher.
2. Launch vanilla target Minecraft version once.
3. Run Fabric installer for that exact version.
4. Copy required jars into:

```text
~/Library/Application Support/minecraft/mods
```

Typical required jars:

- `<modid>-<version>.jar`
- Fabric API jar, if the mod uses Fabric API.

After installing a new jar, fully quit and restart Minecraft. `/reload` is not enough for new code.

Config changes can often use:

```mcfunction
/<modid> reload
```

But biome spawn table changes usually require a full restart because they register during startup.

## Troubleshooting Order

When something appears broken, prove layers in this order:

1. Does Minecraft launch with Fabric?
2. Does the log show the mod loaded?
3. Does `/<modid> status` work?
4. Did config generate in the expected folder?
5. Is debug enabled?
6. Does `/<modid> testspawn minecraft:zombie` work?
7. Does the debug log show settings being applied?
8. Does the natural or active runtime system produce the behavior?

If a feature works via `testspawn` but not naturally, the problem is likely spawn pacing, caps, environment rules, biome registration, or world difficulty.

If no hostile mobs appear, check:

- Difficulty is not Peaceful.
- `doMobSpawning` gamerule is true.
- The user is not judging from a very short sample window.
- The mod is not blocked by caps.
- The world is not full of existing hostile mobs outside visible range.

## Packaging And Release

Maintain:

- `README.md` with install and validation steps.
- `CHANGELOG.md`.
- `RELEASE_CHECKLIST.md`.
- `DEVELOPMENT_STATUS.md` or equivalent current-state notes.

For local handoff, create an ignored `install-kit/` containing:

- Built mod jar.
- Fabric API jar if needed.
- Fabric installer jar if useful.

Do not commit downloaded launchers, generated Minecraft folders, or built jars unless creating a separate release artifact flow.

## Engineering Style

- Keep systems small and named by behavior.
- Prefer structured config parsing over string hacks.
- Validate config shape and value ranges.
- Use namespaced IDs consistently.
- Make runtime behavior observable in commands and logs.
- Build and playtest early, before adding breadth.
- Commit only working states.
- Push after meaningful milestones.

## First Milestone Definition Of Done

A new mod's first milestone is not complete until:

- `./gradlew build` succeeds.
- The jar is installed locally.
- Minecraft launches with Fabric.
- `/<modid> status` works.
- Config files generate.
- At least one visible in-game feature is verified.
- The result is committed and pushed.

