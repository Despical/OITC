/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2021 Despical and contributors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.handlers.setup.components;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.setup.SetupInventory;
import org.bukkit.Location;
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

	@Override
	public void registerComponent(SetupInventory setupInventory, StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		String serializedLocation = LocationSerializer.toString(player.getLocation());

		pane.addItem(GuiItem.of(new ItemBuilder(XMaterial.REDSTONE_BLOCK)
			.name("&e&lSet Ending Location")
			.lore("&7Click to set the ending location")
			.lore("&7on the place where you are standing.")
			.lore("&8(location where players will be")
			.lore("&8teleported after the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("Endlocation"))
			.build(), e -> {

			player.closeInventory();

			config.set("instances." + arena.getId() + ".Endlocation", serializedLocation);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			arena.setEndLocation(player.getLocation());
			player.sendMessage(chatManager.coloredRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getId() + " set at your location!"));
		}), 1, 1);

		pane.addItem(GuiItem.of(new ItemBuilder(XMaterial.LAPIS_BLOCK)
			.name(chatManager.coloredRawMessage("&e&lSet Lobby Location"))
			.lore("&7Click to set the lobby location")
			.lore("&7on the place where you are standing")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("lobbylocation"))
			.build(), e -> {

			player.closeInventory();
			config.set("instances." + arena.getId() + ".lobbylocation", serializedLocation);
			arena.setLobbyLocation(player.getLocation());
			player.sendMessage(chatManager.coloredRawMessage("&e✔ Completed | &aLobby location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 2, 1);

		pane.addItem(GuiItem.of(new ItemBuilder(XMaterial.EMERALD_BLOCK)
			.name("&e&lAdd Starting Location")
			.lore("&7Click to add the starting location")
			.lore("&7on the place where you are standing.")
			.lore("&8(locations where players will be")
			.lore("&8teleported when game starts)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneList("playerspawnpoints", arena.getMaximumPlayers()))
			.lore("", "&8Shift + Right Click to remove all spawns")
			.build(), e -> {

			player.closeInventory();

			if (e.getClick() == ClickType.SHIFT_RIGHT) {
				player.sendMessage(chatManager.coloredRawMessage("&eDone | &aPlayer spawn points deleted, you can add them again now!"));

				arena.setPlayerSpawnPoints(new ArrayList<>());
				arena.setReady(false);

				config.set("instances." + arena.getId() + ".playerspawnpoints", new ArrayList<>());
				ConfigUtils.saveConfig(plugin, config, "arenas");
				return;
			}

			List<String> startingSpawns = config.getStringList("instances." + arena.getId() + ".playerspawnpoints");
			startingSpawns.add(LocationSerializer.toString(player.getLocation()));

			config.set("instances." + arena.getId() + ".playerspawnpoints", startingSpawns);

			String startingProgress = startingSpawns.size() >= arena.getMaximumPlayers() ? "&e✔ Completed | " : "&c✘ Not completed | ";
			player.sendMessage(chatManager.coloredRawMessage(
			startingProgress + "&aPlayer spawn added! &8(&7" + startingSpawns.size() + "/" + arena.getMaximumPlayers() + "&8)"));

			if (startingSpawns.size() == arena.getMaximumPlayers()) {
				player.sendMessage(chatManager.coloredRawMessage("&eInfo | &aYou can add more than " + arena.getMaximumPlayers() + " player spawns! " + arena.getMaximumPlayers() + " is just a minimum!"));
			}

			List<Location> spawns = new ArrayList<>(arena.getPlayerSpawnPoints());
			spawns.add(player.getLocation());

			arena.setPlayerSpawnPoints(spawns);
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 3, 1);
	}
}