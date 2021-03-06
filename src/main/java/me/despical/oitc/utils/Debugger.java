/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2021 Despical and contributors
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

package me.despical.oitc.utils;

import me.despical.commons.util.Strings;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Debugger {

	private static boolean enabled, deep;
	private static final Logger logger = Logger.getLogger("OITC");
	private static final HashSet<String> listenedPerformance = new HashSet<>();

	private Debugger() {
	}

	public static void setEnabled(boolean enabled) {
		Debugger.enabled = enabled;
	}

	public static void deepDebug(boolean deep) {
		Debugger.deep = deep;
	}

	public static void monitorPerformance(String task) {
		listenedPerformance.add(task);
	}

	public static void sendConsoleMessage(String message) {
		Bukkit.getConsoleSender().sendMessage(Strings.format(message));
	}

	/**
	 * Prints debug message with selected log level. Messages of level INFO or TASK
	 * won't be posted if debugger is enabled, warnings and errors will be.
	 *
	 * @param level level of debugged message
	 * @param msg message to debug
	 */
	public static void debug(Level level, String msg) {
		if (!enabled && (level != Level.WARNING || level != Level.SEVERE)) {
			return;
		}

		logger.log(level, "[OITCDBG] " + msg);
	}

	/**
	 * Prints debug message with selected INFO log level.
	 *
	 * @param msg debugged message
	 * @param params to debug
	 */
	public static void debug(String msg, Object... params) {
		debug(Level.INFO, msg, params);
	}

	/**
	 * Prints debug message with selected log level. Messages of level INFO or TASK
	 * won't be posted if debugger is enabled, warnings and errors will be.
	 *
	 * @param msg debugged message
	 */
	public static void debug(String msg) {
		debug(Level.INFO, msg);
	}

	/**
	 * Prints debug message with selected log level and replaces parameters.
	 * Messages of level INFO or TASK won't be posted if debugger is enabled,
	 * warnings and errors will be.
	 *
	 * @param level level of debugged message
	 * @param msg debugged message
	 * @param params any params to debug
	 */
	public static void debug(Level level, String msg, Object... params) {
		if (!enabled && (level != Level.WARNING || level != Level.FINE)) {
			return;
		}

		logger.log(level, "[OITCDBG] " + msg, params);
	}

	/**
	 * Prints performance debug message with selected log level and replaces
	 * parameters.
	 *
	 * @param monitorName name of monitor
	 * @param msg debugged message
	 * @param params any params to debug
	 */
	public static void performance(String monitorName, String msg, Object... params) {
		if (!deep) {
			return;
		}

		if (!listenedPerformance.contains(monitorName)) {
			return;
		}

		logger.log(Level.INFO, "[OITCDBG] [Performance Monitor] " + msg, params);
	}
}