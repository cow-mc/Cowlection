# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## 1.8.9-0.3.0 - unreleased
### Added
- `/moo nameChangeCheck`: Force a scan for changed names of best friends

### Changed
- Moved best friends add/remove functionality from config GUI back to commands (`/moo <add|remove> <name>`)
- Saving best friends' UUIDs now (instead of just the name), also checking for name changes periodically to keep best friends list up to date


## [1.8.9-0.2.0] - 08.03.2020
### Added
- Mod update notification (opt-out via config)

### Changed
- *Mod versioning now includes Minecraft version*: `<mc-version>-<modversion>` instead of just `<modversion>`
- Manage 'best friends' list via GUI instead of add/remove commands

### Fixed
- Reduced greediness of login/logout notification detection 

## [0.1] - 01.03.2020
### Added
- Toggle to hide all join/leave notifications (`/moo toggle`)
- 'Best friends' list to limit the amount of join and leave notifications (see below)
- Auto-replace `/r` with `/msg <latest username>`
- Copy chat components via <kbd>ALT</kbd> + <kbd>right click</kbd>
- Change guiScale to any value (`/moo guiscale [newValue]`)

*Note:* The 'best friends' list is currently available via <kbd>ESC</kbd> > Mod Options > Cowmoonication > Config > bestFriends.

[1.8.9-0.2.0]: https://github.com/cow-mc/Cowmoonication/compare/v0.1...v1.8.9-0.2.0
[0.1]: https://github.com/cow-mc/Cowmoonication/releases/tag/v0.1
