# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.8.9-0.12.0] - unreleased
### Changed
- Item age: show timestamp in the local timezone instead of "SkyBlock"-timezone (Eastern Time; also fixed the incorrect 12h ↔ 24h clock conversion)

### Fixed
- Fixed some possible problems with bad server connection
  - Fixed sending 'offline' messages (new version notification and online best friends)
  - Fixed rare occurrence of repeated triggering of server join and leave events
- Fixed other mods interfering with detection of specific chat messages

## [1.8.9-0.11.0] - 28.09.2020
### Added
- SkyBlock Dungeons Party: new command `/moo dungeon party`
    - short alias: `/m dp`):
    - displays current `/party` members' selected class, armor and dungeons floor completions

### Changed
- Completely re-done the config gui (`/moo config`)
  - now separated into sections and sub-sections
  - added moar configurable things
  - some config settings have a live-preview next to them
- Improved SkyBlock dungeon party finder
  - more config options
  - marks (non-)joinable parties even better than before
  - When a new player joins the party, it shows not only armor, but also completed dungeons stats
- Improved SkyBlock dungeon performance overlay
  - Overlay can be moved more precisely
  - Dungeons can be "joined" and "left" manually (if the automatic detection fails): `/moo dungeon <enter/leave>`
- Improved handling of invalid/missing Hypixel API key
- Dungeon item quality:
   - Show item quality + obtained floor by default (can be changed in config)
- `/moo stalkskyblock`:
   - Switched from sky.lea.moe (discontinued) to sky.shiiyu.moe
   - Added dungeons stats

### Fixed
- Fixed crash caused by another, outdated and buggy mod which sadly too many people still use
- various smaller fixes here and there, e.g.:
  - 'Create Auction' and 'Create BIN Auction' now show the price per item if multiple items are sold
  - Dungeon party finder: entered vs queued floor wasn't detected correctly
  - A dead player was counted as another death when they left the SkyBlock dungeon

## [1.8.9-0.10.2] - 15.09.2020
### Added
- Added keybinding (default `M`) to open chat with `/moo ` pre-typed
- New sub-command: `/m cmd [arguments]` to fix command conflicts with server-side commands with the same name `/m`
  - e.g. `/m cmd hello world` executes the server command `/m hello world`
- Added `/<command with tab-completable username> say [optional text]`: You can e.g. say `f` again without triggering the server-side command `/f` by typing `/f say`
- Dungeon party finder: Added a warning message if you enter a floor other than the one you have queued for 

### Fixed
- Fixed crash when entering a dungeon (caused by a small change in the scoreboard formatting)

## [1.8.9-0.10.1] - 06.08.2020
### Added
- Dungeon performance tracker: added Class Milestones

### Changed
- Reorganized `/moo` command (internally and `/moo help`)
- Added optional parameter: `/moo search [initial query]` to set the initial search query before opening the Log search

### Fixed
- Added another way to detect entering a SkyBlock dungeon
- Some very small not mention-worthy fixes

## [1.8.9-0.10.0] - 31.07.2020
### Added
- Best friends online check:
  - check if one of your best friends is currently online
  - checked automatically after joining a server (can be disabled via `/moo config`)
  - also, manually with `/moo online`
  - names of online best friends can be tab-completed as well
- SkyBlock Dungeon performance tracker:
  - Features: Skill score calculation (death counter and failed puzzle counter), destroyed crypts tracker (only detects up to ~50 blocks away from the player), and elapsed time indicator
  - Overlay + chat output
    - Chat output: runs automatically; or manually with `/moo dungeon`
    - Overlay can be modified with `/moo dungeonGui`
  - (replaces Dungeon deaths tracker)
- SkyBlock Dungeon Party Finder: Lookup of joined players' armor
  - can be disabled or changed with `/moo config`

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
- Prevent adding client-side commands to commands with Tab-completable usernames
  - This would either overwrite the existing command, or wouldn't do anything at all. Only *one* client-side command can be registered for a command name.

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

[1.8.9-0.12.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.11.0...master
[1.8.9-0.11.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.10.2...v1.8.9-0.11.0
[1.8.9-0.10.2]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.10.1...v1.8.9-0.10.2
[1.8.9-0.10.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.10.0...v1.8.9-0.10.1
[1.8.9-0.10.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.9.0...v1.8.9-0.10.0
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
