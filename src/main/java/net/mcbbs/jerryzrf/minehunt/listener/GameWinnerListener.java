package net.mcbbs.jerryzrf.minehunt.listener;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;
import java.util.Optional;

public class GameWinnerListener implements Listener {
	private final MineHunt plugin = MineHunt.getInstance();
	private String dragonKiller = "Magic";

	BossBar dragonHealth = Bukkit.createBossBar(
			new NamespacedKey(plugin, "dragonHealth"),
			"末影龙",
			BarColor.PURPLE,
			BarStyle.SEGMENTED_20,
			BarFlag.PLAY_BOSS_MUSIC
	);

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void playerDeath(PlayerDeathEvent event) {
		//游戏已开始
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		Optional<PlayerRole> role = plugin.getGame().getPlayerRole(event.getEntity());  //死亡玩家的角色
		if (role.isPresent()) {
			if (role.get() == PlayerRole.RUNNER) {
				//逃亡者死了
				String killer = Objects.requireNonNull(event.deathMessage()).toString();
				if (event.getEntity().getLastDamageCause() != null) {
					killer = event.getEntity().getLastDamageCause().getEntity().getName();  //获得最后一次造成逃亡者伤害的实体
					if (event.getEntity().getLastDamageCause().getEntity() instanceof Projectile projectile) {
						if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
							killer = ((Player) projectile.getShooter()).getName();
						}
					}
				}
				String finalKiller = killer;
				plugin.getGame().getGameEndingData().setRunnerKiller(finalKiller);
				event.getEntity().setGameMode(GameMode.SPECTATOR);  //旁观模式
				if (plugin.getGame().getPlayersAsRole(PlayerRole.RUNNER).stream().allMatch(p -> p.getGameMode() == GameMode.SPECTATOR)) {
					//游戏结束
					plugin.getGame().stop(PlayerRole.HUNTER, event.getEntity().getLocation().add(0, 3, 0));
					event.getEntity().setHealth(20); //Prevent player dead
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void entityDeath(EntityDamageByEntityEvent event) {
		//游戏已开始
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		//末影龙
		if (event.getEntityType() != EntityType.ENDER_DRAGON) {
			return;
		}
		//玩家对末影龙造成伤害
		if (event.getDamager() instanceof Player) {
			Optional<PlayerRole> role = MineHunt.getInstance().getGame().getPlayerRole(((Player) event.getDamager()));
			if (role.isPresent()) {
				if (role.get() == PlayerRole.HUNTER) {
					//猎人攻击龙
					event.setCancelled(true);  //取消事件
					event.getEntity().sendMessage(ChatColor.RED + Messages.HunterHurtDragon);
					return;
				}
			}
			LivingEntity livingEnt = (LivingEntity) event.getEntity();
			dragonHealth.setProgress(livingEnt.getHealth() / livingEnt.getMaxHealth());
			Bukkit.getOnlinePlayers().forEach(p -> {
				if (p.getWorld().getEnvironment() != World.Environment.THE_END || !dragonHealth.getPlayers().contains(p)) {
					//不在末地 && 没有添加
					dragonHealth.addPlayer(p);
				}
			});
		}
		dragonKiller = event.getDamager().getName();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void entityDeath(EntityDamageByBlockEvent event) {
		//游戏已开始
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		//末影龙
		if (event.getEntityType() != EntityType.ENDER_DRAGON) {
			return;
		}
		if (event.getDamager() == null) {
			return;
		}
		dragonKiller = event.getDamager().getType().name();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void entityDeath(EntityDeathEvent event) {
		//游戏已开始
		if (plugin.getGame().getStatus() != GameStatus.Running) {
			return;
		}
		//末影龙
		if (event.getEntityType() != EntityType.ENDER_DRAGON) {
			return;
		}
		//设置屠龙勇士
		plugin.getGame().getGameEndingData().setDragonKiller(dragonKiller);
		//游戏结束
		plugin.getGame().stop(PlayerRole.RUNNER, new Location(event.getEntity().getLocation().getWorld(), 0, 85, 0));
	}
}
