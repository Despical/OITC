package me.despical.oitc.commands.player;

import me.despical.commons.util.Collections;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 23.06.2022
 */
public class RandomJoinCommand extends SubCommand {

	public RandomJoinCommand() {
		super ("randomjoin");
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
	public void execute(CommandSender sender, String label, String[] args) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			return;
		}

		List<Arena> arenas = ArenaRegistry.getArenas().stream().filter(arena -> Collections.contains(arena.getArenaState(), ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING)
			&& arena.getPlayers().size() < arena.getMaximumPlayers()).collect(Collectors.toList());

		if (!arenas.isEmpty()) {
			Arena arena = arenas.get(ThreadLocalRandom.current().nextInt(arenas.size()));
			ArenaManager.joinAttempt((Player) sender, arena);
			return;
		}

		sender.sendMessage(chatManager.prefixedMessage("commands.no_free_arenas"));
	}

	@Override
	public String getTutorial() {
		return null;
	}

	@Override
	public CommandType getType() {
		return CommandType.HIDDEN;
	}

	@Override
	public SubCommand.SenderType getSenderType() {
		return SubCommand.SenderType.PLAYER;
	}
}