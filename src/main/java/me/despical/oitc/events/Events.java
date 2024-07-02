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

package me.despical.oitc.events;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commons.compat.Titles;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.commons.reflection.XReflection;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.Collections;
import me.despical.commons.util.UpdateChecker;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import me.despical.oitc.util.ItemPosition;
import me.despical.oitc.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;

import java.util.regex.Pattern;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Events extends ListenerAdapter {

	public Events(Main plugin) {
		super(plugin);

		registerLegacyEvents();
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (!plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED) || e.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
			return;
		}

		if (e.getPlayer().hasPermission(plugin.getPermissionsManager().getJoinPerm())) {
			e.setResult(PlayerLoginEvent.Result.ALLOWED);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player eventPlayer = event.getPlayer();
		userManager.loadStatistics(eventPlayer);

		if (plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			arenaRegistry.getBungeeArena().teleportToLobby(eventPlayer);
			return;
		}

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (!arenaRegistry.isInArena(player)) {
				continue;
			}

			PlayerUtils.hidePlayer(player, eventPlayer, plugin);
			PlayerUtils.hidePlayer(eventPlayer, player, plugin);
		}

		if (plugin.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, eventPlayer);
		}
	}

	@EventHandler
	public void onJoinCheckVersion(PlayerJoinEvent event) {
		if (!plugin.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED) || !event.getPlayer().hasPermission("oitc.updatenotify")) {
			return;
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 81185).requestUpdateCheck().whenComplete((result, exception) -> {
			if (result.requiresUpdate()) {
				final Player player = event.getPlayer();

				player.sendMessage(chatManager.coloredRawMessage("&3[OITC] &bFound an update: v" + result.getNewestVersion() + " Download:"));
				player.sendMessage(chatManager.coloredRawMessage("&3>> &bhttps://spigotmc.org/resources/81185"));
			}
		}), 25);
	}

	@EventHandler
	public void onLobbyDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		final Player player = (Player) event.getEntity();
		final Arena arena = arenaRegistry.getArena(player);

		if (arena == null || arena.getArenaState() == ArenaState.IN_GAME) {
			return;
		}

		event.setCancelled(true);
		player.setFireTicks(0);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		handleQuit(event.getPlayer());
	}

	@EventHandler
	public void onKick(PlayerKickEvent event) {
		handleQuit(event.getPlayer());
	}

	private void handleQuit(Player player) {
		final Arena arena = arenaRegistry.getArena(player);

		if (arena != null) {
			ArenaManager.leaveAttempt(player, arena, true);
		}

		userManager.removeUser(player);
	}

	@EventHandler
	public void onCommandExecute(PlayerCommandPreprocessEvent event) {
		if (!arenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		if (!plugin.getOption(ConfigPreferences.Option.BLOCK_COMMANDS)) {
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
		event.getPlayer().sendMessage(chatManager.prefixedMessage("In-Game.Only-Command-Ingame-Is-Leave"));
	}

	@EventHandler
	public void onInGameInteract(PlayerInteractEvent event) {
		if (!arenaRegistry.isInArena(event.getPlayer()) || event.getClickedBlock() == null) {
			return;
		}

		event.setCancelled(Collections.contains(event.getClickedBlock().getType(), XMaterial.PAINTING.parseMaterial(), XMaterial.FLOWER_POT.parseMaterial()));
	}

	@EventHandler
	public void onInGameBedEnter(PlayerBedEnterEvent event) {
		if (arenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER && arenaRegistry.isInArena((Player) event.getEntity())) {
			event.setFoodLevel(20);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (arenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBuild(BlockPlaceEvent event) {
		if (arenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onHangingBreakEvent(HangingBreakByEntityEvent event) {
		if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting) {
			if (event.getRemover() instanceof Player && arenaRegistry.isInArena((Player) event.getRemover())) {
				event.setCancelled(true);
				return;
			}

			if (!(event.getRemover() instanceof Arrow)) {
				return;
			}

			Arrow arrow = (Arrow) event.getRemover();

			if (arrow.getShooter() instanceof Player && arenaRegistry.isInArena((Player) arrow.getShooter())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDamageEntity(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player && e.getDamager() instanceof Player)) return;

		Player damager = (Player) e.getDamager();
		Player player = (Player) e.getEntity();

		if (arenaRegistry.isInArena(player) && arenaRegistry.isInArena(damager)) {
			User user = plugin.getUserManager().getUser(player);

			if (user.getCooldown("respawn_protection") > 0) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onArrowDamage(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player && e.getDamager() instanceof Arrow)) return;

		Arrow arrow = (Arrow) e.getDamager();

		if (!(arrow.getShooter() instanceof Player)) return;

		Player shooter = (Player) arrow.getShooter();
		Player player = (Player) e.getEntity();

		if (arenaRegistry.isInArena(player) && arenaRegistry.isInArena(shooter)) {
			if (!player.getUniqueId().equals(shooter.getUniqueId())) {
				User user = plugin.getUserManager().getUser(player);

				if (user.getCooldown("respawn_protection") > 0) {
					e.setCancelled(true);
					return;
				}

				e.setDamage(100.0);
			} else {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player victim = e.getEntity();
		Arena arena = arenaRegistry.getArena(e.getEntity());

		if (arena == null) {
			return;
		}

		e.setDeathMessage("");
		e.getDrops().clear();
		e.setDroppedExp(0);
		e.getEntity().getLocation().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
		e.getEntity().playEffect(org.bukkit.EntityEffect.HURT);

		User victimUser = userManager.getUser(victim);
		victimUser.setStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 0);
		victimUser.addStat(StatsStorage.StatisticType.LOCAL_DEATHS, 1);
		victimUser.addStat(StatsStorage.StatisticType.DEATHS, 1);
		victimUser.performReward(Reward.RewardType.DEATH);
		victimUser.setCooldown("respawn_protection", plugin.getConfig().getInt("Respawn-Protection.Invulnerable"));

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> victim.spigot().respawn(), 5);

		Player killer = victim.getKiller();

		if (killer == null) {
			arena.broadcastMessage(chatManager.prefixedMessage("in_game.messages.unknown_death").replace("%player%", victim.getName()));
			return;
		}

		User killerUser = userManager.getUser(victim.getKiller());
		killerUser.sendTitle("", chatManager.message("in_game.messages.score_subtitle"));
		killerUser.addStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 1);
		killerUser.addStat(StatsStorage.StatisticType.LOCAL_KILLS, 1);
		killerUser.addStat(StatsStorage.StatisticType.KILLS, 1);
		killerUser.performReward(Reward.RewardType.KILL);

		if (plugin.getOption(ConfigPreferences.Option.HEAL_ON_KILL)) {
			AttributeUtils.healPlayer(killer);
		}

		int killStreak = killerUser.getStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK);

		if (killStreak == 1) {
			arena.broadcastMessage(chatManager.prefixedFormattedMessage(arena, chatManager.message("in_game.messages.death").replace("%killer%", victim.getKiller().getName()), victim));
		} else {
			arena.broadcastMessage(chatManager.prefixedFormattedMessage(arena, chatManager.getStreakMessage(killStreak).replace("%killer%", victim.getKiller().getName()), victim));
		}

		ItemPosition.addItem(killer, ItemPosition.ARROW, ItemPosition.ARROW.getItem());

		if (StatsStorage.getUserStats(killer, StatsStorage.StatisticType.LOCAL_KILLS) == plugin.getConfig().getInt("Winning-Score", 25)) {
			ArenaManager.stopGame(false, arena);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Arena arena = arenaRegistry.getArena(player);

		if (arena == null) return;

		event.setRespawnLocation(arena.getRandomSpawnPoint());

		int invisibilityDuration = plugin.getConfig().getInt("Respawn-Protection.Invisibility");

		if (invisibilityDuration != 0) {
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> Utils.addPotionEffect(player, PotionEffectType.INVISIBILITY, invisibilityDuration, 0), 1);
		}

		Titles.sendTitle(player, 10, 40, 10, "", chatManager.message("in_game.messages.death_subtitle"));

		ItemPosition.giveKit(player);
	}

	@EventHandler
	public void onHealthRegen(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player)) return;

		if (!arenaRegistry.isInArena((Player) event.getEntity())) return;

		if (!plugin.getOption(ConfigPreferences.Option.REGEN_ENABLED)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteractWithArmorStand(PlayerArmorStandManipulateEvent event) {
		if (arenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onItemMove(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) {
			return;
		}

		Player player = (Player) e.getWhoClicked();

		if (userManager.getUser(player).getArena() == null) return;

		if (e.getView().getType() == InventoryType.CRAFTING || e.getView().getType() == InventoryType.PLAYER) {
			e.setResult(Event.Result.DENY);
		}
	}

	@EventHandler
	public void playerCommandExecution(PlayerCommandPreprocessEvent e) {
		if (plugin.getOption(ConfigPreferences.Option.ENABLE_SHORT_COMMANDS)) {
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
	public void onFallDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player victim = (Player) event.getEntity();

		if (!arenaRegistry.isInArena(victim)) {
			return;
		}

		if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
			if (plugin.getOption(ConfigPreferences.Option.DISABLE_FALL_DAMAGE)) {
				return;
			}

			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPickupItem(PlayerPickupItemEvent event) {
		if (plugin.getOption(ConfigPreferences.Option.ENABLE_ARROW_PICKUPS)) return;

		if (!arenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		event.setCancelled(true);
		event.getItem().remove();
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (arenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onChatInGame(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Arena arena = arenaRegistry.getArena(player);

		if (arena == null) {
			if (!plugin.getOption(ConfigPreferences.Option.DISABLE_SEPARATE_CHAT)) {
				arenaRegistry.getArenas().forEach(loopArena -> loopArena.getPlayers().forEach(p -> event.getRecipients().remove(p)));
			}

			return;
		}

		if (plugin.getOption(ConfigPreferences.Option.CHAT_FORMAT_ENABLED)) {
			String message = formatChatPlaceholders(chatManager.message("In-Game.Game-Chat-Format"), player, event.getMessage().replaceAll(Pattern.quote("[$\\]"), ""));

			if (!plugin.getOption(ConfigPreferences.Option.DISABLE_SEPARATE_CHAT)) {
				event.setCancelled(true);

				final boolean dead = userManager.getUser(player).isSpectator();

				for (Player p : arena.getPlayers()) {
					if (dead && arena.getPlayersLeft().contains(p)) {
						continue;
					}

					p.sendMessage(dead ? formatChatPlaceholders(chatManager.message("In-Game.Game-Death-Format"), player, null) + message : message);
				}

				plugin.getServer().getConsoleSender().sendMessage(message);
			} else {
				event.setMessage(message);
			}
		}
	}

	private String formatChatPlaceholders(String message, Player player, String saidMessage) {
		String formatted = message;

		formatted = formatted.replace("%player%", player.getName());
		formatted = formatted.replace("%message%", ChatColor.stripColor(saidMessage));

		if (chatManager.isPapiEnabled()) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return chatManager.coloredRawMessage(formatted);
	}

	private void registerLegacyEvents() {
		registerIf(XReflection.supports(9) && XReflection.supportsPatch(2), new Listener() {

			@EventHandler
			public void onItemSwap(PlayerSwapHandItemsEvent event) {
				if (arenaRegistry.isInArena(event.getPlayer())) {
					event.setCancelled(true);
				}
			}
		});

		registerIf(XReflection.supports(9), new Listener() {

			@EventHandler
			public void onArrowPickup(PlayerPickupArrowEvent event) {
				if (plugin.getOption(ConfigPreferences.Option.ENABLE_ARROW_PICKUPS)) return;

				if (arenaRegistry.isInArena(event.getPlayer())) {
					event.getItem().remove();
					event.setCancelled(true);
				}
			}
		});
	}
}