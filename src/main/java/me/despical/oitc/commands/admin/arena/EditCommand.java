package me.despical.oitc.commands.admin.arena;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.commands.exception.CommandException;
import me.despical.oitc.handlers.setup.SetupInventory;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class EditCommand extends SubCommand {

	public EditCommand(String name) {
		super("edit");
		setPermission("oitc.admin.setup");
	}

	@Override
	public String getPossibleArguments() {
		return "<arena>";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public void execute(CommandSender sender, String label, String[] args) throws CommandException {
		if (ArenaRegistry.getArena(args[0]) == null) {
			sender.sendMessage(getPlugin().getChatManager().getPrefix() + getPlugin().getChatManager().colorMessage("Commands.No-Arena-Like-That"));
			return;
		}
		new SetupInventory(ArenaRegistry.getArena(args[0]), (Player) sender).openInventory();	
	}

	@Override
	public List<String> getTutorial() {
		return Arrays.asList("Open arena editor menu");
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