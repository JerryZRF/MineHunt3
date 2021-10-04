package net.mcbbs.jerryzrf.minehunt.game;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.kit.Kit;
import net.mcbbs.jerryzrf.minehunt.util.GameEndingData;
import net.mcbbs.jerryzrf.minehunt.util.MusicPlayer;
import net.mcbbs.jerryzrf.minehunt.util.StatisticsBaker;
import net.mcbbs.jerryzrf.minehunt.util.Util;
import net.mcbbs.jerryzrf.minehunt.watcher.PlayerMoveWatcher;
import net.mcbbs.jerryzrf.minehunt.watcher.RadarWatcher;
import net.mcbbs.jerryzrf.minehunt.watcher.ReconnectWatcher;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Game {
	@Getter
	final Map<Player, Double> teamDamageData = new HashMap<>();
	private final MineHunt plugin = MineHunt.getInstance();
	@Getter
	private final Set<Player> inGamePlayers = Sets.newCopyOnWriteArraySet(); //线程安全
	@Getter
	private final Map<Player, Long> reconnectTimer = new HashMap<>();
	@Getter
	private final GameProgressManager progressManager = new GameProgressManager();
	@Getter
	private final GameEndingData gameEndingData = new GameEndingData();
	@Getter
	private final int maxPlayers = plugin.getConfig().getInt("max-players");
	@Getter
	private final int countdown = plugin.getConfig().getInt("Countdown");
	private final int minPlayers = plugin.getConfig().getInt("min-players");
	private final int XRandom = plugin.getConfig().getInt("XRandom");
	private final int XBasic = plugin.getConfig().getInt("XBasic");
	private final int YRandom = plugin.getConfig().getInt("YRandom");
	private final int YBasic = plugin.getConfig().getInt("YBasic");
	private final boolean AutoRestart = plugin.getConfig().getBoolean("AutoRestart");
	private final String prefix = Messages.prefix;
	@Getter
	@Setter
	private GameStatus status = GameStatus.WAITING_PLAYERS;
	@Getter
	private Map<Player, PlayerRole> roleMapping; //线程安全
	@Getter
	private boolean CompassUnlocked = plugin.getConfig().getBoolean("CompassUnlocked");
	public BossBar runnerHealth = Bukkit.createBossBar(
			new NamespacedKey(plugin, "runnerHealth"),
			null,
			BarColor.GREEN,
			BarStyle.SEGMENTED_10,
			BarFlag.PLAY_BOSS_MUSIC
	);

	public void switchCompass(boolean unlocked) {
		if (this.CompassUnlocked == unlocked) {
			return;
		}
		this.CompassUnlocked = unlocked;
		if (unlocked) {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().addItem(new ItemStack(Material.COMPASS, 1)));
			Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "猎人已解锁追踪指南针！逃亡者的位置已经暴露！");
		} else {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().remove(Material.COMPASS));
			Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "猎人的追踪指南针被破坏失效，需要重新解锁！");
		}
		getPlayersAsRole(PlayerRole.RUNNER).forEach(p -> p.getInventory().remove(Material.COMPASS)); //清除逃亡者合成的指南针
	}
	
	/**
	 * 获取玩家角色
	 *
	 * @param player 玩家
	 * @return 可能是Empty（玩家不属于游戏中的玩家）否则返回玩家角色
	 */
	public Optional<PlayerRole> getPlayerRole(Player player) {
		if (status == GameStatus.WAITING_PLAYERS) {
			return Optional.of(PlayerRole.WAITING);
		}
		if (!this.roleMapping.containsKey(player)) {
			return Optional.empty();
		}
		return Optional.of(this.roleMapping.get(player));
	}
	
	/**
	 * 玩家加入
	 *
	 * @param player 玩家
	 * @return 是否可以加入
	 */
	public boolean playerJoining(Player player) {
		reconnectTimer.remove(player);
		if (inGamePlayers.size() < maxPlayers) {
			inGamePlayers.add(player);
			return true;
		}
		return false;
	}
	
	/**
	 * 玩家离开(倒计时)
	 *
	 * @param player 玩家
	 */
	public void playerLeaving(Player player) {
		if (status == GameStatus.WAITING_PLAYERS) {
			this.inGamePlayers.remove(player);
		} else {
			this.reconnectTimer.put(player, System.currentTimeMillis());
		}
	}
	
	/**
	 * 玩家退出(游戏中)
	 *
	 * @param player 玩家
	 */
	public void playerLeft(Player player) {
		this.roleMapping.remove(player);
		this.inGamePlayers.remove(player);
		
		if (getPlayersAsRole(PlayerRole.RUNNER).isEmpty() || getPlayersAsRole(PlayerRole.HUNTER).isEmpty()) {
			Bukkit.broadcastMessage(prefix + Messages.GameExit);
			Bukkit.broadcastMessage(prefix + "服务器将会在 10 秒钟后重新启动。");
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.shutdown();
				}
			}.runTaskLater(plugin, 200);
			return;
		}
		Bukkit.broadcastMessage(prefix + "玩家：" + player.getName() + " 因长时间未能重新连接回对战而被从列表中剔除");
		Bukkit.broadcastMessage(prefix + ChatColor.RED + "猎人: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
		Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "逃亡者: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
	}

	/**
	 * 游戏开始
	 */
	public void start() {
		if (status != GameStatus.WAITING_PLAYERS) {
			return;
		}
		Bukkit.broadcastMessage(prefix + "请稍后，系统正在随机分配玩家身份...");
		Random random = new Random();
		List<Player> noRolesPlayers = new ArrayList<>(inGamePlayers);
		Map<Player, PlayerRole> roleMapTemp = new HashMap<>();

		int runners = 1;
		if (inGamePlayers.size() >= plugin.getConfig().getInt("L0Player")) {
			runners = plugin.getConfig().getInt("L0Runner");
		}
		if (inGamePlayers.size() >= plugin.getConfig().getInt("L1Player")) {
			runners = plugin.getConfig().getInt("L1Runner");
		}
		if (inGamePlayers.size() >= plugin.getConfig().getInt("L2Player")) {
			runners = plugin.getConfig().getInt("L2Runner");
		}
		if (inGamePlayers.size() >= plugin.getConfig().getInt("L3Player")) {
			runners = plugin.getConfig().getInt("L3Runner");
		}
		
		for (int i = 0; i < runners; i++) {
			Player selected = noRolesPlayers.get(random.nextInt(noRolesPlayers.size()));
			roleMapTemp.put(selected, PlayerRole.RUNNER);
			noRolesPlayers.remove(selected);
		}
		noRolesPlayers.forEach(p -> roleMapTemp.put(p, PlayerRole.HUNTER));
		this.roleMapping = new ConcurrentHashMap<>(roleMapTemp);
		Bukkit.broadcastMessage(prefix + "正在将逃亡者随机传送到远离猎人的位置...");
		Location airDropLoc = airDrop(getPlayersAsRole(PlayerRole.RUNNER).get(0).getWorld().getSpawnLocation());
		getPlayersAsRole(PlayerRole.RUNNER).forEach(runner -> runner.teleport(airDropLoc));
		getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.teleport(p.getWorld().getSpawnLocation()));
		Bukkit.broadcastMessage(prefix + "正在设置游戏规则...");
		inGamePlayers.forEach(p -> {
			p.setGameMode(GameMode.SURVIVAL);  //生存模式
			p.setFoodLevel(40);                //饥饿
			p.setHealth(p.getMaxHealth());     //生命
			p.setExp(0.0f);                    //经验
			p.setCompassTarget(p.getWorld().getSpawnLocation());  //指南针指向出生点
			p.getInventory().clear();          //清空物品栏
		});
		if (CompassUnlocked) {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().addItem(new ItemStack(Material.COMPASS, 1)));
		}
		switchWorldRuleForReady(true);
		if (Kit.isEnable()) {
			Bukkit.broadcastMessage("正在发放职业物品");
			inGamePlayers.forEach(player -> {
				player.getInventory().setItem(8, Kit.kitItem);
				for (int i = 0; i < Kit.kitsItems.get(Kit.playerKits.get(player)).size(); i++) {
					ItemStack item = new ItemStack(Material.getMaterial(Kit.kitsItems.get(Kit.playerKits.get(player)).get(i)));
					ItemMeta im = item.getItemMeta();
					im.setUnbreakable(true);  //无法破坏
					player.getInventory().addItem(item);
				}
			});
		}
		if (plugin.getConfig().getBoolean("showRunnerHealth")) {
			inGamePlayers.forEach(player -> runnerHealth.addPlayer(player));
		}
		List<String> runnerBuff = plugin.getConfig().getStringList("runnerBuff.buff");
		List<Integer> runnerLevel = plugin.getConfig().getIntegerList("runnerBuff.level");
		getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> {
			for (int i = 0; i < runnerBuff.size(); i++)
				player.addPotionEffect(new PotionEffect(
						PotionEffectType.getByName(runnerBuff.get(i)),
						0x7FFFFFFF,
						runnerLevel.get(i)
				));
		});
		List<String> hunterBuff = plugin.getConfig().getStringList("hunterBuff.buff");
		List<Integer> hunterLevel = plugin.getConfig().getIntegerList("hunterBuff.level");
		getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> {
			for (int i = 0; i < hunterBuff.size(); i++)
				player.addPotionEffect(new PotionEffect(
						PotionEffectType.getByName(hunterBuff.get(i)),
						0x7FFFFFFF,
						hunterLevel.get(i)
				));
		});
		Bukkit.broadcastMessage(prefix + "游戏开始！");
		for (int i = 0; i < Messages.GameInfo.size(); i++) {
			Bukkit.broadcastMessage(prefix + Messages.GameInfo.get(i).replace("%runner%", String.valueOf(runners)));
		}
		Bukkit.broadcastMessage(prefix + ChatColor.RED + "猎人: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
		Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "逃亡者: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
		status = GameStatus.GAME_STARTED;
		this.registerWatchers();
		plugin.getGame().getProgressManager().unlockProgress(GameProgress.GAME_STARTING, null);
	}
	
	/***
	 * 世界设置
	 *
	 * @param ready 游戏状态 -> ture开始||false等待
	 */
	public void switchWorldRuleForReady(boolean ready) {
		if (ready) {
			Bukkit.getWorlds().forEach(world -> {
				world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);  //昼夜交替
				world.setGameRule(GameRule.DO_MOB_SPAWNING, true);    //怪物生成
				world.setGameRule(GameRule.DO_FIRE_TICK, true);       //火焰蔓延与熄灭
				world.setGameRule(GameRule.MOB_GRIEFING, true);       //怪物破坏
				//world.setDifficulty(difficultyMap.getOrDefault(world, Difficulty.NORMAL));
				Difficulty diff = Difficulty.getByValue(plugin.getConfig().getInt("Difficult"));
				if (diff == null) {
					plugin.getLogger().warning("未知难度，默认为普通模式");
					diff = Difficulty.NORMAL;
					plugin.getConfig().set("Difficult", 1);
					plugin.saveConfig();
				}
				world.setDifficulty(diff);
			});
		} else {
			Bukkit.getWorlds().forEach(world -> {
				world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);  //昼夜交替
				world.setGameRule(GameRule.DO_MOB_SPAWNING, false);    //怪物生成
				world.setGameRule(GameRule.DO_FIRE_TICK, false);       //火焰蔓延与熄灭
				world.setGameRule(GameRule.MOB_GRIEFING, false);       //怪物破坏
				//我觉得这个没必要，在配置文件里设置就行了
				//difficultyMap.put(world, world.getDifficulty());            //储存原本的世界模式
				world.setDifficulty(Difficulty.PEACEFUL);                     //和平
			});
		}
	}
	
	/***
	 * 游戏结束
	 *
	 * @param winner   赢家团队
	 * @param location 传送的坐标
	 */
	public void stop(PlayerRole winner, Location location) {
		this.inGamePlayers.stream().filter(Player::isOnline).forEach(player -> {
			player.setGameMode(GameMode.SPECTATOR);
			player.teleport(location.clone().add(0, 3, 0));
			player.teleport(Util.lookAt(player.getEyeLocation(), location));
		});
		this.status = GameStatus.ENDED;
		Bukkit.broadcastMessage(ChatColor.YELLOW + prefix + "游戏结束! 服务器将在30秒后重新启动！");
		String runnerNames = Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList()));
		String hunterNames = Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList()));
		
		if (winner == PlayerRole.HUNTER) {
			Bukkit.broadcastMessage(ChatColor.GOLD + prefix + "胜利者：猎人");
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + prefix + "恭喜：" + hunterNames);
			getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功击败了逃亡者", 0, 2000, 0));
			getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "不幸阵亡", 0, 2000, 0));
		} else {
			Bukkit.broadcastMessage(ChatColor.GOLD + prefix + "胜利者：逃亡者");
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + prefix + "恭喜：" + runnerNames);
			getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.GOLD + "胜利", "成功战胜了末影龙", 0, 2000, 0));
			getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.RED + "游戏结束", "未能阻止末影龙死亡", 0, 2000, 0));
		}
		new MusicPlayer().playEnding();  //放音乐
		Bukkit.getOnlinePlayers().stream().filter(p -> !inGamePlayers.contains(p)).forEach(p -> p.sendTitle(ChatColor.RED + "游戏结束", "The End", 0, 2000, 0));
		new BukkitRunnable() {
			@Override
			public void run() {
				//开始结算阶段
				StatisticsBaker baker = new StatisticsBaker();
				//计算输出最多的玩家
				getGameEndingData().setDamageOutput(baker.getDamageMaster());
				getGameEndingData().setDamageReceive(baker.getDamageTakenMaster());
				getGameEndingData().setWalkMaster(baker.getWalkingMaster());
				getGameEndingData().setJumpMaster(baker.getJumpMaster());
				getGameEndingData().setTeamKiller(baker.getTeamBadGuy());
				new BukkitRunnable() {
					@Override
					public void run() {
						sendEndingAnimation();
					}
				}.runTaskLaterAsynchronously(plugin, 200);
			}
		}.runTaskLater(MineHunt.getInstance(), 200);
	}
	
	@SneakyThrows
	//结束动画
	private void sendEndingAnimation() {
		double maxCanCost = 20000d;  //消耗时间
		int needShows = 0;
		if (StringUtils.isNotBlank(gameEndingData.getDamageOutput())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getDragonKiller())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getDamageReceive())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getStoneAgePassed())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getRunnerKiller())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getWalkMaster())) {
			needShows++;
		}
		if (StringUtils.isNotBlank(gameEndingData.getJumpMaster())) {
			needShows++;
		}
		maxCanCost /= needShows;
		
		int sleep = (int) maxCanCost;  //每个动画消耗的时间
		
		//屠龙勇士
		if (StringUtils.isNotBlank(gameEndingData.getDragonKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GOLD + plugin.getConfig().getString("DragonKiller"), gameEndingData.getDragonKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//逃亡者杀手
		if (StringUtils.isNotBlank(gameEndingData.getRunnerKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.RED + plugin.getConfig().getString("RunnerKiller"), gameEndingData.getRunnerKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//最佳输出
		if (StringUtils.isNotBlank(gameEndingData.getDamageOutput())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.AQUA + plugin.getConfig().getString("DamageOutPut"), gameEndingData.getDamageOutput(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//最佳受伤
		if (StringUtils.isNotBlank(gameEndingData.getDamageReceive())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.LIGHT_PURPLE + plugin.getConfig().getString("DamageReceive"), gameEndingData.getDamageReceive(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//团队杀手
		if (StringUtils.isNotBlank(gameEndingData.getTeamKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.DARK_RED + plugin.getConfig().getString("TeamKiller"), gameEndingData.getTeamKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//旅行者
		if (StringUtils.isNotBlank(gameEndingData.getWalkMaster())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.YELLOW + plugin.getConfig().getString("WalkMaster"), gameEndingData.getWalkMaster(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//跳跃者
		if (StringUtils.isNotBlank(gameEndingData.getJumpMaster())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GRAY + plugin.getConfig().getString("JumpMaster"), gameEndingData.getJumpMaster(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//结束语
		Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + plugin.getConfig().getString("EndText1"), plugin.getConfig().getString("EndText2"), 0, 20000, 0));
		Thread.sleep(sleep);
		Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + plugin.getConfig().getString("ServerName"), plugin.getConfig().getString("ServerGame"), 0, 20000, 0));
		Thread.sleep(sleep);
		Bukkit.getOnlinePlayers().forEach(Player::resetTitle);
		//重启
		if (AutoRestart) {
			Bukkit.shutdown();
		}
	}
	
	private void registerWatchers() {
		new RadarWatcher();
//        new CompassWatcher();
		new ReconnectWatcher();
		new PlayerMoveWatcher();
	}
	
	public List<Player> getPlayersAsRole(PlayerRole role) {
		return this.roleMapping.entrySet().stream().filter(playerPlayerRoleEntry -> playerPlayerRoleEntry.getValue() == role).map(Map.Entry::getKey).collect(Collectors.toList());
	}
	
	//Code from ManHunt
	
	private Location airDrop(Location spawnpoint) {
		Location loc = spawnpoint.clone();
		loc = new Location(loc.getWorld(), loc.getBlockX(), 0, loc.getBlockZ());
		Random random = new Random();
		loc.add(random.nextInt(XRandom) + XBasic, 0, random.nextInt(YRandom) + YBasic);
		loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
		loc.getBlock().setType(Material.GLASS);
		loc.setY(loc.getY() + 1);
		return loc;
	}
	
	public int getMinPlayers() {
		return minPlayers;
	}
}
