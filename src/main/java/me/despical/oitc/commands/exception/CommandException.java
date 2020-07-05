package me.despical.oitc.commands.exception;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class CommandException extends Exception {

	private static final long serialVersionUID = 1L;

	public CommandException(String message) {
		super (message);
	}
}