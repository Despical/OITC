### 1.1.1 Release (13.01.2020)
* Added online player completion to tab complete for stats command.
* Added more option to customize for spectator and lobby items.
* Added play again item.
* Added support for Minecraft 1.16 and higher versions.
* Added 144 character support for scoreboard entries.
* Added `Arena#broadcastMessage` method and removed `ChatManager#broadcastMessage`.
* Fixed default script engine commands in rewards file.
* Fixed joining through a sign while player is holding leave item.
* Fixed colorizing for scoreboard entries in Minecraft 1.14 and higher versions.
* Fixed Hex colors in debugger and normal in-game messages.
* Fixed slot for lobby items.
* Fixed and updated license header.
* Remove duplicates spectator events.
* Removed default false booleans.
* Deleted unnecessary default false booleans.
* Changed some events in efficient way.
* Changed default leave item to red bed from white bed.
* Changed PAPI's version with Bukkit's.
* Changed some debugger keys.
* Now list command replaces values faster.
* Now system saves and loads player data faster.
* Improved tab completion.
* Too many performance improvements.

### 1.1.0 Release (22.10.2020 - 31.10.2020)
* Added new placeholder to get amount of players in arena without spectator amount:
  * `%oitc_{arena_id}:players_left%`
* Added enable option for spectator settings.
* Fixed possible NPE when registering commands.
* Fixed broken imports in InventoryFramework library.
* Fixed getting NPE when place signs another world.
* Fixed scoreboard friendly fire caused players visible.
* Fixed NPE when signs aren't located in default world.
* Fixed statistics are not resetting after game finishes.
* Optimized events.

### 1.0.9 Release (09.10.2020 - 14.10.2020)
* Fixed debugger sends prefix twice.
* Fixed NPE with conversation builder.
* Fixed item lores for Minecraft 1.13 and higher versions.
* Replaced some methods with thread-safe API methods.
* Command optimizations and performance improvements.
* Improved GUI performances.

### 1.0.8 Addition Hotfix Release (07.10.2020)
* Fixed all of the sign problems.
* Removed unnecessary colorize method for lores.

### 1.0.7 Hotfix Release (06.10.2020)
* Added more in-game tips.
* Added license header.
* Fixed pom.xml issues.
* Fixed signs.
* Fixed sending prefix twice.
* Fixed bStats metrics.
* Removed static handler list getter from events. 
* Improved GUI performances.

### 1.0.6 Release (29.09.2020 - 03.10.2020)
* Added more in-game tips.
* Added missing 1.14, 1.15 and 1.16 items.
* Added arena selector.
* Added new death reward.
* Added death prefix.
* Added support for 1.16 hex colors
* Fixed wrong order in bow trails.
* Fixed NPE when trying to teleport players to lobby location.
* Fixed update links.
* Fixed plugin version for PAPI.
* Fixed player skulls on spectator menu.
* Fixed update checker.
* Fixed separate chat.
* Fixed prefix is not updating on reload with plugin command.
* Removed system message options.
* Removed unnecessary command exceptions.
* Changed MySQL updates to do only one instead of more than nearly 15.
* Changed message delay of "Waiting for players" from 15 to 45 seconds.
* Reworked on update checker.
* Reworked on tab completion.
* Reworked on spectator settings menu.
  * Added no speed option.
  * Added disable night vision option.
  * Added hide spectators option.
* Updated dependencies to latest versions.
* So many improvements for newer version of Java.
* Made code more readable.

### 1.0.5 Release (03.09.2020)
* Added new message options.
* Improved arena creation.
* Some bug fixes.

### 1.0.5 Pre-Release (30.08.2020
* Generated JavaDocs.

### 1.0.4 (24.08.2020)
* Added Minecraft 1.16.2 compatibility.

### 1.0.3 Release (17.08.2020)
* Fixed Bungee-cord mode getting null pointer exception.

### 1.0.2 Release (01.08.2020)
* Fixed all of the messages.
* Performance improvements.

### 1.0.1 Release (22.07.2020)
* Fixed summary messages getting null pointer exception.
* Some performance updates.
* Activated update checker