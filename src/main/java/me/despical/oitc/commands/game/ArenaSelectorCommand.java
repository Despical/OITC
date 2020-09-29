package me.despical.oitc.commands.game;

import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.commands.exception.CommandException;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ArenaSelectorCommand extends SubCommand {

	public ArenaSelectorCommand(String name) {
		super(name);
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
		// TODO: implement here
	}

	@Override
	public List<String> getTutorial() {
		return null;
	}

	@Override
	public CommandType getType() {
		return null;
	}

	@Override
	public SenderType getSenderType() {
		return null;
	}
}
