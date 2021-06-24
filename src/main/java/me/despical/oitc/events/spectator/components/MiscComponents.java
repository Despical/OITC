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

package me.despical.oitc.events.spectator.components;

import me.despical.commons.item.ItemBuilder;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.events.spectator.SpectatorSettingsMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 03.10.2020
 */
public class MiscComponents implements SpectatorSettingComponent {

	@Override
	public void registerComponent(SpectatorSettingsMenu spectatorSettingsMenu, StaticPane pane) {
		Player player = spectatorSettingsMenu.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);
		ItemStack nightVision = player.hasPotionEffect(PotionEffectType.NIGHT_VISION) ?
			new ItemBuilder(Material.ENDER_PEARL)
				.name(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Disable-Night-Vision"))
				.lore(plugin.getChatManager().getStringList("In-Game.Spectator.Settings-Menu.Disable-Night-Vision-Lore"))
				.build()

			:

			new ItemBuilder(Material.ENDER_EYE)
				.name(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Enable-Night-Vision"))
				.lore(plugin.getChatManager().getStringList("In-Game.Spectator.Settings-Menu.Enable-Night-Vision-Lore"))
				.build();

		pane.addItem(new GuiItem(nightVision, e -> {
			if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
			}
		}), 2, 2);

		List<Player> spectators = arena.getPlayers().stream().filter(p -> plugin.getUserManager().getUser(p).isSpectator()).collect(Collectors.toList());
		boolean canSee = spectators.stream().anyMatch(player::canSee);

		ItemStack specItem = canSee ?
			new ItemBuilder(Material.REDSTONE)
			.name(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Hide-Spectators"))
			.lore(plugin.getChatManager().getStringList("In-Game.Spectator.Settings-Menu.Hide-Spectators-Lore"))
			.build()

			:

			new ItemBuilder(Material.GLOWSTONE_DUST)
			.name(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Show-Spectators"))
			.lore(plugin.getChatManager().getStringList("In-Game.Spectator.Settings-Menu.Show-Spectators-Lore"))
			.build();

		pane.addItem(new GuiItem(specItem, e -> {
			if (canSee) {
				spectators.forEach(p -> PlayerUtils.hidePlayer(player, p, plugin));
				player.sendMessage(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Show-Spectators-Message"));
			} else {
				spectators.forEach(p -> PlayerUtils.showPlayer(player, p, plugin));
				player.sendMessage(plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Hide-Spectators-Message"));
			}
		}), 3, 2);
	}
}
