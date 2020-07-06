# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.8.9-0.8.0] - unreleased
### Changed
- Replaced `/moo nameChangeCheck` with `/moo nameChangeCheck <playerName>`
  - Instead of triggering a manual check for name changes of *all* best friends, you can now only trigger a manualy check for a single name

### Fixed
- Various smaller command fixed (including error messages, handling of invalid arguments, ...)

## [1.8.9-0.7.1] - 05.07.2020
### Fixed
- Fixed Nullpointer on 2nd+ game launch with empty friends file

## [1.8.9-0.7.0] - 05.07.2020
### Changed
- Renamed mod from `Cowmoonication` to `Cowlection` 🐮
  - `Cowmoonication` originally focused on **communication**-related features
  - `Cowlection` is a **collection** of various features

## [1.8.9-0.6.0] - 05.07.2020
### Added
- Minecraft log file search `/moo search`
- Analyze minions on a private island `/moo analyzeIsland` 
- List SkyBlock info of a player `/moo stalkskyblock <playerName>`
- Config option to toggle between Arabic and Roman numerals
  - used for skill levels and minion tiers currently
- Added info to auctions' tooltips: price per item
- Added item age tooltips (works for most non-stackable items)

### Changed
- Improved handling of command error messages

## [1.8.9-0.5.0] - 04.05.2020
### Added
- Added Tab-completable usernames for several commands (e.g. party, msg, boop, ...)
  - the list of supported commands can be modified via `/moo config` &rarr; `Commands with Tab-completable usernames`
  - Tab-completable usernames consist of: the last 50 players that typed in private/friends, Guild or Party chat; up to 50 best friends that are currently logged in

### Changed
- Improved player stalking feature:
  - now includes 'offline for &lt;duration&gt;'
  - better handling of special cases (e.g. nicked players, players who haven't joined in years)
- Various code refactorings 

## [1.8.9-0.4.0] - 18.04.2020
### Added
- `¯\_(ツ)_/¯`

### Changed
- Adapted to new login/logout notifications of Hypixel
  - added options to suppress friends' and guild members' notifications separately
- Improved copying of chat components
  - `ALT` + `right click` now copies just the text without any formatting
  - `SHIFT` + `ALT` + `right click` copies full chat component including all formatting, hover and click actions (original behavior)

### Fixed
- Fixed encoding

## [1.8.9-0.3.1] - 29.03.2020
### Fixed
- Fixed NullPointer in `/moo stalk` and improved mode output to be more human-readable

## [1.8.9-0.3.0] - 28.03.2020
### Added
- Added first iteration of Hypixel API integration:
  - `/moo stalk <player>`: Get info about player's status
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

*Note:* The 'best friends' list is currently available via <kbd>ESC</kbd> > Mod Options > Cowlection > Config > bestFriends.

[1.8.9-0.8.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.7.1...master
[1.8.9-0.7.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.7.0...v1.8.9-0.7.1
[1.8.9-0.7.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.6.0...v1.8.9-0.7.0
[1.8.9-0.6.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.5.0...v1.8.9-0.6.0
[1.8.9-0.5.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.4.0...v1.8.9-0.5.0
[1.8.9-0.4.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.3.1...v1.8.9-0.4.0
[1.8.9-0.3.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.3.0...v1.8.9-0.3.1
[1.8.9-0.3.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.2.0...v1.8.9-0.3.0
[1.8.9-0.2.0]: https://github.com/cow-mc/Cowlection/compare/v0.1...v1.8.9-0.2.0
[0.1]: https://github.com/cow-mc/Cowlection/releases/tag/v0.1
