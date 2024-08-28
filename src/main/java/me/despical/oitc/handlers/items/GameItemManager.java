/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2024 Despical
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

package me.despical.oitc.handlers.items;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.number.NumberUtils;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class GameItemManager {

	private final Main plugin;
	private final GameItem arrowItem;
	private final Map<String, List<GameItem>> kits;
	private final Map<String, GameItem> gameItems;

	public GameItemManager(final Main plugin) {
		this.plugin = plugin;
		this.arrowItem = new GameItem("&7Arrow", "ARROW", 7, new ArrayList<>(), new ArrayList<>());
		this.kits = new HashMap<>();
		this.gameItems = new HashMap<>();
		this.registerItems();
	}

	public GameItem getGameItem(final String id) {
		return this.gameItems.get(id);
	}

	public void giveKit(Player player, Arena arena) {
		if (player == null) return;

		player.getInventory().clear();

		String kitId = kits.containsKey(arena.getId()) ? arena.getId() : "default";
		kits.get(kitId).forEach(item -> Utils.addItem(player, item.getItemStack(), item.getSlot()));

		player.updateInventory();
	}

	public void giveArrow(Player player, Arena arena) {
		String kitId = kits.containsKey(arena.getId()) ? arena.getId() : "default";
		Material arrowMaterial = XMaterial.ARROW.parseMaterial();
		GameItem arrow = kits.get(kitId).stream().filter(item -> item.getItemStack().getType() == arrowMaterial).findFirst().orElse(arrowItem);

		Utils.addItem(player, arrow.getItemStack(), arrow.getSlot());
	}

	private void registerItems() {
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "items");

		items:
		{
			final ConfigurationSection section = config.getConfigurationSection("items");

			if (section == null) {
				plugin.getLogger().warning("Couldn't find 'items' section in items.yml, delete file to regenerate it!");
				break items;
			}

			for (final String id : section.getKeys(false)) {
				final String path = String.format("items.%s.", id);
				final GameItem gameItem = new GameItem(config.getString(path + "name"), config.getString(path + "material"), config.getInt(path + "slot"), config.getStringList(path + "lore"), config.getStringList(path + "actions"));

				this.gameItems.put(id, gameItem);
			}
		}

		final ConfigurationSection section = config.getConfigurationSection("kits");

		if (section == null) {
			plugin.getLogger().warning("Couldn't find 'kits' section in items.yml, delete file to regenerate it!");
			return;
		}

		for (final String arenaId : section.getKeys(false)) {
			ConfigurationSection itemSection = config.getConfigurationSection(String.format("kits.%s.", arenaId));

			if (itemSection == null) continue;

			List<GameItem> kitItems = new ArrayList<>();

			for (final String slotPath : itemSection.getKeys(false)) {
				final String path = String.format("kits.%s.%s.", arenaId, slotPath);
				final int slot = NumberUtils.getInt(slotPath, -1);

				if (slot == -1) {
					plugin.getLogger().warning("Keys under 'arena id' must be an integer that represents slots!");
					return;
				}

				final Map<Enchantment, Integer> enchants = config.getStringList(path + "enchantments").stream().collect(Collectors.toMap(value -> Enchantment.getByName(value.split(":")[0]), value -> NumberUtils.getInt(value.split(":")[1], 1)));
				final List<ItemFlag> flags = config.getStringList(path + "item-flags").stream().map(ItemFlag::valueOf).collect(Collectors.toList());
				final GameItem gameItem = new GameItem(config.getString(path + "name"), config.getString(path + "material"), slot, config.getStringList(path + "lore"), flags, enchants);

				kitItems.add(gameItem);
			}

			kits.put(arenaId, kitItems);
		}
	}
}