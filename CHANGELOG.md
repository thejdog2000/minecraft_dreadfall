# Changelog

All notable changes to Dreadfall will be documented here.

## 0.1.0 - Unreleased

### Added

- Fabric 26.1.2 mod scaffold.
- Generated YAML config files for Overworld, nightmare, and mob settings.
- Config validation for mob ids, identifiers, block strength tiers, protected blocks, and unsupported explode-on-death behavior.
- `/dreadfall` and `/ma` status/reload commands.
- Per-mob attributes, equipment, drop chances, aggro range, and sunlight settings.
- Zombie daylight survival support.
- Skeleton explosive arrows.
- Creeper fuse and explosion radius tuning.
- Ghast fireball power tuning.
- Overworld spawn-table injection, with ghasts enabled by default.
- Mob block-breaking foundation while pursuing a player and stuck.
- Mob block-placing foundation while pursuing a player and stuck.
- Debug logging toggle.

### Known Limitations

- Block breaking and placing need more live in-game validation.
- Nightmare mode config exists, but runtime scaling is not implemented yet.
- Custom mobs are not supported yet.
- Overworld spawn-table changes require restart.

