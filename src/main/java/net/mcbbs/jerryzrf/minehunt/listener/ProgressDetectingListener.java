package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.game.GameProgress;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ProgressDetectingListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	
	//指南针
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void craftCompass(CraftItemEvent event) {
		if (event.getRecipe().getResult().getType() != Material.COMPASS) {
			return;
		}
		plugin.getGame().getProgressManager().unlockProgress(GameProgress.COMPASS_UNLOCKED, null);
	}
	
	//铁
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void getIron(FurnaceExtractEvent event) {
		if (event.getItemType() != Material.IRON_INGOT) {
			return;
		}
		plugin.getGame().getProgressManager().unlockProgress(GameProgress.IRON_MINED, event.getPlayer());
	}
	
	//石器时代
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void breakStone(BlockBreakEvent event) {
		if (event.getBlock().getType() != Material.STONE) {
			return;
		}
		plugin.getGame().getProgressManager().unlockProgress(GameProgress.STONE_AGE, event.getPlayer());
		plugin.getGame().getGameEndingData().setStoneAgePassed(event.getPlayer().getName());
	}
	
	//下界 末地 传送门
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void changeDim(PlayerPortalEvent event) {
		if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.ENTER_NETHER, event.getPlayer());
			return;
		}
		if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.ENTER_END, event.getPlayer());
		}
	}
	
	//下界 末地 进入
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void teleport(PlayerTeleportEvent event) {
		if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.ENTER_NETHER, event.getPlayer());
			return;
		}
		if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.ENTER_END, event.getPlayer());
		}
	}
	
	//捡起 末影珍珠 烈焰棒
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void pickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		if (event.getItem().getItemStack().getType() == Material.ENDER_PEARL) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.GET_ENDER_PERAL, ((Player) event.getEntity()).getPlayer());
			return;
		}
		if (event.getItem().getItemStack().getType() == Material.BLAZE_ROD) {
			plugin.getGame().getProgressManager().unlockProgress(GameProgress.GET_BLAZE_ROD, ((Player) event.getEntity()).getPlayer());
		}
	}
}
