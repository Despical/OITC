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

package me.despical.oitc.events.spectator;

import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.events.ListenerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpectatorEvents extends ListenerAdapter {

	public SpectatorEvents(Main plugin) {
		super (plugin);
	}

	@EventHandler
	public void onSpectatorTarget(EntityTargetEvent e) {
		if (!(e.getTarget() instanceof Player)) {
			return;
		}

		if (userManager.getUser((Player) e.getTarget()).isSpectator()) {
			e.setCancelled(true);
			e.setTarget(null);
		}
	}

	@EventHandler
	public void onSpectatorTarget(EntityTargetLivingEntityEvent e) {
		if (!(e.getTarget() instanceof Player)) {
			return;
		}

		if (userManager.getUser((Player) e.getTarget()).isSpectator()) {
			e.setCancelled(true);
			e.setTarget(null);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDropItem(PlayerDropItemEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEntityEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onShear(PlayerShearEntityEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (userManager.getUser(player).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (!userManager.getUser(player).isSpectator() || !ArenaRegistry.isInArena(player)) {
			return;
		}

		if (player.getLocation().getY() < 1) {
			player.teleport(ArenaRegistry.getArena(player).getRandomSpawnPoint());
		}

		event.setCancelled(true);
	}

	@EventHandler
	public void onDamageToPlayers(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player)) return;

		Player player = (Player) event.getDamager();

		if (plugin.getUserManager().getUser(player).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamageByBlock(EntityDamageByBlockEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (userManager.getUser(player).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getDamager();

		if (userManager.getUser(player).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPickupItem(PlayerPickupItemEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (userManager.getUser(event.getPlayer()).isSpectator()) {
			event.setCancelled(true);
		}
	}
}