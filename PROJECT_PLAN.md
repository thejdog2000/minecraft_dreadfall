# MonsterApocalypse Rebuild Project Plan

## Product Direction

Build a small-server Minecraft Java mod inspired by the old MonsterApocalypse plugin, targeting Minecraft 26.1.2. The first implementation should be a Fabric mod for private survival challenge servers with 2-3 players, focused on configurable vanilla hostile mob behavior.

The core game loop is survival-run oriented: players replay interesting maps, survive escalating hostile pressure, and can reset worlds often. Because this is not intended for public protected servers, phase one will not include land-claim or region-protection integrations.

## Technical Baseline

- Platform: Fabric mod
- Minecraft version: 26.1.2
- Scope: server-side gameplay behavior for vanilla mobs
- Initial audience: private friend server, 2-3 players
- Out of scope for phase one: custom mobs, claim protection, public-server grief controls, client-only visual features, custom loot tables, virtual spawn points, always-night mode, explode-on-death behavior
- Future-compatible design goal: isolate config and behavior modules so custom mobs can be added later without rewriting the core systems

## Configuration Model

Use a small set of domain-focused config files instead of one large legacy-style file.

### `config/monsterapocalypse/overworld_settings.yml`

Controls world and spawn behavior.

- Enabled worlds and dimensions
- Overworld spawning rules for vanilla mobs that do not normally spawn there, such as blazes and ghasts
- Spawn weights
- Spawn caps
- Group sizes
- Light-level constraints
- Height ranges
- Biome allowlists and denylists
- Spawn pacing multipliers
- Daylight behavior defaults for natural overworld pressure, while per-mob sunlight behavior lives in `mobs_settings.yml`

### `config/monsterapocalypse/nightmare_settings.yml`

Controls run escalation and optional nightmare behavior.

- Nightmare mode enabled/disabled
- Start trigger: immediate, day number, command, or percentage of players sleeping
- Scaling curves by day/time
- Spawn intensity multipliers
- Equipment chance multipliers
- Block breaking aggression multipliers
- Block placing aggression multipliers
- Future hooks for wave events and boss-like pressure

### `config/monsterapocalypse/mobs_settings.yml`

Controls per-mob behavior.

- Vanilla mob enable/disable
- Health, speed, damage, follow range, detection range
- Armor and held item loadouts
- Equipment drop chances
- Enchantments
- Aggro enablement and configurable detection/follow ranges
- Sunlight burning behavior
- Explosion behavior for mobs that already explode or fire explosive projectiles
- Block breaking settings
- Block placing settings
- Targeting and pursuit behavior modifiers
- Vanilla drops only for phase one

### Block Strength Tiers

Block break timing should be configured through named strength tiers, with optional per-block overrides.

- `very_weak`
- `weak`
- `normal`
- `strong`
- `very_strong`

Example design intent:

- Dirt, sand, gravel: weak or very weak
- Wood, glass, wool: weak or normal
- Stone, brick: strong
- Obsidian, reinforced blocks: very strong or unbreakable

Phase-one destructive policy:

- Default stance is aggressive: mobs may break almost anything when configured to do so.
- Bedrock, obsidian, command blocks, barriers, portal blocks, and other server-critical blocks should be unbreakable by default.
- The default config should make the hard blacklist obvious and easy to edit.

## Core Behavior Rule

Mobs may only break or place blocks when all of these are true:

- The mob is actively moving toward a player target.
- The mob is blocked or stuck.
- The mob is not getting closer to the target over a short measured interval.
- The configured mob type has block breaking or block placing enabled.
- The target block or placement material is allowed by config.

This keeps the behavior survival-pressure oriented instead of general world griefing.

## Agile Delivery Plan

Each deliverable should be independently testable before moving to the next one.

### Milestone 0: Repository and Build Foundation

Goal: establish a clean Fabric 26.1.2 project that can build and load.

Tasks:

- Create Gradle Fabric mod project.
- Add mod metadata.
- Add package structure for config, spawn control, mob behavior, commands, and tests.
- Add `.gitignore`.
- Add README with dev setup and server install notes.
- Add first CI-ready build command documentation.

Acceptance criteria:

- `./gradlew build` produces a mod jar.
- A local Fabric 26.1.2 server starts with the mod installed.
- Server logs show the mod loaded successfully.

Validation:

- Build the jar.
- Launch test server.
- Confirm no startup errors.

### Milestone 1: Config Loader and Validation

Goal: implement the three-file config system with clear defaults and errors.

Tasks:

- Create default `overworld_settings.yml`.
- Create default `nightmare_settings.yml`.
- Create default `mobs_settings.yml`.
- Implement config file generation on first run.
- Implement strict validation with line/path-aware error messages where practical.
- Add `/ma reload` or `/monsterapocalypse reload`.
- Add debug config dump command for development.

Acceptance criteria:

- Missing configs are generated.
- Valid configs load successfully.
- Invalid configs fail safely with useful messages.
- Reload command updates runtime settings without a full server restart where feasible.

Validation:

- Start server with no config files.
- Intentionally break config syntax.
- Intentionally use an invalid mob or block id.
- Reload changed settings in-game.

### Milestone 2: Per-Mob Attribute and Equipment Rules

Goal: customize vanilla mob stats, aggro range, sunlight behavior, and loadouts.

Tasks:

- Hook vanilla hostile mob spawn events.
- Apply per-mob health, speed, damage, follow range, and detection range.
- Add configurable mega-aggro enablement and range settings.
- Add configurable sunlight burning behavior for mobs that normally burn.
- Apply armor and weapon loadouts.
- Apply enchantments.
- Apply equipment drop chances.
- Add a development spawn/test command for specific configured mobs.

Acceptance criteria:

- Zombies can spawn with configured armor and swords.
- Skeletons, creepers, spiders, blazes, ghasts, and other vanilla hostile mobs can receive supported stat changes.
- Zombies and skeletons can be configured to survive daylight.
- Mobs can be configured to detect and pursue players from much farther away than vanilla.
- Disabled mob configs leave vanilla behavior untouched.

Validation:

- Spawn configured zombies and inspect equipment.
- Compare configured health and movement speed.
- Test mega-aggro at short, medium, and long ranges.
- Test daylight burning enabled and disabled.
- Confirm disabled mobs remain vanilla.

### Milestone 3: Combat Ability Tuning

Goal: support high-impact legacy-inspired mob combat behavior without enabling explode-on-death.

Tasks:

- Add configurable creeper explosion radius and fuse behavior.
- Add configurable ghast fireball explosion power.
- Add configurable blaze projectile pressure where supported by Fabric hooks.
- Add optional true-damage style settings if practical without invasive combat rewrites.
- Keep drops vanilla.
- Explicitly omit explode-on-death behavior.

Acceptance criteria:

- Creeper explosions can be made stronger or weaker from config.
- Ghast fireball explosions can be tuned from config.
- Explode-on-death is not implemented or exposed in config.
- Vanilla drops remain unchanged.

Validation:

- Spawn creepers with several explosion-radius settings.
- Spawn ghasts and compare fireball impact behavior.
- Confirm entity death does not trigger custom explosions.

### Milestone 4: Overworld Spawn Control

Goal: allow vanilla hostile mobs to spawn in the Overworld according to config, including mobs like ghasts and blazes.

Tasks:

- Implement overworld spawn injection for configured mobs.
- Support spawn weights and group sizes.
- Support biome filters.
- Support height filters.
- Support light constraints.
- Support global and per-mob spawn caps.
- Add debug logging for why spawn attempts are accepted or rejected.

Acceptance criteria:

- Blazes can spawn in the Overworld when enabled.
- Ghasts can spawn in the Overworld when enabled.
- Spawn settings are configurable without code changes.
- Spawn caps prevent runaway entity counts.

Validation:

- Test flat world with controlled config.
- Confirm enabled mobs spawn.
- Confirm disabled mobs do not spawn.
- Confirm caps are respected.

### Milestone 5: Stuck Detection and Pursuit State

Goal: identify when a mob is trying to reach a player but is blocked.

Tasks:

- Track target player per mob.
- Sample mob-to-target distance over time.
- Detect blocked or not-approaching state.
- Add cooldowns to avoid excessive checks.
- Add debug particles/logging option for stuck-state testing.

Acceptance criteria:

- A mob walking normally toward a player is not considered stuck.
- A mob blocked by a wall is considered stuck after the configured delay.
- A mob that loses target stops block interaction attempts.

Validation:

- Place player behind a wall and observe stuck detection.
- Let mob path around an obstacle and confirm it does not break/place unnecessarily.

### Milestone 6: Block Breaking

Goal: allow configured mobs to break configured blocks only when pursuit-blocked.

Tasks:

- Implement block strength tier config.
- Implement per-block break time overrides.
- Implement per-mob break permissions.
- Implement break attempt cooldowns.
- Add unbreakable blocks list, with bedrock and obsidian blocked by default.
- Add visual/progress feedback if feasible.
- Ensure breaking runs on server tick safely.

Acceptance criteria:

- Zombies can break configured weak blocks while pursuing a player.
- Stronger blocks take longer.
- Disallowed blocks are never broken.
- The default config allows broad destruction while protecting server-critical blocks.
- Mobs do not break blocks when idle or wandering.

Validation:

- Test dirt, wood, stone, obsidian, and bedrock.
- Confirm break timing matches configured tiers.
- Confirm behavior stops when player target is gone.

### Milestone 7: Block Placing

Goal: allow configured mobs to place blocks only when pursuit-blocked.

Tasks:

- Implement per-mob block placement settings.
- Support allowed placement materials.
- Support placement cooldowns.
- Support max placement attempts.
- Support simple bridging or climbing behavior when blocked.
- Prevent obvious spam loops.

Acceptance criteria:

- Configured mobs can place blocks to reach or pressure players.
- Mobs only place blocks when actively pursuing and stuck.
- Placement respects configured material and cooldown.

Validation:

- Test mob below player ledge.
- Test mob across small gap.
- Confirm no block placement when idle.

### Milestone 8: Nightmare Mode

Goal: add configurable survival-run escalation.

Tasks:

- Implement nightmare enabled flag.
- Implement time/day-based scaling.
- Apply scaling to spawn rates, equipment odds, block breaking speed, and block placement odds.
- Add commands to start, stop, and inspect nightmare state.
- Add preset defaults for a replayable survival challenge.

Acceptance criteria:

- Nightmare mode can be enabled from config or command.
- Difficulty escalates predictably over time.
- Current nightmare state can be inspected in-game.

Validation:

- Start a test run at day 0.
- Fast-forward time.
- Confirm spawn and mob behavior intensity changes.

### Milestone 9: Packaging, Docs, and Playtest Loop

Goal: make the mod easy to install, tune, and replay.

Tasks:

- Write install instructions for the friend server.
- Document each config file.
- Add sample presets: mild, classic, nightmare.
- Add changelog.
- Create playtest checklist.
- Add known limitations and future backlog.

Acceptance criteria:

- A friend can install the jar and generated config on a Fabric 26.1.2 server.
- Config changes can be made by editing three predictable files.
- The server can be reset and replayed without manual cleanup.

Validation:

- Fresh server install test.
- 30-minute survival playtest.
- Tune config from observed gameplay.

## Future Backlog

These are intentionally deferred until the vanilla-mob foundation is stable.

- Custom mobs from other mods.
- Custom mob equipment pools by biome or day.
- Custom loot tables.
- Virtual spawn points with timers.
- Always-night mode.
- Explode-on-death behavior.
- Wave events.
- Blood moon or scheduled invasion nights.
- Per-player aggro range scaling.
- Legacy MonsterApocalypse feature parity review.
- Smarter digging and tunneling.
- Structure-aware siege behavior.
- Boss-style elite mob variants.
- Integration with map presets.
- Optional server-side compatibility layer for permissions or protection mods.
- Config migration tools between versions.

## Immediate Next Steps

1. Scaffold the Fabric 26.1.2 Gradle project.
2. Commit the planning baseline.
3. Build a minimal mod that logs startup.
4. Add config generation for the three config files.
5. Validate the mod on a local Fabric 26.1.2 server.
