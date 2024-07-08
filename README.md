# Introducing The Bingo Brewers Mod Public Beta
[Bingo Brewers](https://github.com/IndigoPolecat/BingoBrewers) is an open source mod intended to help with bingo, it is currently under development and has 3 key features.

## Splash Notifications
When a splasher splashes in #splashes, your client will be sent a notification including the:
- Hub/Dungeon Hub Number
- Splasher
- Bingo Party listed in splash message
- Notes (Any lines the splasher added that aren't identified as for discord use (e.g. "react with :SPLASHWHEN:"), or haven't already been covered by other purposes)
The notification will disappear after 2 minutes. The title will disappear after 5 seconds.
https://i.imgur.com/ipDvQdy.jpeg

### Splash Hub Player Count
Any client inside of the splash hub will be communicating the current player count to my server, which will broadcast the current player count of the splash hub to all clients every 2 seconds.
https://i.imgur.com/bliIBRX.png

## Crystal Hollows Crowdsourced Chest Waypoints
This feature crowdsources loot from chests in the Crystal Hollows and displays it to all mod users when they join a lobby. There are filters and other settings allowing you to customize what you see.
![Ekxb59A](https://github.com/IndigoPolecat/BingoBrewers/assets/115671621/406b8652-72a9-4c4d-b200-f73b670b04e8)
The accompanying HUD also comes with a justify setting, allowing you to place it anywhere on the screen.

## Minor Features
### Coins/Bingo Point Calculator
This is the original feature I created for the mod when I was first learning java in December, it displays the Coins/Bingo Point for any item in the bazaar or auction house that has price data. It isn't perfect for every use. It takes the lowest BIN, subtracts the cost of the lower talisman tier (for example a Bingo Ring) if necessary, then divides by the number of bingo points.

### Chicken Head Reminder
Reminds you to crouch every 5 seconds.

### Auto Updater
The mod comes with an "auto" updater which will prompt you with patch notes when a new update is available. The feature allows you to easily update your JAR by clicking a single button ingame. This feature can be made fully automatic, or completely disabled depending on your preferences.

# Potential Concerns

**I'm connecting to your server, are you logging my IP?**
- Nope, not logging it, not printing it, not doing anything with it.

**What is OneConfig?**
- OneConfig is the config library I'm using that is similar to Essential, but without the cosmetics and general bullshit. It's an open source mod by [PolyFrost](https://polyfrost.org/) and is likely compatible with several of your other Skyblock Mods such as NEU. It will be installed when you first launch your game.

**Is it a RAT?**
- Nope, you can read the source and compile yourself at https://github.com/IndigoPolecat/BingoBrewers.

# Upcoming Features
The next feature I expect to release will be a Crystal Hollows loot tracker that will display crowdsourced Crystal Hollows loot for the server you're in. This feature may be ready by next bingo but no guarantees.
