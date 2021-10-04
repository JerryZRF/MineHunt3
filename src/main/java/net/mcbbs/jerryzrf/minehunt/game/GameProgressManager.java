package net.mcbbs.jerryzrf.minehunt.game;

import lombok.Setter;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 处理新的进度事件
 */
public class GameProgressManager {
	private final MineHunt plugin = MineHunt.getInstance();
	private final Set<GameProgress> unlocked = new HashSet<>();
	private final String prefix = Messages.prefix;
	@Setter
	private static boolean enable = true;
	public static Map<String, ProgressInfo> progressInfo = new HashMap<>();
	
	/**
	 * 检查和解锁新的游戏进度
	 *
	 * @param progress 游戏进度
	 */
	public void unlockProgress(GameProgress progress, Player player) {
		//未开始 || 已结束
		
		if (plugin.getGame().getStatus() != GameStatus.Running && plugin.getGame().getStatus() != GameStatus.Ending) {
            return;
        }
        if (!enable) {
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
		if (progressInfo.get(progress.name()) != null) {
			broadcastProgress(progress, progressInfo.get(progress.name()).itemNum != null, progressInfo.get(progress.name()).buffTime != null);
			if (progressInfo.get(progress.name()).itemNum != null) {
				for (int i = 0; i < progressInfo.get(progress.name()).itemNum.size(); i++) {
					int finalI = i;
					plugin.getGame().getPlayersAsRole(players.get()).forEach(
							player -> player.getInventory().addItem(new ItemStack(
											Objects.requireNonNull(Material.getMaterial(progressInfo.get(progress.name()).itemMaterial.get(finalI))),
											progressInfo.get(progress.name()).itemNum.get(finalI)
									)
							)
					);
				}
			}
			if (progressInfo.get(progress.name()).buffTime != null) {
				for (int i = 0; i < progressInfo.get(progress.name()).buffTime.size(); i++) {
					int finalI = i;
					plugin.getGame().getPlayersAsRole(players.get()).forEach(
							player -> player.addPotionEffect(new PotionEffect(
											Objects.requireNonNull(PotionEffectType.getByName(progressInfo.get(progress.name()).buffType.get(finalI))),
											progressInfo.get(progress.name()).buffTime.get(finalI) * 20,
											progressInfo.get(progress.name()).buffLevel.get(finalI)
									)
							)
					);
				}
			}
		}
		if (progress == GameProgress.GAME_STARTING) {
			plugin.getGame().getPlayersAsRole(players.get()).forEach(player -> player.getInventory().addItem(new ItemStack(Material.BREAD, plugin.getConfig().getInt("Bread"))));
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
