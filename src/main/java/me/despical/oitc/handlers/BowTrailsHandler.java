/*
 * OITC - Reach 25 points to win!
 * Copyright (C) 2020 Despical
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.handlers;

import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.utils.Debugger;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class BowTrailsHandler implements Listener {

	private final Main plugin;
	private final Map<String, Particle> registeredTrails = new LinkedHashMap<>();

	public BowTrailsHandler(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		registerBowTrail("oitc.trails.heart", Particle.HEART);
		registerBowTrail("oitc.trails.flame", Particle.FLAME);
		registerBowTrail("oitc.trails.critical", Particle.CRIT);
		registerBowTrail("oitc.trails.cloud", Particle.CLOUD);
		registerBowTrail("oitc.trails.lava", Particle.DRIP_LAVA);
		registerBowTrail("oitc.trails.water", Particle.WATER_DROP);
	}

	public void registerBowTrail(String permission, Particle particle) {
		registeredTrails.put(permission, particle);
	}

	@EventHandler
	public void onArrowShoot(EntityShootBowEvent e) {
		if (!(e.getEntity() instanceof Player && e.getProjectile() instanceof Arrow)) {
			return;
		}

		if (!ArenaRegistry.isInArena((Player) e.getEntity()) || e.getProjectile().isDead() || e.getProjectile().isOnGround()) {
			return;
		}

		for (String perm : registeredTrails.keySet()) {
			if (e.getEntity().hasPermission(perm)) {
				new BukkitRunnable() {
					
					@Override
					public void run() {
						if (e.getProjectile().isDead() || e.getProjectile().isOnGround()) {
							this.cancel();
						}

						Debugger.debug("Spawned particle with perm {0} for player {1}", perm, e.getEntity().getName());

						e.getProjectile().getWorld().spawnParticle(registeredTrails.get(perm), e.getProjectile().getLocation(), 3, 0, 0, 0, 0);
					}
				}.runTaskTimer(plugin, 0, 0);

				break;
			}
		}
	}
}