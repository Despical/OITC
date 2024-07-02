package me.despical.oitc.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public class Utils {

	private Utils() {
	}

	public static void addPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
		PotionEffect effect;

		try {
			effect = new PotionEffect(type, duration, amplifier, false, false, false);
		} catch (Exception | Error throwable) {
			effect = new PotionEffect(type, duration, amplifier, false, false);
		}

		player.addPotionEffect(effect);
	}
}