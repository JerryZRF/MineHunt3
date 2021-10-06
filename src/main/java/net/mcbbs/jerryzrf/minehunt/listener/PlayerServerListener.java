package net.mcbbs.jerryzrf.minehunt.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class PlayerServerListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void join(PlayerJoinEvent event) {
		if (plugin.getGame().getStatus() == GameStatus.Waiting) {
            if (plugin.getGame().playerJoining(event.getPlayer())) {
				plugin.getGame().getNoRolesPlayers().add(event.getPlayer());
                if (KitManager.isEnable()) {
					KitManager.playerKits.put(event.getPlayer().getName(), 0);
					KitManager.useKitTime.put(event.getPlayer().getName(), 0L);
					KitManager.mode.put(event.getPlayer().getName(), 0);
					KitManager.lastMode.put(event.getPlayer().getName(), 0);
					ItemStack is = KitManager.kitItem.clone();
					ItemMeta im = is.getItemMeta();
					im.setLore(List.of("点击打开职业菜单", "KIT"));
					is.setItemMeta(im);
					event.getPlayer().getInventory().setItem(8, is);
				}
				ItemStack item = new ItemStack(Material.RED_BED);
				ItemMeta im = item.getItemMeta();
				im.setLore(List.of("点击选择阵容", "TEAM"));
				item.setItemMeta(im);
				event.getPlayer().getInventory().setItem(0, item);
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
			//不在游戏中玩家中
			event.getPlayer().getInventory().clear();  //清空物品栏
			return;
		}
		//是游戏中的玩家
		plugin.getGame().playerLeaving(event.getPlayer());
	}

	@EventHandler
	public void motd(ServerListPingEvent event) {
		TextComponent motd = Component.text(plugin.getGame().getStatus().name());
		event.motd(motd);
	}
}
