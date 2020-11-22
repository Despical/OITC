/*
 * OITC - Reach 25 points to win!
 * Copyright (C) 2020 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.handlers.setup.components;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.setup.SetupInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpawnComponents implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		Main plugin = setupInventory.getPlugin();
		String serializedLocation = LocationSerializer.locationToString(player.getLocation());

		pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK)
			.name("&e&lSet Ending Location")
			.lore("&7Click to set the ending location")
			.lore("&7on the place where you are standing.")
			.lore("&8(location where players will be")
			.lore("&8teleported after the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".Endlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set("instances." + arena.getId() + ".Endlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 0, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.LAPIS_BLOCK)
			.name(plugin.getChatManager().colorRawMessage("&e&lSet Lobby Location"))
			.lore("&7Click to set the lobby location")
			.lore("&7on the place where you are standing")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".lobbylocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set("instances." + arena.getId() + ".lobbylocation", serializedLocation);
			arena.setLobbyLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aLobby location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 1, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.EMERALD_BLOCK)
			.name("&e&lAdd Starting Location")
			.lore("&7Click to add the starting location")
			.lore("&7on the place where you are standing.")
			.lore("&8(locations where players will be")
			.lore("&8teleported when game starts)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneList("instances." + arena.getId() + ".playerspawnpoints", arena.getMaximumPlayers()))
			.lore("", "&8Shift + Right Click to remove all spawns")
			.build(), e -> {

			e.getWhoClicked().closeInventory();

			if (e.getClick() == ClickType.SHIFT_RIGHT) {
				config.set("instances." + arena.getId() + ".playerspawnpoints", new ArrayList<>());
				arena.setPlayerSpawnPoints(new ArrayList<>());
				player.sendMessage(plugin.getChatManager().colorRawMessage("&eDone | &aPlayer spawn points deleted, you can add them again now!"));
				arena.setReady(false);
				ConfigUtils.saveConfig(plugin, config, "arenas");
				return;
			}

			List<String> startingSpawns = config.getStringList("instances." + arena.getId() + ".playerspawnpoints");

			startingSpawns.add(LocationSerializer.locationToString(player.getLocation()));
			config.set("instances." + arena.getId() + ".playerspawnpoints", startingSpawns);
			String startingProgress = startingSpawns.size() >= arena.getMaximumPlayers() ? "&e✔ Completed | " : "&c✘ Not completed | ";
			player.sendMessage(plugin.getChatManager().colorRawMessage(
			startingProgress + "&aPlayer spawn added! &8(&7" + startingSpawns.size() + "/" + arena.getMaximumPlayers() + "&8)"));

			if (startingSpawns.size() == arena.getMaximumPlayers()) {
				player.sendMessage(plugin.getChatManager().colorRawMessage("&eInfo | &aYou can add more than " + arena.getMaximumPlayers() + " player spawns! " + arena.getMaximumPlayers() + " is just a minimum!"));
			}

			List<Location> spawns = new ArrayList<>(arena.getPlayerSpawnPoints());
			spawns.add(player.getLocation());

			arena.setPlayerSpawnPoints(spawns);
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 2, 0);
	}
}