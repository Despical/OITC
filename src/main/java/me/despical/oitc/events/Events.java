package me.despical.oitc.events;

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.item.ItemUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.items.SpecialItemManager;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.utils.ItemPosition;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

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

	@EventHandler(priority = EventPriority.HIGH)
	public void onItemSwap(PlayerSwapHandItemsEvent e) {
		if (ArenaRegistry.isInArena(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onDrop(PlayerDropItemEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCommandExecute(PlayerCommandPreprocessEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null) {
			return;
		}

		if (!plugin.getConfig().getBoolean("Block-Commands-In-Game", true)) {
			return;
		}

		for (String msg : plugin.getConfig().getStringList("Whitelisted-Commands")) {
			if (event.getMessage().contains(msg)) {
				return;
			}
		}

		if (event.getPlayer().isOp() || event.getPlayer().hasPermission("oitc.admin") || event.getPlayer().hasPermission("oitc.command.bypass")) {
			return;
		}

		if (event.getMessage().startsWith("/oneinthechamber") || event.getMessage().startsWith("/oitc") || event.getMessage().contains("leave") || event.getMessage().contains("stats")) {
			return;
		}

		event.setCancelled(true);
		event.getPlayer().sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Only-Command-Ingame-Is-Leave"));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInGameInteract(PlayerInteractEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null || event.getClickedBlock() == null) {
			return;
		}

		if (event.getClickedBlock().getType() == XMaterial.PAINTING.parseMaterial() || event.getClickedBlock().getType() == XMaterial.FLOWER_POT.parseMaterial()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInGameBedEnter(PlayerBedEnterEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onLeave(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
			return;

		}

		Arena arena = ArenaRegistry.getArena(event.getPlayer());
		ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();

		if (arena == null || !ItemUtils.isNamed(itemStack)) {
			return;
		}

		String key = SpecialItemManager.getRelatedSpecialItem(itemStack);

		if (key == null) {
			return;
		}

		if (SpecialItemManager.getRelatedSpecialItem(itemStack).equalsIgnoreCase("Leave")) {
			event.setCancelled(true);

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getBungeeManager().connectToHub(event.getPlayer());
			} else {
				ArenaManager.leaveAttempt(event.getPlayer(), arena);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER && ArenaRegistry.isInArena((Player) event.getEntity())) {
			event.setFoodLevel(20);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBuild(BlockPlaceEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH)
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

		if (arrow.getShooter() instanceof Player) {
			if (ArenaRegistry.isInArena((Player) e.getEntity()) && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
				if (!e.getEntity().getName().equals(((Player) arrow.getShooter()).getName())) {
					e.setDamage(100.0);
					plugin.getRewardsFactory().performReward((Player) e.getEntity(), Reward.RewardType.DEATH);
				}
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

		victim.getKiller().sendTitle("", plugin.getChatManager().colorMessage("In-Game.Messages.Score-Subtitle"), 5, 30, 5);

		plugin.getUserManager().getUser(victim).setStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 0);
		plugin.getUserManager().getUser(victim).addStat(StatsStorage.StatisticType.LOCAL_DEATHS, 1);
		plugin.getUserManager().getUser(victim).addStat(StatsStorage.StatisticType.DEATHS, 1);
		plugin.getUserManager().getUser(victim.getKiller()).addStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK, 1);
		plugin.getUserManager().getUser(victim.getKiller()).addStat(StatsStorage.StatisticType.LOCAL_KILLS, 1);
		plugin.getUserManager().getUser(victim.getKiller()).addStat(StatsStorage.StatisticType.KILLS, 1);

		if (plugin.getUserManager().getUser(victim.getKiller()).getStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK) == 1){
			plugin.getChatManager().broadcast(arena, plugin.getChatManager().formatMessage(arena, plugin.getChatManager().colorMessage("In-Game.Messages.Death").replace("%killer%", victim.getKiller().getName()), victim));
		} else {
			plugin.getChatManager().broadcast(arena, plugin.getChatManager().formatMessage(arena, plugin.getChatManager().colorMessage("In-Game.Messages.Kill-Streak").replace("%kill_streak%", String.valueOf(plugin.getUserManager().getUser(victim.getKiller()).getStat(StatsStorage.StatisticType.LOCAL_KILL_STREAK))).replace("%killer%", victim.getKiller().getName()), victim));
		}

		ItemPosition.addItem(victim.getKiller(), ItemPosition.ARROW, new ItemStack(Material.ARROW, 1));
		Bukkit.getScheduler().runTaskLater(plugin, () -> victim.spigot().respawn(), 5);

		plugin.getRewardsFactory().performReward(victim.getKiller(), Reward.RewardType.KILL);

		if (StatsStorage.getUserStats(victim.getKiller(), StatsStorage.StatisticType.LOCAL_KILLS) == plugin.getConfig().getInt("Winning-Score", 25)) {
			ArenaManager.stopGame(false, arena);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		if (!ArenaRegistry.isInArena(player)) {
			return;
		}

		event.getPlayer().sendTitle(null, plugin.getChatManager().colorMessage("In-Game.Messages.Death-Subtitle"), 5, 30, 5);
		event.setRespawnLocation(ArenaRegistry.getArena(player).getRandomSpawnPoint());

		ItemPosition.setItem(event.getPlayer(), ItemPosition.SWORD, new ItemBuilder(XMaterial.WOODEN_SWORD.parseItem()).unbreakable(true).amount(1).build());
		ItemPosition.setItem(event.getPlayer(), ItemPosition.BOW, new ItemBuilder(XMaterial.BOW.parseItem()).enchantment(Enchantment.LUCK).flag(ItemFlag.HIDE_ENCHANTS).unbreakable(true).amount(1).build());
		ItemPosition.setItem(event.getPlayer(), ItemPosition.ARROW, new ItemStack(Material.ARROW, 1));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onArmorStandDestroy(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof LivingEntity)) {
			return;
		}

		LivingEntity livingEntity = (LivingEntity) e.getEntity();

		if (!livingEntity.getType().equals(EntityType.ARMOR_STAND)) {
			return;
		}

		if (e.getDamager() instanceof Player && ArenaRegistry.isInArena((Player) e.getDamager())) {
			e.setCancelled(true);
		} else if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();

			if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
				e.setCancelled(true);
				return;
			}

			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteractWithArmorStand(PlayerArmorStandManipulateEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemMove(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (ArenaRegistry.getArena((Player) e.getWhoClicked()) != null) {
				e.setResult(Event.Result.DENY);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
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
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onFallDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}

		Player victim = (Player) e.getEntity();
		Arena arena = ArenaRegistry.getArena(victim);

		if (arena == null) {
			return;
		}

		if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
			if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_FALL_DAMAGE)) {
				return;
			}

			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onArrowPickup(PlayerPickupArrowEvent e) {
		if (ArenaRegistry.isInArena(e.getPlayer())) {
			e.getItem().remove();
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPickupItem(PlayerPickupItemEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null) {
			return;
		}

		event.setCancelled(true);
		event.getItem().remove();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCraft(PlayerInteractEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		if (event.getPlayer().getTargetBlock(null, 7).getType() == XMaterial.CRAFTING_TABLE.parseMaterial()) {
			event.setCancelled(true);
		}
	}
}