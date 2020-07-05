package me.despical.oitc.arena;

/**
 * Contains all GameStates.
 * 
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public enum ArenaState {

	WAITING_FOR_PLAYERS("Waiting"), STARTING("Starting"), IN_GAME("Playing"), ENDING("Finishing"), RESTARTING("Restarting"), INACTIVE("Inactive");

	String formattedName;

	ArenaState(String formattedName) {
		this.formattedName = formattedName;
	}

	public String getFormattedName() {
		return formattedName;
	}
}