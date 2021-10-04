package net.mcbbs.jerryzrf.minehunt.watcher;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class CountDownWatcher {
	private final MineHunt plugin = MineHunt.getInstance();
	int remains = plugin.getGame().getCountdown();                  //倒计时
	int shorter = plugin.getConfig().getInt("FullCountDown");  //满人后的倒计时缩减
	
	public CountDownWatcher() {
		new BukkitRunnable() {
			@Override
			public void run() {
				Game game = plugin.getGame();
				if (game.getStatus() != GameStatus.Waiting) {
					return;
				}
				//倒计时结束
				if (remains <= 0) {
					game.getInGamePlayers().forEach(p ->
							p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
					game.start();
					return;
				}
				//玩家数量不足
				if (game.getInGamePlayers().size() < game.getMinPlayers()) {
					game.getInGamePlayers().forEach(p -> p.sendTitle(ChatColor.AQUA + "" + game.getInGamePlayers().size() + " " + ChatColor.WHITE + "/ " + ChatColor.AQUA + game.getMinPlayers(),
							Messages.Waiting, 0, 40, 0));
					//我觉得这没有必要，倒计时暂停就够了
					//remains = plugin.getGame().getCountdown();  //倒计时重置
					return;
				} else {
					game.getInGamePlayers().forEach(p -> {
						p.sendTitle(ChatColor.GOLD.toString() + remains,
								"游戏即将开始... [" + game.getInGamePlayers().size() + "/" + game.getMaxPlayers() + "]", 0, 40, 0);
						p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
					});
				}
				remains--;  //倒计时继续
				//玩家数量 >= 最大玩家数
				if (game.getInGamePlayers().size() >= game.getMaxPlayers()) {
					if (remains > shorter) {
						remains = shorter;
						Bukkit.broadcastMessage(Messages.PlayerEnough);
					}
				}
			}
		}.runTaskTimer(plugin, 0, 20);
	}
	
	public void resetCountdown() {
		remains = plugin.getGame().getCountdown();
		Bukkit.broadcastMessage(Messages.resetCountdown);
	}
}
