package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.Messages;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.game.GameStatus;
import net.mcbbs.jerryzrf.minehunt.game.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.kit.Kit;
import net.mcbbs.jerryzrf.minehunt.kit.KitInfo;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PlayerItemListener implements Listener {
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
	public void respawnGivenItem(PlayerRespawnEvent event) {
		//开局/复活给予猎人指南针&职业工具
		if (plugin.getGame().getStatus() == GameStatus.GAME_STARTED) {
			if (plugin.getGame().isCompassUnlocked()) {
				Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getPlayer());
				if (role.isPresent()) {
					if (role.get() == PlayerRole.HUNTER) {
						event.getPlayer().getInventory().addItem(new ItemStack(Material.COMPASS));
					}
				}
			}
			event.getPlayer().getInventory().setItem(8, Kit.kitItem);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void deathDropRemove(PlayerDeathEvent event) {
		event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.COMPASS);  //删除死亡掉落的指南针
		event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.NETHER_STAR);  //删除死亡掉落的职业工具
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void clickCompass(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
			return;
		}
		
		if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) {
			return;
		}
		if (!plugin.getGame().isCompassUnlocked()) {
			//没解锁指南针
			event.getPlayer().setCompassTarget(event.getPlayer().getWorld().getSpawnLocation());
			event.getPlayer().sendMessage(Messages.NoCompass);
			return;
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
	
	@EventHandler
	public void GUIClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		// 只有玩家可以触发 InventoryClickEvent，可以强制转换
		InventoryView inv = player.getOpenInventory();
		if (inv.getTitle().equals("职业")) {
			// 通过标题区分 GUI
			event.setCancelled(true);
		}
		if (event.getRawSlot() < 0 || event.getRawSlot() >= Kit.kits.size()) {
			// 这个方法来源于 Bukkit Development Note
			// 如果在合理的范围内，getRawSlot 会返回一个合适的编号（0 ~ 物品栏大小-1）
			return;
			// 结束处理，使用 return 避免了多余的 else
		}
		ItemStack clickedItem = event.getCurrentItem();
		// 获取被点的物品
		if (clickedItem == null) {
			// 确保不是 null
			return;
		}
		// 后续处理
		Kit.playerKits.put((Player) event.getWhoClicked(), event.getSlot());
		event.getWhoClicked().sendMessage("已选择职业" + Kit.kits.get(event.getRawSlot()).name);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void clickKitItem(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
			return;
		}
		if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED) {
			return;
		}
		if (event.getItem() == null || event.getItem().getType() != Material.NETHER_STAR) {
			return;
		}
		KitInfo kits = Kit.kits.get(Kit.playerKits.get(event.getPlayer()));
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			Date time = new Date();
			
			if ((time.getTime() - Kit.useKitTime.get(event.getPlayer()) <
					(kits.mode.get(Kit.lastMode).CD) * 1000L)) {
				event.getPlayer().sendMessage("技能冷却中...，剩余" +
						((kits.mode.get(Kit.lastMode).CD * 1000L - time.getTime() + Kit.useKitTime.get(event.getPlayer())) / 1000) + "s");
				return;
			}
			
			for (int i = 0; i < kits.mode.get(Kit.mode).duration.size(); i++) {
				event.getPlayer().addPotionEffect(new PotionEffect(
						PotionEffectType.getByName(kits.buff.get(i)),
						(kits.mode.get(Kit.mode).duration.get(i)) * 20,
						kits.mode.get(Kit.mode).level.get(i)));
			}
			
			event.getPlayer().sendMessage(org.bukkit.ChatColor.GOLD + "技能使用成功！");
			Kit.lastMode = Kit.mode;
			Kit.useKitTime.put(event.getPlayer(), time.getTime());
		} else {
			Kit.mode++;
			Kit.mode %= Kit.kits.size();
			event.getPlayer().sendMessage(ChatColor.GOLD + "技能模式已更换，当前为" + kits.mode.get(Kit.mode).name);
		}
	}
}
