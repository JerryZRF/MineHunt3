package net.mcbbs.jerryzrf.minehunt.game;

import net.mcbbs.jerryzrf.minehunt.Messages;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 处理新的进度事件
 */
public class GameProgressManager {
	private final MineHunt plugin = MineHunt.getInstance();
	private final Set<GameProgress> unlocked = new HashSet<>();
	private final String prefix = Messages.prefix;
	
	/**
	 * 检查和解锁新的游戏进度
	 *
	 * @param progress 游戏进度
	 */
	public void unlockProgress(GameProgress progress, Player player) {
		//未开始
		if (plugin.getGame().getStatus() != GameStatus.GAME_STARTED || plugin.getGame().getStatus() != GameStatus.ENDED) {
			return;
		}
		//已解锁进度
		if (!unlocked.add(progress)) {
			return;
		}
		processProgress(progress, player);
	}
	
	/***
	 * 发放进度奖励
	 *
	 * @param progress 进度
	 * @param p        玩家
	 */
	private void processProgress(GameProgress progress, Player p) {
		if (p == null) {
			return;
		}
		Optional<PlayerRole> players = plugin.getGame().getPlayerRole(p);
		if (players.isEmpty()) {
			return;
		}
		switch (progress) {
			case GAME_STARTING -> plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.BREAD, plugin.getConfig().getInt("Bread"))));
			case STONE_AGE, IRON_MINED -> {
				broadcastProgress(progress, true, true);
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.IRON_ORE, 8)));
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 1)));
			}
			case COMPASS_UNLOCKED, KILLED_DRAGON -> broadcastProgress(progress, false, false);
			case ENTER_NETHER -> {
				broadcastProgress(progress, true, false);
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.OBSIDIAN, 4)));
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.FLINT, 1)));
			}
			case GET_BLAZE_ROD -> {
				broadcastProgress(progress, false, true);
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 1)));
			}
			case GET_ENDER_PERAL -> {
				broadcastProgress(progress, true, false);
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1)));
			}
			case ENTER_END -> {
				broadcastProgress(progress, true, false);
				plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1)));
			}
		}
	}
	
	/***
	 * 广播解锁进度
	 * @param progress 进度
	 * @param item     是否有物品
	 * @param buff     是否有buff
	 */
	private void broadcastProgress(GameProgress progress, boolean item, boolean buff) {
		Bukkit.broadcastMessage(prefix + ChatColor.AQUA + "新的游戏阶段已解锁 " + ChatColor.GREEN + "[" + progress.getDisplay() + "]");
		if (item) {
			Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "奖励补给已发放到您的背包中，请查收!");
		}
		if (buff) {
			Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "奖励药水效果已应用，请查看！");
		}
	}
}
