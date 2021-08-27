package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.Messages;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.game.GameStatus;
import net.mcbbs.jerryzrf.minehunt.game.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.kit.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerServerListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void join(PlayerJoinEvent event) {
		if (plugin.getGame().getStatus() == GameStatus.WAITING_PLAYERS) {
			if (plugin.getGame().playerJoining(event.getPlayer())) {
				Kit.playerKits.put(event.getPlayer(), 0);
				Kit.useKitTime.put(event.getPlayer(), 0L);
				Kit.mode.put(event.getPlayer(), 0);
				Kit.lastMode.put(event.getPlayer(), 0);
				event.getPlayer().setGameMode(GameMode.ADVENTURE);
			} else {
				//人满了
				event.getPlayer().setGameMode(GameMode.SPECTATOR);
				event.getPlayer().sendMessage(Messages.GameFull);
			}
		} else {
			//游戏开始后玩家加入
			if (plugin.getGame().getInGamePlayers().stream().anyMatch(p -> p.getUniqueId().equals(event.getPlayer().getUniqueId()))) {
				//断线重连
				plugin.getGame().getInGamePlayers().removeIf(p -> p.getUniqueId().equals(event.getPlayer().getUniqueId()));
				plugin.getGame().getInGamePlayers().add(event.getPlayer());
				
				for (Map.Entry<Player, PlayerRole> playerPlayerRoleEntry : plugin.getGame().getRoleMapping().entrySet()) {
					if (playerPlayerRoleEntry.getKey().getUniqueId().equals(event.getPlayer().getUniqueId())) {
						PlayerRole role = playerPlayerRoleEntry.getValue();
						plugin.getGame().getRoleMapping().remove(playerPlayerRoleEntry.getKey());
						plugin.getGame().getRoleMapping().put(event.getPlayer(), role);
						break;
					}
				}
				
				if (plugin.getGame().getInGamePlayers().contains(event.getPlayer())) {
					Bukkit.broadcastMessage(ChatColor.GREEN + "玩家 " + event.getPlayer().getName() + " 已重新连接");
					plugin.getGame().getReconnectTimer().entrySet().removeIf(set -> set.getKey().getUniqueId().equals(event.getPlayer().getUniqueId()));
				}
				
			} else {
				//普通加入：旁观模式
				event.getPlayer().setGameMode(GameMode.SPECTATOR);
				event.getPlayer().sendMessage(Messages.GameStart);
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void quit(PlayerQuitEvent event) {
		if (!plugin.getGame().getInGamePlayers().contains(event.getPlayer())) {
			//是玩家
			return;
		}
		//是旁观者
		plugin.getGame().playerLeaving(event.getPlayer());
	}
}
