package me.despical.oitc.events;

import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.items.GameItem;
import me.despical.oitc.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class GameItemEvents extends ListenerAdapter {

	private final Set<User> leaveConfirmations;

	public GameItemEvents(Main plugin) {
		super(plugin);
		this.leaveConfirmations = new HashSet<>();
	}

	@EventHandler
	public void onLeaveItemClicked(final PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) return;

		final User user = plugin.getUserManager().getUser(event.getPlayer());
		final Arena arena = user.getArena();

		if (arena == null) return;
		if (event.getItem() == null) return;

		final GameItem leaveItem = plugin.getGameItemManager().getGameItem("leave-item");

		if (leaveItem == null || leaveItem.checkAction(event.getAction())) return;
		if (!leaveItem.equals(event.getItem())) return;

		final Player player = user.getPlayer();

		if (plugin.getOption(ConfigPreferences.Option.INSTANT_LEAVE)) {
			this.leaveArena(player, arena);
			return;
		}

		if (leaveConfirmations.contains(user)) {
			this.leaveConfirmations.remove(user);

			player.sendMessage(chatManager.message("in_game.game_items.leave_item.teleport_cancelled"));
		} else {
			player.sendMessage(chatManager.message("in_game.game_items.leave_item.returning_lobby"));

			this.leaveConfirmations.add(user);

			new BukkitRunnable() {

				int ticks = 0;

				@Override
				public void run() {
					if (!leaveConfirmations.contains(user)) {
						cancel();
						return;
					}

					if ((ticks += 2) == 60) {
						cancel();
						leaveArena(player, arena);

						leaveConfirmations.remove(user);
					}
				}
			}.runTaskTimer(plugin, 0, 2);
		}
	}

	private void leaveArena(Player player, Arena arena) {
		if (plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			plugin.getBungeeManager().connectToHub(player);
		} else {
			ArenaManager.leaveAttempt(player, arena);
		}
	}

	@EventHandler
	public void onForceStartItemClicked(final PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) return;

		final User user = plugin.getUserManager().getUser(event.getPlayer());
		final Arena arena = user.getArena();

		if (arena == null) return;
		if (event.getItem() == null) return;

		final GameItem forceStartItem = plugin.getGameItemManager().getGameItem("force-start-item");

		if (forceStartItem == null || forceStartItem.checkAction(event.getAction())) return;
		if (!forceStartItem.equals(event.getItem())) return;

		if (arena.getPlayers().size() < 2) {
			arena.broadcastMessage(chatManager.formatMessage(arena, "in_game.messages.lobby_messages.waiting_for_players"));
			return;
		}

		if (arena.isForceStart()) {
			user.getPlayer().sendMessage(chatManager.message("in_game.messages.already-force-start"));
			return;
		}

		if (arena.isArenaState(ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING)) {
			arena.setArenaState(ArenaState.STARTING);
			arena.setTimer(0);
			arena.setForceStart(true);
			arena.broadcastMessage(chatManager.prefixedMessage("in_game.messages.admin_messages.set_starting_in_to_0"));
		}
	}

	@EventHandler
	public void onPlayAgain(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) return;

		ItemStack itemStack = event.getItem();
		Player player = event.getPlayer();
		Arena currentArena = arenaRegistry.getArena(player);

		if (currentArena == null) return;
		if (event.getItem() == null) return;

		final GameItem playAgainItem = plugin.getGameItemManager().getGameItem("play-again");

		if (playAgainItem == null || playAgainItem.checkAction(event.getAction())) return;
		if (!playAgainItem.equals(event.getItem())) return;

		event.setCancelled(true);

		ArenaManager.leaveAttempt(player, currentArena);

		Map<Arena, Integer> arenas = new HashMap<>();

		for (Arena arena : arenaRegistry.getArenas()) {
			if (arena.isArenaState(ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING) && arena.getPlayers().size() < 2) {
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

		player.sendMessage(chatManager.prefixedMessage("commands.no_free_arenas"));
	}
}