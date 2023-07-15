/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2023 Despical
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

package me.despical.oitc.events.spectator;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemUtils;
import me.despical.commons.number.NumberUtils;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.events.ListenerAdapter;
import org.bukkit.ChatColor;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpectatorItemEvents extends ListenerAdapter {

	public SpectatorItemEvents(Main plugin) {
		super (plugin);
	}

	@EventHandler
	public void onSpectatorItemClick(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() != Action.PHYSICAL) {
			if (!arenaRegistry.isInArena(e.getPlayer())) {
				return;
			}

			ItemStack stack = e.getPlayer().getInventory().getItem(e.getPlayer().getInventory().getHeldItemSlot());

			if (!ItemUtils.isNamed(stack)) {
				return;
			}

			e.setCancelled(true);

			if (stack.getItemMeta().getDisplayName().equalsIgnoreCase(chatManager.message("In-Game.Spectator.Spectator-Item-Name"))) {
				openSpectatorMenu(e.getPlayer().getWorld(), e.getPlayer());
			} else if (stack.getItemMeta().getDisplayName().equalsIgnoreCase(chatManager.message("In-Game.Spectator.Settings-Menu.Item-Name"))) {
				new SpectatorSettingsMenu(e.getPlayer(), plugin).openInventory();
			}
		}
	}

	private void openSpectatorMenu(World world, Player p) {
		Set<Player> players = arenaRegistry.getArena(p).getPlayers();
		Inventory inventory = plugin.getServer().createInventory(null, NumberUtils.roundInteger(players.size(), 9), chatManager.message("In-Game.Spectator.Spectator-Menu-Name"));

		for (Player player : world.getPlayers()) {
			if (players.contains(player) && !userManager.getUser(player).isSpectator()) {
				ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
				SkullMeta meta = (SkullMeta) skull.getItemMeta();
				meta = ItemUtils.setPlayerHead(player, meta);
				meta.setDisplayName(player.getName());

				String score = chatManager.message("In-Game.Spectator.Target-Player-Score", p).replace("%score%", String.valueOf(arenaRegistry.getArena(p).getScoreboardManager().getRank(p)));

				meta.setLore(Collections.singletonList(score));
				skull.setDurability((short) SkullType.PLAYER.ordinal());
				skull.setItemMeta(meta);
				inventory.addItem(skull);
			}
		}

		p.openInventory(inventory);
	}

	@EventHandler
	public void onSpectatorInventoryClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();

		if (!arenaRegistry.isInArena(p)) {
			return;
		}

		Arena arena = arenaRegistry.getArena(p);

		if (!ItemUtils.isNamed(e.getCurrentItem())) {
			return;
		}

		if (!e.getView().getTitle().equalsIgnoreCase(chatManager.message("In-Game.Spectator.Spectator-Menu-Name", p))) {
			return;
		}

		e.setCancelled(true);
		ItemMeta meta = e.getCurrentItem().getItemMeta();

		for (Player player : arena.getPlayers()) {
			if (player.getName().equalsIgnoreCase(meta.getDisplayName()) || ChatColor.stripColor(meta.getDisplayName()).contains(player.getName())) {
				p.sendMessage(chatManager.formatMessage(arena, chatManager.message("Commands.Admin-Commands.Teleported-To-Player"), player));
				p.teleport(player);
				p.closeInventory();
				return;
			}
		}

		p.sendMessage(chatManager.message("Commands.Admin-Commands.Player-Not-Found"));
	}
}