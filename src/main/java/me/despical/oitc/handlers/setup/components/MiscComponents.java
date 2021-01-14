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

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.setup.SetupInventory;
import me.despical.oitc.handlers.sign.ArenaSign;
import me.despical.oitc.utils.conversation.SimpleConversationBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class MiscComponents implements SetupComponent {

	@Override
	public void registerComponent(SetupInventory setupInventory, StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		Main plugin = setupInventory.getPlugin();
		ItemStack bungeeItem;

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			bungeeItem = new ItemBuilder(XMaterial.OAK_SIGN.parseItem())
				.name("&e&lAdd Game Sign")
				.lore("&7Target a sign and click this.")
				.lore("&8(this will set target sign as game sign)")
				.build();
		} else {
			bungeeItem = new ItemBuilder(Material.BARRIER)
				.name("&c&lAdd Game Sign")
				.lore("&7Option disabled in bungee cord mode.")
				.lore("&8Bungee mode is meant to be one arena per server")
				.lore("&8If you wish to have multi arena, disable bungee in config!")
				.build();
		}

		pane.addItem(new GuiItem(bungeeItem, e -> {
			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				return;
			}

			e.getWhoClicked().closeInventory();
			Location location = player.getTargetBlock(null, 10).getLocation();

			if (!(location.getBlock().getState() instanceof Sign)) {
				player.sendMessage(plugin.getChatManager().colorMessage("Commands.Look-Sign"));
				return;
			}

			if (location.distance(e.getWhoClicked().getWorld().getSpawnLocation()) <= Bukkit.getServer().getSpawnRadius() && e.getClick() != ClickType.SHIFT_LEFT) {
				e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✖ &cWarning | Server spawn protection is set to &6" + Bukkit.getServer().getSpawnRadius() + " &cand sign you want to place is in radius of this protection! &c&lNon opped players won't be able to interact with this sign and can't join the game so."));
				e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&cYou can ignore this warning and add sign with Shift + Left Click, but for now &c&loperation is cancelled"));
				return;
			}

			plugin.getSignManager().getArenaSigns().add(new ArenaSign((Sign) location.getBlock().getState(), arena));
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("Signs.Sign-Created"));

			String signLoc = LocationSerializer.locationToString(location);
			List<String> locs = config.getStringList("instances." + arena.getId() + ".signs");
			locs.add(signLoc);

			config.set("instances." + arena.getId() + ".signs", locs);
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 5, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.NAME_TAG)
			.name("&e&lSet Map Name")
			.lore("&7Click to set arena map name")
			.lore("", "&a&lCurrently: &e" + config.getString("instances." + arena.getId() + ".mapname"))
			.build(), e -> {
			e.getWhoClicked().closeInventory();

			new SimpleConversationBuilder().withPrompt(new StringPrompt() {

				@Override
				public String getPromptText(ConversationContext context) {
					return plugin.getChatManager().colorRawMessage(plugin.getChatManager().getPrefix() + "&ePlease type in chat arena name! You can use color codes.");
				}

				@Override
				public Prompt acceptInput(ConversationContext context, String input) {
					String name = plugin.getChatManager().colorRawMessage(input);
					player.sendRawMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aName of arena " + arena.getId() + " set to " + name));
					arena.setMapName(name);
					config.set("instances." + arena.getId() + ".mapname", arena.getMapName());
					ConfigUtils.saveConfig(plugin, config, "arenas");

					new SetupInventory(arena, player).openInventory();
					return Prompt.END_OF_CONVERSATION;
				}
			}).buildFor(player);
		}), 6, 0);

		pane.addItem(new GuiItem(new ItemBuilder(XMaterial.FILLED_MAP.parseItem())
			.name("&e&lView Wiki Page")
			.lore("&7Having problems with setup or wanna")
			.lore("&7know some useful tips? Click to get wiki link!")
			.build(), e -> {
			e.getWhoClicked().closeInventory();
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorRawMessage("&7Check out our wiki: https://github.com/Despical/OITC/wiki"));
		}), 7, 2);
	}
}