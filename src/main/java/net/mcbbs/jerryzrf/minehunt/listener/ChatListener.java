package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.Optional;


public class ChatListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chat(AsyncPlayerChatEvent event) {
		//游戏已开始
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getPlayer());
		if (role.isEmpty()) {
			event.setFormat(ChatColor.GRAY + plugin.getConfig().getString("ObserverName") + " " + event.getPlayer().getDisplayName() + " " + ChatColor.RESET + event.getMessage());
			return;
		}
		//私聊
		List<String> chatInTeam = plugin.getConfig().getStringList("ChatinTeam");
		for (int i = 0; i < chatInTeam.size(); i++) {
			if (event.getMessage().startsWith(chatInTeam.get(i))) {
				event.setCancelled(true);
				Optional<PlayerRole> players = plugin.getGame().getPlayerRole(event.getPlayer());
				int finalI = i;
				plugin.getGame().getPlayersAsRole(players.get()).forEach(p -> p.sendMessage(ChatColor.GRAY + "[团队] " + event.getPlayer().getDisplayName() + ": " + ChatColor.RESET + event.getMessage().replace(chatInTeam.get(finalI), "")));
				return;
			}
		}
		//聊天加入前缀
		if (role.get() == PlayerRole.HUNTER) {
			event.setFormat(ChatColor.RED + plugin.getConfig().getString("HunterName") + " " + event.getPlayer().getDisplayName() + " " + ChatColor.RESET + event.getMessage());
		} else if (role.get() == PlayerRole.RUNNER) {
			event.setFormat(ChatColor.GREEN + plugin.getConfig().getString("RunnerName") + " " + event.getPlayer().getDisplayName() + " " + ChatColor.RESET + event.getMessage());
		}
	}
}
