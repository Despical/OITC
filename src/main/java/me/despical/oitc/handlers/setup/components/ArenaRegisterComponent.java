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

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.setup.SetupInventory;
import me.despical.oitc.handlers.sign.ArenaSign;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegisterComponent implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		FileConfiguration config = setupInventory.getConfig();
		Main plugin = setupInventory.getPlugin();
		Arena arena = setupInventory.getArena();
		ItemStack registeredItem;

		if (!setupInventory.getArena().isReady()) {
			registeredItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
				.name("&e&lRegister Arena - Finish Setup")
				.lore("&7Click this when you're done with configuration.")
				.lore("&7It will validate and register arena.")
				.build();
		} else {
			registeredItem = new ItemBuilder(Material.BARRIER)
				.name("&a&lArena Registered - Congratulations")
				.lore("&7This arena is already registered!")
				.lore("&7Good job, you went through whole setup!")
				.lore("&7You can play on this arena now!")
				.enchantment(Enchantment.DURABILITY)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.build();
		}

		pane.addItem(new GuiItem(registeredItem, e -> {
			e.getWhoClicked().closeInventory();

			if (arena.isReady()) {
				e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&a&l✔ &aThis arena was already validated and is ready to use!"));
				return;
			}

			String path = "instances." + arena.getId() + ".";
			String[] locations = {"lobbylocation", "Endlocation"};
			String[] spawns = {"playerspawnpoints"};

			for (String s : locations) {
				if (!config.isSet(path + s) || config.getString(path + s).equals(LocationSerializer.locationToString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
					e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✘ &cArena validation failed! Please configure following spawn properly: " + s + " (cannot be world spawn location)"));
					return;
				}
			}

			for (String s : spawns) {
				if (!config.isSet(path + s) || config.getStringList(path + s).size() < arena.getMaximumPlayers()) {
					e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✘ &cArena validation failed! Please configure following spawns properly: " + s + " (must be minimum " + arena.getMaximumPlayers() + " spawns)"));
					return;
				}
			}

			e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&a&l✔ &aValidation succeeded! Registering new arena instance: " + arena.getId()));
			config.set(path + "isdone", true);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			List<Sign> signsToUpdate;
			ArenaRegistry.unregisterArena(setupInventory.getArena());

			signsToUpdate = plugin.getSignManager().getArenaSigns().stream().filter(arenaSign -> arenaSign.getArena().equals(setupInventory.getArena())).map(ArenaSign::getSign).collect(Collectors.toList());

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			arena.setReady(true);

			List<Location> playerSpawnPoints = config.getStringList(path + "playerspawnpoints").stream().map(LocationSerializer::locationFromString).collect(Collectors.toList());

			arena.setPlayerSpawnPoints(playerSpawnPoints);
			arena.setMinimumPlayers(config.getInt(path + "minimumplayers"));
			arena.setMaximumPlayers(config.getInt(path + "maximumplayers"));
			arena.setMapName(config.getString(path + "mapname"));
			arena.setLobbyLocation(LocationSerializer.locationFromString(config.getString(path + "lobbylocation")));
			arena.setEndLocation(LocationSerializer.locationFromString(config.getString(path + "Endlocation")));

			ArenaRegistry.registerArena(arena);
			arena.start();

			ConfigUtils.saveConfig(plugin, config, "arenas");

			for (Sign s : signsToUpdate) {
				plugin.getSignManager().getArenaSigns().add(new ArenaSign(s, arena));
			}
		}), 8, 2);
	}
}