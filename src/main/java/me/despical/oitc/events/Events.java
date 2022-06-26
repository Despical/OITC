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

package me.despical.oitc.events;

import me.despical.commons.compat.Titles;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemUtils;
import me.despical.commons.util.Collections;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.items.SpecialItemManager;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import me.despical.oitc.util.ItemPosition;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Events implements Listener {

	private final Main plugin;

	public Events(Main plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onCommandExecute(PlayerCommandPreprocessEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BLOCK_COMMANDS)) {
			return;
		}

		for (String msg : plugin.getConfig().getStringList("Whitelisted-Commands")) {
			if (event.getMessage().contains(msg)) {
				return;
			}
		}

		if (event.getPlayer().isOp() || event.getPlayer().hasPermission("oitc.admin")) {
			return;
		}

		if (Collections.contains(event.getMessage(), "/oneinthechamber", "/oitc", "leave", "stats")) {
			return;
		}

		event.setCancelled(true);
		event.getPlayer().sendMessage(plugin.getChatManager().prefixedMessage("In-Game.Only-Command-Ingame-Is-Leave"));
	}

	@EventHandler
	public void onInGameInteract(PlayerInteractEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer()) || event.getClickedBlock() == null) {
			return;
		}

		event.setCancelled(Collections.contains(event.getClickedBlock().getType(), XMaterial.PAINTING.parseMaterial(), XMaterial.FLOWER_POT.parseMaterial()));
	}

	@EventHandler
	public void onInGameBedEnter(PlayerBedEnterEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onLeave(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
			return;
		}

		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);
		ItemStack itemStack = player.getInventory().getItemInMainHand();

		if (arena == null || !ItemUtils.isNamed(itemStack)) {
			return;
		}

		String key = SpecialItemManager.getRelatedSpecialItem(itemStack);

		if (key == null) {
			return;
		}

		if (SpecialItemManager.getRelatedSpecialItem(itemStack).equals("Leave")) {
			event.setCancelled(true);

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getBungeeManager().connectToHub(player);
			} else {
				ArenaManager.leaveAttempt(player, arena);
				player.sendMessage(plugin.getChatManager().prefixedMessage("commands.teleported_to_the_lobby", player));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayAgain(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		ItemStack itemStack = event.getItem();
		Player player = event.getPlayer();
		Arena currentArena = ArenaRegistry.getArena(player);

		if (currentArena == null || !ItemUtils.isNamed(itemStack)) {
			return;
		}

		String key = SpecialItemManager.getRelatedSpecialItem(itemStack);

		if (key == null) {
			return;
		}

		if (SpecialItemManager.getRelatedSpecialItem(itemStack).equals("Play-Again")) {
			event.setCancelled(true);

			ArenaManager.leaveAttempt(player, currentArena);

			Map<Arena, Integer> arenas = new HashMap<>();

			for (Arena arena : ArenaRegistry.getArenas()) {
				if ((arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == ArenaState.STARTING) && arena.getPlayers().size() < 2) {
					arenas.put(arena, arena.getPlayers().size());
				}
			}

			if (!arenas.isEmpty()) {
				Stream<Map.Entry<Arena, Integer>> sorted = arenas.entrySet().stream().sorted(Map.Entry.comparingByValue());
				Arena arena = sorted.findFirst().get().getKey();

				if (arena != null) {
					ArenaManager.joinAttempt(player, arena);
					return;
				}
			}

			player.sendMessage(plugin.getChatManager().prefixedMessage("commands.no_free_arenas"));
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER && ArenaRegistry.isInArena((Player) event.getEntity())) {
			event.setFoodLevel(20);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBuild(BlockPlaceEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onHangingBreakEvent(HangingBreakByEntityEvent event) {
		if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting) {
			if (event.getRemover() instanceof Player && ArenaRegistry.isInArena((Player) event.getRemover())) {
				event.setCancelled(true);
				return;
			}

			if (!(event.getRemover() instanceof Arrow)) {
				return;
			}

			Arrow arrow = (Arrow) event.getRemover();

			if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onDamageEntity(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player && e.getDamager() instanceof Arrow)) {
			return;
		}

		Arrow arrow = (Arrow) e.getDamager();

		if (!(arrow.getShooter() instanceof Player)) {
			return;
		}

		if (ArenaRegistry.isInArena((Player) e.getEntity()) && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
			if (!e.getEntity().getName().equals(((Player) arrow.getShooter()).getName())) {
				e.setDamage(100.0);
				plugin.getRewardsFactory().performReward((Player) e.getEntity(), Reward.RewardType.DEATH);
			} else {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent e) {
		Player victim = e.getEntity();
		Arena arena = ArenaRegistry.getArena(e.getEntity());

		if (arena == null) {
			return;
		}

		e.setDeathMessage("");
		e.getDrops().clear();
		e.setDroppedExp(0);
		e.getEntity().getLocation().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
		e.getEntity().playEffect(org.bukkit.EntityEffect.HURT);

		Titles.sendTitle(victim.getKiller(), 5, 30, 5, null, plugin.getChatManager().message("in_game.messages.score_subtitle"));

		User victimUser = plugin.getUserManager().getUser(victim);
		victimUser.setStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 0);
		victimUser.addStat(StatsStorage.StatisticType.LOCAL_DEATHS, 1);
		victimUser.addStat(StatsStorage.StatisticType.DEATHS, 1);

		User killerUser = plugin.getUserManager().getUser(victim.getKiller());
		killerUser.addStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 1);
		killerUser.addStat(StatsStorage.StatisticType.LOCAL_KILLS, 1);
		killerUser.addStat(StatsStorage.StatisticType.KILLS, 1);

		if (killerUser.getStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK) == 1){
			arena.broadcastMessage(plugin.getChatManager().prefixedFormattedMessage(arena, plugin.getChatManager().message("in_game.messages.death").replace("%killer%", victim.getKiller().getName()), victim));
		} else {
			arena.broadcastMessage(plugin.getChatManager().prefixedFormattedMessage(arena, plugin.getChatManager().message("in_game.messages.kill_streak").replace("%kill_streak%", Integer.toString(killerUser.getStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK))).replace("%killer%", victim.getKiller().getName()), victim));
		}

		ItemPosition.addItem(victim.getKiller(), ItemPosition.ARROW, ItemPosition.ARROW.getItem());
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> victim.spigot().respawn(), 5);

		plugin.getRewardsFactory().performReward(victim.getKiller(), Reward.RewardType.KILL);

		if (StatsStorage.getUserStats(victim.getKiller(), StatsStorage.StatisticType.LOCAL_KILLS) == plugin.getConfig().getInt("winning_score", 25)) {
			ArenaManager.stopGame(false, arena);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) return;

		event.setRespawnLocation(arena.getRandomSpawnPoint());

		Titles.sendTitle(player, null, plugin.getChatManager().message("in_game.messages.death_subtitle"));

		ItemPosition.giveKit(player);
	}

	@EventHandler
	public void onInteractWithArmorStand(PlayerArmorStandManipulateEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onItemMove(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			if (ArenaRegistry.isInArena((Player) event.getWhoClicked())) {
				event.setResult(Event.Result.DENY);
			}
		}
	}

	@EventHandler
	public void playerCommandExecution(PlayerCommandPreprocessEvent e) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.ENABLE_SHORT_COMMANDS)) {
			Player player = e.getPlayer();

			if (e.getMessage().equalsIgnoreCase("/start")) {
				player.performCommand("oitc forcestart");
				e.setCancelled(true);
				return;
			}

			if (e.getMessage().equalsIgnoreCase("/leave")) {
				player.performCommand("oitc leave");
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onFallDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}

		Player victim = (Player) e.getEntity();

		if (!ArenaRegistry.isInArena(victim)) {
			return;
		}

		if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
			if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_FALL_DAMAGE)) {
				return;
			}

			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onArrowPickup(PlayerPickupArrowEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.getItem().remove();
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPickupItem(PlayerPickupItemEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		event.setCancelled(true);
		event.getItem().remove();
	}
}