package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.game.GameStatus;
import net.mcbbs.jerryzrf.minehunt.game.PlayerRole;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.List;
import java.util.Optional;

public class PlayerCompassListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void craftCompass(CraftItemEvent event) {
		//是指南针
		if (event.getRecipe().getResult().getType() != Material.COMPASS) {
			return;
		}
		Player player = (Player) event.getWhoClicked();                      //获取合成玩家
		Optional<PlayerRole> role = plugin.getGame().getPlayerRole(player);  //获取玩家角色
		if (role.isEmpty()) {
			return;
		}
		if (role.get() == PlayerRole.HUNTER) {
			plugin.getGame().switchCompass(true);  //猎人合成，解锁
		} else if (role.get() == PlayerRole.RUNNER) {
			plugin.getGame().switchCompass(false); //逃亡者合成，锁回去
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void respawnGivenCompass(PlayerRespawnEvent event) {
		//开局/复活给予猎人指南针
		if (plugin.getGame().getStatus() == GameStatus.GAME_STARTED && plugin.getGame().isCompassUnlocked()) {
			Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getPlayer());
			if (role.isPresent()) {
				if (role.get() == PlayerRole.HUNTER) {
					event.getPlayer().getInventory().addItem(new ItemStack(Material.COMPASS));
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void deathDropRemoveCompass(PlayerDeathEvent event) {
		event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.COMPASS);  //删除死亡掉落的指南针
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void clickCompass(PlayerInteractEvent event) {
		//这句话我觉得没必要
        /*
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        */
		if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) {
			return;
		}
		if (!plugin.getGame().isCompassUnlocked()) {
			//没解锁指南针
			event.getPlayer().setCompassTarget(event.getPlayer().getWorld().getSpawnLocation());
			event.getPlayer().sendMessage(Messages.NoCompass);
		}
		List<Player> runners = plugin.getGame().getPlayersAsRole(PlayerRole.RUNNER);
		if (runners.isEmpty()) {
			event.getPlayer().sendMessage("追踪失败，所有逃亡者均已离线等待重连中...");
		}
		Player closestRunner = null;
		int dis = 0;
		for (Player runner : runners) {
			if (runner.getWorld() != event.getPlayer().getWorld()) {
				//不在一个世界
				continue;
			}
			if (runner.getGameMode() == GameMode.SPECTATOR) {
				//旁观者模式
				continue;
			}
			if (closestRunner == null) {
				closestRunner = runner;
				dis = (int) event.getPlayer().getLocation().distance(runner.getLocation());
				continue;
			}
			if (event.getPlayer().getLocation().distance(runner.getLocation()) < closestRunner.getLocation().distance(event.getPlayer().getLocation())) {
				closestRunner = runner;
				dis = (int) event.getPlayer().getLocation().distance(runner.getLocation());
			}
		}
		if (closestRunner == null) {
			event.getPlayer().sendMessage(Messages.DifferentWorld);
		} else {
			TextComponent component = new TextComponent(Messages.FindRunner.replace("%s", closestRunner.getName()).replace("%d", String.valueOf(dis)));
			component.setColor(ChatColor.AQUA);
			if (event.getPlayer().getWorld().getEnvironment() == World.Environment.NORMAL) {
				//在主世界
				event.getPlayer().setCompassTarget(closestRunner.getLocation());
			} else {
				//不在主世界
				CompassMeta compassMeta = (CompassMeta) event.getItem().getItemMeta();
				if (compassMeta == null) {
					event.getPlayer().sendMessage("错误：指南针损坏，请联系服务器管理员报告BUG.");
				}
				compassMeta.setLodestone(closestRunner.getLocation());
				compassMeta.setLodestoneTracked(false); //如果为true，则目标位置必须有Lodestone才有效；因此设为false 这貌似也是ManiHunt中的一个BUG
				event.getItem().setItemMeta(compassMeta);
			}
			event.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, component);
		}
	}
}
