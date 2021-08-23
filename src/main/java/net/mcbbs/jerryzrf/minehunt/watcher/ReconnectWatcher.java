package net.mcbbs.jerryzrf.minehunt.watcher;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.game.GameStatus;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ReconnectWatcher {
	private final MineHunt plugin = MineHunt.getInstance();
	
	public ReconnectWatcher() {
		new BukkitRunnable() {
			@Override
			public void run() {
				//游戏已开始
				if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
					return;
				}
				List<Player> removing = new ArrayList<>();
				plugin.getGame().getReconnectTimer().forEach((key, value) -> {
					if (System.currentTimeMillis() - value > 1000 * 600) {
						removing.add(key);
					}
				});
				//Remove timeout players from their team
				//把超时的玩家从团队中移除
				removing.forEach(player -> {
					plugin.getGame().getReconnectTimer().remove(player);
					if (player.isOnline()) {
						return;
					}
					plugin.getGame().playerLeft(player);
				});
			}
		}.runTaskTimer(plugin, 0, 20);
	}
}
