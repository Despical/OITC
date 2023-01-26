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

package me.despical.oitc.handlers.sign;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.BlockUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SignManager implements Listener {

	private final Main plugin;
	private final Set<ArenaSign> arenaSigns;
	private final List<String> signLines;
	private final Map<ArenaState, String> gameStateToString;
	private final FileConfiguration config;

	public SignManager(Main plugin) {
		this.plugin = plugin;
		this.arenaSigns = new HashSet<>();
		this.signLines = plugin.getChatManager().getStringList("Signs.Lines");
		this.gameStateToString = new EnumMap<>(ArenaState.class);
		this.config = ConfigUtils.getConfig(plugin, "arenas");

		for (ArenaState state : ArenaState.values()) {
			gameStateToString.put(state, plugin.getChatManager().message("Signs.Game-States." + state.getDefaultName()));
		}

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();

		if (!player.hasPermission("oitc.admin.sign.create") || !event.getLine(0).equalsIgnoreCase("[oitc]")) {
			return;
		}

		if (event.getLine(1).isEmpty()) {
			player.sendMessage(plugin.getChatManager().prefixedMessage("Signs.Please-Type-Arena-Name"));
			return;
		}

		Arena arena = ArenaRegistry.getArena(event.getLine(1));

		if (arena == null) {
			player.sendMessage(plugin.getChatManager().prefixedMessage("Signs.Arena-Doesnt-Exists"));
			return;
		}

		arenaSigns.add(new ArenaSign((Sign) event.getBlock().getState(), arena));

		for (int i = 0; i < signLines.size(); i++) {
			event.setLine(i, formatSign(signLines.get(i), arena));
		}

		player.sendMessage(plugin.getChatManager().prefixedMessage("Signs.Sign-Created"));

		List<String> locs = config.getStringList("instances." + arena.getId() + ".signs");
		locs.add(LocationSerializer.toString(event.getBlock().getLocation()));

		config.set("instances." + arena.getId() + ".signs", locs);
		ConfigUtils.saveConfig(plugin, config, "arenas");
	}

	private String formatSign(String msg, Arena arena) {
		String formatted = msg;
		int size = arena.getPlayers().size(), max = arena.getMaximumPlayers();

		formatted = StringUtils.replace(formatted, "%map_name%", arena.getMapName());

		if (size >= max) {
			formatted = StringUtils.replace(formatted, "%state%", plugin.getChatManager().message("Signs.Game-States.Full-Game"));
		} else {
			formatted = StringUtils.replace(formatted, "%state%", gameStateToString.get(arena.getArenaState()));
		}

		formatted = StringUtils.replace(formatted, "%players%", Integer.toString(size));
		formatted = StringUtils.replace(formatted, "%max_players%", Integer.toString(max));
		return plugin.getChatManager().coloredRawMessage(formatted);
	}

	@EventHandler
	public void onSignDestroy(BlockBreakEvent event) {
		Block block = event.getBlock();
		ArenaSign arenaSign = getArenaSignByBlock(block);

		if (arenaSign == null) {
			return;
		}

		Player player = event.getPlayer();

		if (!player.hasPermission("oitc.admin.sign.break")) {
			event.setCancelled(true);
			player.sendMessage(plugin.getChatManager().prefixedMessage("Signs.Doesnt-Have-Permission"));
			return;
		}

		arenaSigns.remove(arenaSign);

		String location = LocationSerializer.toString(block.getLocation());

		for (String arena : config.getConfigurationSection("instances").getKeys(false)) {
			String path = "instances." + arena + ".signs";

			for (String sign : config.getStringList(path)) {
				if (!sign.equals(location)) {
					continue;
				}

				List<String> signs = config.getStringList(path);
				signs.remove(location);

				config.set(path, signs);
				ConfigUtils.saveConfig(plugin, config, "arenas");
				player.sendMessage(plugin.getChatManager().prefixedMessage("Signs.Sign-Removed"));
				return;
			}
		}

		player.sendMessage(plugin.getChatManager().prefixedRawMessage("&cCouldn't remove sign from configuration! Please do this manually!"));
	}

	@EventHandler
	public void onJoinAttempt(PlayerInteractEvent e) {
		ArenaSign arenaSign = getArenaSignByBlock(e.getClickedBlock());

		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && arenaSign != null) {
			Arena arena = arenaSign.getArena();

			if (arena == null) {
				return;
			}

			ArenaManager.joinAttempt(e.getPlayer(), arena);
		}
	}

	private ArenaSign getArenaSignByBlock(Block block) {
		return block == null || !(block.getState() instanceof Sign) ? null : arenaSigns.stream().filter(sign -> sign.getSign().getLocation().equals(block.getLocation())).findFirst().orElse(null);
	}

	public void loadSigns() {
		LogUtils.log("Signs load event started.");
		long start = System.currentTimeMillis();

		arenaSigns.clear();

		for (String path : config.getConfigurationSection("instances").getKeys(false)) {
			for (String sign : config.getStringList("instances." + path + ".signs")) {
				Location loc = LocationSerializer.fromString(sign);
				
				if (loc.getBlock().getState() instanceof Sign) {
					arenaSigns.add(new ArenaSign((Sign) loc.getBlock().getState(), ArenaRegistry.getArena(path)));
				} else {
					LogUtils.log(Level.WARNING, "Block at location {0} for arena {1} not a sign.", loc, path);
				}
			}
		}

		LogUtils.log("Sign load event finished took {0} ms.", System.currentTimeMillis() - start);

		updateSigns();
	}

	public void updateSigns() {
		LogUtils.log("[Sign Update] Updating signs.");
		long start = System.currentTimeMillis();

		for (ArenaSign arenaSign : arenaSigns) {
			Sign sign = arenaSign.getSign();

			for (int i = 0; i < signLines.size(); i++) {
				sign.setLine(i, formatSign(signLines.get(i), arenaSign.getArena()));
			}

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.SIGNS_BLOCK_STATES_ENABLED) && arenaSign.getBehind() != null) {
				Block behind = arenaSign.getBehind();

				try {
					switch (arenaSign.getArena().getArenaState()) {
						case WAITING_FOR_PLAYERS:
							behind.setType(XMaterial.WHITE_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 0);
							break;
						case STARTING:
							behind.setType(XMaterial.YELLOW_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 4);
							break;
						case IN_GAME:
							behind.setType(XMaterial.ORANGE_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 1);
							break;
						case ENDING:
							behind.setType(XMaterial.GRAY_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 7);
							break;
						case RESTARTING:
							behind.setType(XMaterial.BLACK_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 15);
							break;
						case INACTIVE:
							behind.setType(XMaterial.RED_STAINED_GLASS.parseMaterial());

							BlockUtils.setData(behind, (byte) 14);
							break;
						default:
							break;
					}
				} catch (Exception ignored) {}
			}

			sign.update();
		}

		LogUtils.log("[Sign Update] Updated signs, took {0} ms.", System.currentTimeMillis() - start);
	}

	public void addArenaSign(Block block, Arena arena) {
		arenaSigns.add(new ArenaSign((Sign) block.getState(), arena));
		updateSigns();
	}

	public Set<ArenaSign> getArenaSigns() {
		return new HashSet<>(arenaSigns);
	}
}