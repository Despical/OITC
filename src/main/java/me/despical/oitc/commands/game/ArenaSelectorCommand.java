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

package me.despical.oitc.commands.game;

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.number.NumberUtils;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.handlers.ChatManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaSelectorCommand extends SubCommand {

	private final ChatManager chatManager;
	private final Map<Integer, Arena> mappings = new HashMap<>();

	public ArenaSelectorCommand() {
		super("arenas");
		this.chatManager = plugin.getChatManager();

		setPermission("arenas");
		registerInventoryClickEvent();
	}

	@Override
	public String getPossibleArguments() {
		return null;
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;

		if (ArenaRegistry.getArenas().isEmpty()) {
			player.sendMessage(chatManager.getPrefix() + chatManager.colorMessage("Commands.No-Free-Arenas"));
			return;
		}

		Inventory inventory = Bukkit.createInventory(player, NumberUtils.serializeInt(ArenaRegistry.getArenas().size()), chatManager.colorMessage("Arena-Selector.Inventory-Title"));
		int slot = 0;

		mappings.clear();

		for (Arena arena : ArenaRegistry.getArenas()) {
			mappings.put(slot, arena);
			ItemStack itemStack;

			switch (arena.getArenaState()) {
				case WAITING_FOR_PLAYERS:
					itemStack = XMaterial.LIME_CONCRETE.parseItem();
					break;
				case STARTING:
					itemStack = XMaterial.YELLOW_CONCRETE.parseItem();
					break;
				default:
					itemStack = XMaterial.RED_CONCRETE.parseItem();
					break;
			}

			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(formatItem(plugin.getChatManager().colorMessage("Arena-Selector.Item.Name"), arena));

			ArrayList<String> lore = new ArrayList<>();

			for (String string : plugin.getChatManager().getStringList("Arena-Selector.Item.Lore")) {
				lore.add(formatItem(string, arena));
			}

			itemMeta.setLore(lore);
			itemStack.setItemMeta(itemMeta);
			inventory.addItem(itemStack);
			slot++;
		}

		player.openInventory(inventory);
	}

	private String formatItem(String string, Arena arena) {
		String formatted = string;

		formatted = StringUtils.replace(formatted, "%mapname%", arena.getMapName());

		if (arena.getPlayers().size() >= arena.getMaximumPlayers()) {
			formatted = StringUtils.replace(formatted, "%state%", chatManager.colorMessage("Signs.Game-States.Full-Game"));
		} else {
			formatted = StringUtils.replace(formatted, "%state%", plugin.getSignManager().getGameStateToString().get(arena.getArenaState()));
		}

		formatted = StringUtils.replace(formatted, "%playersize%", String.valueOf(arena.getPlayers().size()));
		formatted = StringUtils.replace(formatted, "%maxplayers%", String.valueOf(arena.getMaximumPlayers()));
		formatted = chatManager.colorRawMessage(formatted);
		return formatted;
	}

	private void registerInventoryClickEvent() {
		Bukkit.getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void onArenaSelectorMenuClick(InventoryClickEvent e) {
				if (!e.getView().getTitle().equals(chatManager.colorMessage("Arena-Selector.Inventory-Title"))) {
					return;
				}

				if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) {
					return;
				}

				Player player = (Player) e.getWhoClicked();
				player.closeInventory();

				Arena arena = mappings.get(e.getRawSlot());

				if (arena != null) {
					ArenaManager.joinAttempt(player, arena);
				} else {
					player.sendMessage(chatManager.getPrefix() + chatManager.colorMessage("Commands.No-Arena-Like-That"));
				}
			}
		}, plugin);
	}

	@Override
	public List<String> getTutorial() {
		return null;
	}

	@Override
	public CommandType getType() {
		return CommandType.HIDDEN;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.PLAYER;
	}
}
