package net.mcbbs.jerryzrf.minehunt.watcher;

import lombok.Getter;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class RadarWatcher {
	private final MineHunt plugin = MineHunt.getInstance();
	@Getter
	private final int warnDistance = plugin.getConfig().getInt("WarnDistance");
	
	public RadarWatcher() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
					return;
				}
				List<Player> runners = plugin.getGame().getPlayersAsRole(PlayerRole.RUNNER);
				List<Player> hunters = plugin.getGame().getPlayersAsRole(PlayerRole.HUNTER);
				for (Player hunter : hunters) {
					for (Player runner : runners) {
						//不在同一个世界
						if (hunter.getWorld() != runner.getWorld()) {
							continue;
						}
						//旁观者模式
						if (runner.getGameMode() == GameMode.SPECTATOR) {
							continue;
						}
						double distance = hunter.getLocation().distance(runner.getLocation());
						TextComponent textComponent;
						if (distance > warnDistance) {
							textComponent = new TextComponent(Messages.WarnDistanceSafe.replace("%wd", String.valueOf(warnDistance)));
							textComponent.setColor(ChatColor.GREEN);
						} else {
							textComponent = new TextComponent(Messages.WarnDistanceClose.replace("%d", String.valueOf((int) distance)));
							textComponent.setColor(ChatColor.RED);
						}
						runner.sendMessage(ChatMessageType.ACTION_BAR, textComponent);
					}
				}
			}
		}.runTaskTimer(MineHunt.getInstance(), 0, 20);
	}
}
