package net.mcbbs.jerryzrf.minehunt.watcher;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class PlayerMoveWatcher {
	private final MineHunt plugin = MineHunt.getInstance();
	//是否已经进入下界/末地
	private boolean hasRunnerNether = false;
	private boolean hasRunnerTheEnd = false;
	
	public PlayerMoveWatcher() {
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.getGame().getInGamePlayers().forEach(player -> {
					World.Environment environment = player.getWorld().getEnvironment();
					//不在主世界
					if (environment != World.Environment.NORMAL) {
						Optional<PlayerRole> role = plugin.getGame().getPlayerRole(player);
						if (role.isPresent()) {
							if (role.get() == PlayerRole.RUNNER) {
								//逃亡者进入下界
								if (!hasRunnerNether && environment == World.Environment.NETHER) {
									hasRunnerNether = true;
									Bukkit.broadcastMessage(Messages.runnerNether);
								}
								//逃亡者进入末地
								if (!hasRunnerTheEnd && environment == World.Environment.THE_END) {
									hasRunnerTheEnd = true;
									Bukkit.broadcastMessage(Messages.runnerTheEnd);
								}
							}
						}
					}
					
				});
			}
		}.runTaskTimer(MineHunt.getInstance(), 0, 40);
	}
}
