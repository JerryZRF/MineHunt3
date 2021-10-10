package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.Kit;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.kit.KitManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
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
		// 复活给予猎人指南针&职业工具
		if (plugin.getGame().getStatus() == GameStatus.Running) {
			if (plugin.getGame().isCompassUnlocked()) {
				Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getPlayer());
				if (role.isPresent()) {
					if (role.get() == PlayerRole.HUNTER) {
						event.getPlayer().getInventory().addItem(new ItemStack(Material.COMPASS));
					}
				}
			}
			if (KitManager.isEnable()) {
				event.getPlayer().getInventory().setItem(8, KitManager.kitItem);
				KitManager.giveKitItems(event.getPlayer());
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void deathDropRemove(PlayerDeathEvent event) {
		event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.COMPASS);      //删除死亡掉落的指南针
		if (KitManager.isEnable()) {
			//删除掉落的职业物品
			event.getDrops().removeIf(itemStack -> itemStack.getItemMeta().getLore() == null || itemStack.getItemMeta().getLore().contains("KIT"));
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void clickCompass(PlayerInteractEvent event) {
		if (event.getItem() == null) {
			return;
		}
		if (event.getItem().getType() != Material.COMPASS) {
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
	public void kitGUIClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		// 只有玩家可以触发 InventoryClickEvent，可以强制转换
		InventoryView inv = player.getOpenInventory();
		if (inv.getTitle().equals("职业")) {
			// 通过标题区分 GUI
			event.setCancelled(true);
		} else {
			return;
		}
		if (event.getRawSlot() < 0 || event.getRawSlot() >= KitManager.kits.size()) {
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
		if (KitManager.kits.get(event.getSlot()).permission != null && !((Player) event.getWhoClicked()).getPlayer().hasPermission(KitManager.kits.get(event.getSlot()).permission)) {
			((Player) event.getWhoClicked()).getPlayer().sendMessage(Messages.NoPermission);
			return;
		}
		KitManager.playerKits.put(((Player) event.getWhoClicked()).getPlayer().getName(), event.getSlot());
		event.getWhoClicked().sendMessage("已选择职业" + ChatColor.GREEN + KitManager.kits.get(event.getRawSlot()).name);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void clickKitItem(PlayerInteractEvent event) {
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		if (event.getItem() == null) {
			return;
		}
		if (event.getItem().getType() != KitManager.kitItem.getType() || !event.getItem().getItemMeta().getLore().contains("KIT")) {
			return;
		}
		Kit kits = KitManager.getPlayerKit(event.getPlayer());
		if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
			Date time = new Date();
			//冷却
			if ((time.getTime() - KitManager.useKitTime.get(event.getPlayer().getName()) <
					(kits.mode.get(KitManager.lastMode.get(event.getPlayer().getName())).CD) * 1000L)) {
				event.getPlayer().sendMessage(Messages.KitColding.replace("%d", String.valueOf((kits.mode.get(KitManager.lastMode.get(
						event.getPlayer().getName())).CD * 1000L - time.getTime() + KitManager.useKitTime.get(event.getPlayer().getName())) / 1000)));
				return;
			}
			//buff
			for (int i = 0; i < kits.mode.get(KitManager.mode.get(event.getPlayer().getName())).duration.size(); i++) {
				event.getPlayer().addPotionEffect(new PotionEffect(
						PotionEffectType.getByName(kits.buff.get(i)),
						(int) (Double.parseDouble(kits.mode.get(KitManager.mode.get(event.getPlayer().getName())).duration.get(i).toString()) * 20),
						kits.mode.get(KitManager.mode.get(event.getPlayer().getName())).level.get(i))
				);
			}
			//使用完毕
			event.getPlayer().sendMessage(Messages.UseKit);
			KitManager.lastMode.put(event.getPlayer().getName(), KitManager.mode.get(event.getPlayer().getName()));
			KitManager.useKitTime.put(event.getPlayer().getName(), time.getTime());
		} else {
			KitManager.mode.put(event.getPlayer().getName(), (KitManager.mode.get(event.getPlayer().getName()) + 1) % kits.mode.size());
			event.getPlayer().sendMessage(Messages.ChangeKitMode.replace("%s", kits.mode.get(KitManager.mode.get(event.getPlayer().getName())).name));
		}
    }

	@EventHandler(priority = EventPriority.MONITOR)
    public void clickKitChoiceItem(PlayerInteractEvent event) {
		if (event.getItem() == null) {
			return;
		}
		if (event.getItem().getType() != KitManager.kitItem.getType()) {
			return;
		}
		if (plugin.getGame().getStatus() != GameStatus.Waiting) {
			return;
		}
		event.getPlayer().performCommand("minehunt kits");
	}

	@EventHandler
	public void dropKitItems(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getItemMeta().getLore() != null && (event.getItemDrop().getItemStack().getItemMeta().getLore().contains("KIT") || event.getItemDrop().getItemStack().getItemMeta().getLore().contains("TEAM"))) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void clickChooseTeamItem(PlayerInteractEvent event) {
		if (plugin.getGame().getStatus() != GameStatus.Waiting) {
			return;
		}
		if (event.getItem() == null || event.getItem().getItemMeta().getLore() == null) {
			return;
		}
		if (event.getItem().getItemMeta().getLore().contains("TEAM")) {
			Inventory inv = Bukkit.createInventory(event.getPlayer(), 9, "阵容");
			ItemStack item = new ItemStack(Material.GREEN_WOOL);
			item.setDisplayName("逃亡者");
			ItemMeta im = item.getItemMeta();
			im.setLore(List.of("点击加入"));
			item.setItemMeta(im);
			inv.addItem(item);
			item = new ItemStack(Material.RED_WOOL);
			item.setDisplayName("猎人");
			im = item.getItemMeta();
			im.setLore(List.of("点击加入"));
			item.setItemMeta(im);
			inv.addItem(item);
			event.getPlayer().openInventory(inv);
		}
	}

	@EventHandler
	public void clickTeamGUI(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		// 只有玩家可以触发 InventoryClickEvent，可以强制转换
		InventoryView inv = player.getOpenInventory();
		if (inv.getTitle().equals("阵容")) {
			// 通过标题区分 GUI
			event.setCancelled(true);
		} else {
			return;
		}
		if (event.getRawSlot() < 0 || event.getRawSlot() >= 2) {
			// 这个方法来源于 Bukkit Development Note
			// 如果在合理的范围内，getRawSlot 会返回一个合适的编号（0 ~ 物品栏大小-1）
			return;
			// 结束处理，使用 return 避免了多余的 else
		}

		// 后续处理

		if (event.getRawSlot() == 0) {
			event.getWhoClicked().sendMessage(plugin.getGame().playerJoinTeam(player, PlayerRole.RUNNER));
		} else {
			event.getWhoClicked().sendMessage(plugin.getGame().playerJoinTeam(player, PlayerRole.HUNTER));
		}
	}
}

