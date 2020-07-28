# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.8.9-xxx] - unreleased
### Added
- Best friends online check:
  - check if one of your best friends is currently online
  - checked automatically after joining a server (can be disabled via `/moo config`)
  - also, manually with `/moo online`

### Changed
- renamed package to match [cowtipper.de](https://cowtipper.de)
- `/moo stalk`:
  - Replaced 3rd party with official API
  - new, shorter alias: `/moo s`
  - new, gentler alias: `/moo askPolitelyWhereTheyAre` (stalking = bad)
- `/moo stalkskyblock`:
  - added new stats: skill average, slayer levels, pets
  - new, shorter alias: `/moo ss`
  - new, gentler alias: `/moo askPolitelyAboutTheirSkyBlockProgress`

### Fixed
- Remove ": " when copying chat messages
- Fix dungeon tooltip cleaner cleaning a little bit too much
- Temporary work-around to fix crash with other mods which are not able to handle offline messages

## [1.8.9-0.9.0] - 23.07.2020
### Added
- Added SkyBlock Dungeon deaths counter
  - sends current deaths automatically; or manually with `/moo deaths`
- New alias for `/moo` command: `/m`
- New command `/rr`: alias for `/r` without the auto-replacement to `/msg <latest username>`
  - useful when someone has direct messages disabled and can only be messaged back with `/r`
- Added `/moo say [optional text]`: You can say `moo` again without triggering the command `/moo` 🎉 
- Config option: Change position of item quality in tooltip of dungeon items

### Changed
- SkyBlock related event listeners now only run while on SkyBlock, otherwise they are disabled
  - Fixes e.g. removal of enchantments in non-SkyBlock gamemodes
- Tab-completable player names now include names from:
  - party or game (duels) invites
  - Dungeon party finder: player joins group
- Some smaller improvements to Dungeon party finder (highlighting)

### Fixed
- Fixed more special case dungeon item tooltips
  - more special reforge names for specific armor items
  - now includes dungeon items without reforges
- `/moo stalk`: Fix players appearing offline when apiSession is set to `false`

## [1.8.9-0.8.1] - 20.07.2020
### Added
- Added (default) tooltip cleanup
  - hide "Dyed" for colored leather armor
  - hide enchantments (already added via lore)

### Fixed
- Fixed some special case dungeon item tooltips for specific armor with specific reforges

## [1.8.9-0.8.0] - 20.07.2020
### Added
- Copy inventories to clipboard as JSON with <kbd>CTRL</kbd> + <kbd>C</kbd>
- Dungeon update (part 1)
  - Added Dungeon item stats tooltip cleaner
    - goal: normalize stats to make comparing dungeon items much easier
    - hold <kbd>shift</kbd> while viewing the tooltip of a dungeon item: this will normalize stats (remove stats from reforging and essences ✪), recalculate the item stats inside dungeons, and display the item stats inside dungeons if it had been enhanced 5x with essences (✪)
  - Added Dungeon Party Finder improvements
    - indicate parties that (don't) meet certain criteria: "no duped roles", "class levels have to be lvl >X"
    - adjustable via `/moo config`

### Changed
- Replaced `/moo nameChangeCheck` with `/moo nameChangeCheck <playerName>`
  - Instead of triggering a manual check for name changes of *all* best friends, you can now only trigger a manual check for a single name

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

[1.8.9-xxx]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.9.0...master
[1.8.9-0.9.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.8.1...v1.8.9-0.9.0
[1.8.9-0.8.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.8.0...v1.8.9-0.8.1
[1.8.9-0.8.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.7.1...v1.8.9-0.8.0
[1.8.9-0.7.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.7.0...v1.8.9-0.7.1
[1.8.9-0.7.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.6.0...v1.8.9-0.7.0
[1.8.9-0.6.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.5.0...v1.8.9-0.6.0
[1.8.9-0.5.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.4.0...v1.8.9-0.5.0
[1.8.9-0.4.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.3.1...v1.8.9-0.4.0
[1.8.9-0.3.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.3.0...v1.8.9-0.3.1
[1.8.9-0.3.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.2.0...v1.8.9-0.3.0
[1.8.9-0.2.0]: https://github.com/cow-mc/Cowlection/compare/v0.1...v1.8.9-0.2.0
[0.1]: https://github.com/cow-mc/Cowlection/releases/tag/v0.1
