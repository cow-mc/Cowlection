# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.8.9-0.16.1] - 31.01.2024
### Fixed & Improved
- Item Age in tooltips: added support for new item creation timestamp format
- Bestiary overview (`/be`):
  - fixed detection of maxed bestiary entries
  - fixed detection of bestiary categories with sub-categories
- Party Finder: added support for multi-line party notes (https://github.com/cow-mc/Cowlection/pull/8)
- Party Finder Rules Editor: changed party note display conditions for unjoinable/blocked parties in Rules Editor to match display inside Party Finder 
- Dungeons overlay: fixed possible incompatibility when another mod modifies the crypts count in the tab list

## [1.8.9-0.16.0] - 25.07.2023
### Note on API keys ❗
In case you haven't heard yet: player-based API keys are currently being phased out (see also forums post about [Public API Changes](https://hypixel.net/threads/public-api-changes-february-2023.5266129/)).

Therefore, **API-related features _that require an API key_ will probably stop working in the foreseeable future**. I _don't_ currently plan to provide my own API backend system, so sooner or later the few features in Cowlection that require an API key will probably stop working.

However, there are some Discord bots (e.g. [SkyHelper](https://skyhelper.altpapier.dev/)), other bigger SkyBlock mods, and some websites that provide similar API-based player lookup functionality.

The affected features are the following:
1. `/moo stalk`: check online status & current game of a user
2. `/moo stalkskyblock`: check SkyBlock stats of a player
3. `/moo dungeon party` (= `/moo dp`): lookup armor and dungeons stats of each Dungeons party member

All other features of Cowlection *do not* require an API key and thus should be unaffected by the API-related changes.

### Removed
- Removed Cowlection 'Best friends' list:
  - Migrate your current Cowlection best friends with `/moo bestfriends`
  - Hypixel added their own 'Best friends' list quite a while ago: [see patch notes for 'Social Update'](https://hypixel.net/threads/social-update-online-status-best-friends-more.4638020/)
    - Hypixel's best friends list does basically the same, plus doesn't require *any* API requests
    - View best friends list: `/friend list best` or `/fl best`
    - Add or remove best friend: `/friend best <player name>`
- Removed Join & leave notifications toggle for friends, best friends, and guild members
  - use Hypixel's commands instead: 
      - `/friend notifications` to cycle through the available Friends notifications options, or `/settings` → Social Settings → Friend Notifications: All / Best / None 
      - `/guild notifications` to toggle Guild notifications, or `/settings` → Personal Guild Settings → Guild Notifications

### Fixed
- Bestiary Overview (`/moo config bestiary` + `/be`): adapted tooltip detection for newly added bestiary entries
- Pet exp in tooltips: fixed rare crash caused by unexpected NBT data typing
- Bazaar: fixed "Show items left to buy/sell" for buy/sell orders not working anymore
- Enchanted books: fixed "price converted to level 1 books", as enchantments are now sold on the Bazaar, and no longer inside the Auction house
  - works on "intermediate" Bazaar pages, so the 'overview' Bazaar pages that list all levels of a certain enchantment (GUI title starts with either `Enchantments ➜` or `Ultimate Enchantments ➜`)
  - (related config options: `/moo config enchantment`)
- `/moo analyzeChests`: lowest BINs are now stored as long values instead of integers (affects only a handful of items with very high BIN prices)

### Changed
- Dungeons overlay: now disabled by default (old config entries aren't modified)
- SkyBlock player lookup: removed 'last played/last profile save' as it's no longer part of the API
- Analyze island: added new Minion (Vampire) and updated texture IDs for minions that previously erroneously shared the same skin
- `/moo directory`: added 2nd parameter to open either `/config/cowlection` or `/mods/` directory
- some Hypixel API related changes regarding API key validation

## [1.8.9-0.15.1] - 22.12.2022
### Fixed
- Fixed Dungeons Party Finder player lookup when someone joins a party

## [1.8.9-0.15.0] - 23.10.2022
### Added

- Added data for new content since the last release of Cowlection:
  - `/moo stalkskyblock`:
    - added stranded and bingo mode
    - added Social skill (exp wasn't available on the API before)
    - updated skill average calculation (carpentry is no longer a 'cosmetic' skill)
    - added new slayers (enderman + blaze)
  - `/moo analyzeIsland`: added new minions (mainly Crimson Isle related)
- `/moo analyzeIsland`:
  - Added chests and hopper counters
  - Added accumulating results: repeat the command and scan multiple areas of an island
- `/moo analyzeChests`: Chest Analyzer rework:
  - Added search
  - Added NPC sell prices (only used if an item has neither a Bazaar nor lowest BIN price, or if one of them is hidden)
  - Added 'deselect/hide item' = not calculated in total price sum (right click inside GUI)
  - Added list of coords for highlighted chests when searching for chests with a certain item (double click inside the GUI to search, hover the chat message to see coords)
  - Added info button (`[?]`)

### Changed

- `/moo search`: Allow empty search query (= returns all log entries between start and end date)
- `/moo stalkskyblock`: replaced "last played" with "last time *someone* played on the selected profile"
- Updated quick lookup for item prices and wiki: (`/moo config item`)
  - wiki: also added official wiki (use <kbd>SHIFT</kbd> to switch between Fandom and official wiki; default wiki key: <kbd>I</kbd> = info)
  - item prices: replaced stonks.gg with sky.coflnet.com (default key: <kbd>P</kbd> = price)
- Party Finder Rules Editor: increased character limit per rule from 32 to 255
- Dungeons overlay: removed outdated time penalty from overlay due to floor-based scoring
- (technical change: no longer fire `ClientChatReceivedEvent` when sending a mod-internal chat message, as too many other mods have had problems with it)

### Fixed

- Fixed various API related issues caused by updates since the last release of Cowlection:
  - determining the active/selected profile
  - Minecraft username lookup by uuid changed since username history API got removed
- Fixed GUI related issues caused by updates since the last release of Cowlection:
  - Fixed detection for `/bestiary` overview
- (technical fix: add support for [public Hypixel API SSL certificate changes](https://hypixel.net/threads/public-api-ssl-certificate-changes.5116193/))

## [1.8.9-0.14.0] - 14.08.2021
### Added
- New command: `/commandslist` to list all client-side commands added by all installed mods
- Chest Tracker & Analyzer:
  - added support for 'lowest BIN' prices
  - double clicking an analysis row now highlights chests that contain the clicked item
- Bazaar: display items left on a buy order/sell order (toggleable)
- (Dungeons) player lookups:
  - added ironman icon ♲
  - added average secrets per completion
- Dungeon Party Finder: customizable Party Notes filters (`/moo dungeon rules` or `/moo dr`)
  - Add and edit additional rules to mark Dungeon parties based on their Party notes
- Added data for Enderman slayer, Voidling minions, Hard Stone minions, and Golden Dragon (lvl 101+ pets)
- New keybindings to...
  1) run `/moo waila` command (disabled by default; MC Options > Controls > `Cowlection`)
  2) copy a single item to clipboard as JSON with <kbd>CTRL</kbd> + <kbd>C</kbd>
     - must be enabled in `/moo config > General > Copy inventories with CTRL + C` first
     - changed 'copy whole inventory' keybinding to <kbd>CTRL</kbd> + <kbd>SHIFT</kbd> + <kbd>C</kbd> (with <kbd>SHIFT</kbd>)
- New config options for older features:
  - ▶ `/moo config new`: show all new or changed config options
  - Output of `/moo waila` and copied inventory data can now also be saved to files, instead of being copied to clipboard
  - Bazaar: order 'Sell Inventory/Sacks Now' tooltips ascending or descending
  - MC Log file search (`/moo search`): maximum log file size to analyze
  - Toggle: display dungeon performance summary at the end of a dungeon
  - Toggle: send warning when queued and entered dungeon floors are different
  - Toggle: shorten item quality info for non-randomized items
- New sub-command `/moo discord`: Cowshed discord server invite

### Changed
- Disabled `M` keybinding in MC Options > Controls > Cowlection by default to avoid conflicts
- `/moo config` sub-category explanations now default to "tooltip *without* darkened background", as the darkened background was more irritating than helpful 
- MC Log file search now skips large files to prevent huge log files from blocking the search
- Dungeon Party Finder: Each dungeon class can now also be blocked or blocked if duplicated (= red party background)
- Disabled dungeon tooltip cleaner inside dungeons (+ fixed a rare crash)
- Improved error messages for API errors and API related messages
  - also added API key usage statistics to `/moo apikey`

### Fixed
- 'Show Dungeon item base stats' feature now works with HPB'd items and master stars
- Fixed Mythic pets level in player lookup
- Parsing of item tooltips should no longer be affected by other mods
- Fixed Party Finder overlay sometimes rendering behind the UI

## [1.8.9-0.13.0] - 25.04.2021
### Added
- Bestiary Overview: enhances tooltips of `/bestiary` ⬌ `/be`
  - hover over one of the area/location-items in a *sub*-category of the Bestiary to see an overview of the tiers upgrades you are closest to
  - can be ordered by fewest kills or lowest % to next tier by clicking on the area/location item
- `/moo whatAmILookingAt` (or: `/m waila`)
  - copy info of "the thing" you're looking at (NPC or mob + nearby "text-only" armor stands; armor stand, placed skull, banner, sign, dropped item, item in item frame, map on wall)
  - automatically decodes base64 data (e.g. skin details) and unix timestamps
- Chest Tracker & Analyzer: Evaluate Bazaar value of your chests
  - Select chests on your island, then get an overview of all items in the selected chests and the Bazaar value of the items
  - command: `/moo analyzeChests`
- Auction house: Mark sold/ended/expired auctions
  - either one letter (S, E, E) or the full word
- Auction house: show price for each lvl 1 enchantment book required to craft a higher tier book
  - only works on enchanted books with *one* enchantment
  - enabled for all ultimate and Turbo-crop enchantments
  - additional enchantments can be added via `/moo config` *(Keep in mind that not all high-level enchantments can be created by combining lower level books!)*
- Bazaar: Added order (sort) functionality to 'Sell Inventory/Sacks Now' tooltips
- SkyBlock Dungeon Party Finder additions:
  - new `/moo config` option: `Minimum "Dungeon level required"`
- Added new minions to `/m analyzeIslands` (Mining + Farming)
- `/moo stalkskyblock` additions:
  - Added 'last profile save' (= last time user played SkyBlock)
  - Added Enchanting, Farming, Mining, and Combat 51-60
  - Added missing Runecrafting 25
  - Added Dungeons: Catacombs Master Mode support
- `/moo stalk`: Added "Game Master" rank
- Added mini-"tutorial" on how to open the config gui (to move the dungeon overlay)
- Added a search to `/moo config`

### Changed
- Refined the comma representation of large numbers abbreviated with k, m, b
- "Copy inventories to clipboard"-feature now automatically decodes base64 data (e.g. skin details) and unix timestamps
- Dungeon Party Finder:
  - Parties with specific classes can now *always* be marked as 'unideal' (additionally to the already existing option to mark a party when 2+ members use the same specific class)
  - colored overlay is now also disable-able via config
  - Player lookup now shows - in addition to the active pet - a spirit pet
- Dungeon Performance Overlay: added an alternative text border option
- Dungeon item tooltips: Gear Score can now be hidden separately (instead of getting replaced by Item Quality)

### Fixed
- Fixed issue with 'no dung class selected'
- Unexpected API-related exceptions no longer void all chat output
- Greatly increased speed of the Log Search (`/moo search`)

## [1.8.9-0.12.0] - 03.01.2021
### Added
- New config options to change/adjust/deactivate some features:
  - Auto-replacement of `/r ` with `/w <last sender>`
  - Short alias `/m` for `/moo` command
  - Copy inventories to clipboard as JSON with <kbd>CTRL</kbd> + <kbd>C</kbd>
  - (and several more - simply look through `/moo config`)
- Added sound when a best friend comes online (deactivated by default)
- Check how long current world has been loaded
  - ≈ when server was last restarted
  - via command `/moo worldage`
  - notification when joining a recently loaded or a very old server (toggleable via config)
- SkyBlock Bazaar graphs improvements:
  - make graphs easier to read by connecting graphs' nodes
  - Fix graphs when using the mc unicode font
- Added quick lookup for item prices and wiki:
  - wiki: hypixel-skyblock.fandom.com (default key: <kbd>I</kbd> = info)
  - item prices: stonks.gg (default key: <kbd>P</kbd> = price)
- Display pet exp in pet tooltips

### Changed
- Item age: show timestamp in the local timezone instead of "SkyBlock"-timezone (Eastern Time; also fixed the incorrect 12h ↔ 24h clock conversion)
- Improved 'being on SkyBlock' detection
  - gave scoreboard more time to get detected
  - also added config option to always (or never) enable SkyBlock event listeners
- Dungeon Party Finder (overlay):
  - Made party indicators clearer (current, suitable, unideal, unjoinable party)
  - Show sizes of parties
  - Mark parties with 'carry' or 'hyperion' in their notes (disabled by default)
  - Lookup info when dungeon party is full
  - Lookup info when joining another party via Dungeon Party Finder
  - Added active pet + found dungeon secrets + dungeon types (currently only Catacombs) level to dungeon player lookup

### Fixed
- Fixed some possible problems with bad server connection
  - Fixed sending 'offline' messages (new version notification and online best friends)
  - Fixed rare occurrence of repeated triggering of server join and leave events
- Fixed other mods interfering with detection of specific chat messages
- SkyBlock Dungeons related:
  - Fixed deaths sometimes being counted multiple times
  - Read destroyed crypts from tab list (if available) for more accurate numbers
  - Fixed rarely occurring infinite message loop
- MC log search: now ignores corrupted/broken files instead of displaying an error

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

[1.8.9-0.16.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.16.0...v1.8.9-0.16.1
[1.8.9-0.16.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.15.1...v1.8.9-0.16.0
[1.8.9-0.15.1]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.15.0...v1.8.9-0.15.1
[1.8.9-0.15.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.14.0...v1.8.9-0.15.0
[1.8.9-0.14.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.13.0...v1.8.9-0.14.0
[1.8.9-0.13.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.12.0...v1.8.9-0.13.0
[1.8.9-0.12.0]: https://github.com/cow-mc/Cowlection/compare/v1.8.9-0.11.0...v1.8.9-0.12.0
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
