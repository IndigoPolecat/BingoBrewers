# Introducing The Bingo Brewers Mod Public Beta
[Bingo Brewers](https://github.com/IndigoPolecat/BingoBrewers) is an open source mod intended to help with bingo, it is currently under development and has 2 key features.

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

## Coins/Bingo Point Calculator
This is the original feature I created for the mod when I was first learning java in December, it displays the Coins/Bingo Point for any item in the bazaar or auction house that has price data. It isn't perfect for every use. It takes the lowest BIN, subtracts the cost of the lower talisman tier (for example a Bingo Ring) if necessary, then divides by the number of bingo points.
https://i.imgur.com/9vGIZQ5.png

# Potential Concerns

**I'm connecting to your server, are you logging my IP?**
- Nope, not logging it, not printing it, not doing anything with it.

**What is OneConfig?**
- OneConfig is the config library I'm using that is similar to Essential, but without the cosmetics and general bullshit. It's an open source mod by [PolyFrost](https://polyfrost.org/) and is likely compatible with several of your other Skyblock Mods such as NEU. It will be installed when you first launch your game.

**Is it a RAT?**
- Nope, you can read the source and compile yourself at https://github.com/IndigoPolecat/BingoBrewers.

# Upcoming Features
The next feature I expect to release will be a Crystal Hollows loot tracker that will display crowdsourced Crystal Hollows loot for the server you're in. This feature may be ready by next bingo but no guarantees.
