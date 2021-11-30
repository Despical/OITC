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
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.setup.SetupInventory;
import me.despical.oitc.handlers.sign.ArenaSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegisterComponent implements SetupComponent {

	@Override
	public void registerComponent(SetupInventory setupInventory, StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		ItemStack registeredItem;

		if (arena.isReady()) {
			registeredItem = new ItemBuilder(XMaterial.BARRIER)
				.name("&a&lArena Registered - Congratulations")
				.lore("&7This arena is already registered!")
				.lore("&7Good job, you went through whole setup!")
				.lore("&7You can play on this arena now!")
				.enchantment(Enchantment.DURABILITY)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.build();
		} else {
			registeredItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET)
				.name("&e&lRegister Arena - Finish Setup")
				.lore("&7Click this when you're done with configuration.")
				.lore("&7It will validate and register arena.")
				.build();
		}

		pane.addItem(GuiItem.of(registeredItem, e -> {
			player.closeInventory();

			if (arena.isReady()) {
				player.sendMessage(chatManager.coloredRawMessage("&a&l✔ &aThis arena was already validated and is ready to use!"));
				return;
			}

			String path = "instances." + arena.getId() + ".", locations[] = {"lobbylocation", "Endlocation"}, spawns[] = {"playerspawnpoints"};

			for (String location : locations) {
				if (!config.isSet(path + location) || LocationSerializer.isDefaultLocation(config.getString(path + location))) {
					player.sendMessage(chatManager.coloredRawMessage("&c&l✘ &cArena validation failed! Please configure following spawn properly: " + location + " (cannot be world spawn location)"));
					return;
				}
			}

			for (String spawn : spawns) {
				if (!config.isSet(path + spawn) || config.getStringList(path + spawn).size() < arena.getMaximumPlayers()) {
					player.sendMessage(chatManager.coloredRawMessage("&c&l✘ &cArena validation failed! Please configure following spawns properly: " + spawn + " (must be minimum " + arena.getMaximumPlayers() + " spawns)"));
					return;
				}
			}

			player.sendMessage(chatManager.coloredRawMessage("&a&l✔ &aValidation succeeded! Registering new arena instance: " + arena.getId()));

			config.set(path + "isdone", true);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			arena.setReady(true);
			arena.setPlayerSpawnPoints(config.getStringList(path + "playerspawnpoints").stream().map(LocationSerializer::fromString).collect(Collectors.toList()));
			arena.setMinimumPlayers(config.getInt(path + "minimumplayers"));
			arena.setMaximumPlayers(config.getInt(path + "maximumplayers"));
			arena.setMapName(config.getString(path + "mapname"));
			arena.setLobbyLocation(LocationSerializer.fromString(config.getString(path + "lobbylocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(path + "Endlocation")));
			arena.start();

			plugin.getSignManager().getArenaSigns().stream().filter(arenaSign -> arenaSign.getArena().equals(arena)).map(ArenaSign::getSign)
				.forEach(sign -> plugin.getSignManager().addArenaSign(sign.getBlock(), arena));
		}), 7, 3);
	}
}