package me.despical.oitc.commands.game;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.commands.exception.CommandException;
import me.despical.oitc.utils.Debugger;
import me.despical.oitc.utils.Utils;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class LeaveCommand extends SubCommand {

	public LeaveCommand() {
		super("leave");
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
	public void execute(CommandSender sender, String label, String[] args) throws CommandException {
		if (!getPlugin().getConfig().getBoolean("Disable-Leave-Command", false)) {
			Player player = (Player) sender;
			if (!Utils.checkIsInGameInstance((Player) sender)) {
				return;
			}
			player.sendMessage(getPlugin().getChatManager().getPrefix() + getPlugin().getChatManager().colorMessage("Commands.Teleported-To-The-Lobby", player));
			if (getPlugin().getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				getPlugin().getBungeeManager().connectToHub(player);
				Debugger.debug(Level.INFO, "{0} was teleported to the Hub server", player.getName());
				return;
			}
			Arena arena = ArenaRegistry.getArena(player);
			ArenaManager.leaveAttempt(player, arena);
			Debugger.debug(Level.INFO, "{0} has left the arena {1}! Teleported to end location.", player.getName(), arena.getId());
		}
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