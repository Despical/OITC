package me.despical.oitc.commands.admin.arena;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.commands.exception.CommandException;
import me.despical.oitc.utils.Utils;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ForceStartCommand extends SubCommand {

	public ForceStartCommand() {
		super("forcestart");
		setPermission("oitc.admin.forcestart");
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
		if (!Utils.checkIsInGameInstance((Player) sender)) {
			return;
		}
		Arena arena = ArenaRegistry.getArena((Player) sender);
		if (arena.getPlayers().size() < 2) {
			getPlugin().getChatManager().broadcast(arena, getPlugin().getChatManager().formatMessage(arena, getPlugin().getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Waiting-For-Players"), arena.getMinimumPlayers()));
			return;
		}
		
		if (arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == ArenaState.STARTING) {
			arena.setArenaState(ArenaState.STARTING);
			arena.setForceStart(true);
			arena.setTimer(0);
			ArenaRegistry.getArena((Player) sender).getPlayers().forEach(p -> p.sendMessage(getPlugin().getChatManager().getPrefix() + getPlugin().getChatManager().colorMessage("In-Game.Messages.Admin-Messages.Set-Starting-In-To-0")));
		}
	}

	@Override
	public List<String> getTutorial() {
		return Collections.singletonList("Force start arena you're in");
	}

	@Override
	public CommandType getType() {
		return CommandType.GENERIC;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.PLAYER;
	}
}