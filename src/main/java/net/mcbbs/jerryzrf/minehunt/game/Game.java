package net.mcbbs.jerryzrf.minehunt.game;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.kit.KitManager;
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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class Game {
	@Getter
	final Map<Player, Double> teamDamageData = new HashMap<>();
	private final MineHunt plugin = MineHunt.getInstance();
	@Getter
	private final Set<Player> inGamePlayers = new HashSet<>();
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
	private GameStatus status = GameStatus.Waiting;
	@Getter
	private final Map<Player, PlayerRole> roleMapping = new HashMap<>();
	@Getter
	private boolean CompassUnlocked = plugin.getConfig().getBoolean("CompassUnlocked");
	@Getter
	private final List<Player> noRolesPlayers = new ArrayList<>();
	@Getter
	private final BossBar runnerHealth = Bukkit.createBossBar(
			new NamespacedKey(plugin, "runnerHealth"),
			null,
			BarColor.GREEN,
			BarStyle.SEGMENTED_10,
			BarFlag.PLAY_BOSS_MUSIC
	);
	@Getter
	private int runners = 1;
	@Getter
	private final Scoreboard teamSB = Bukkit.getScoreboardManager().getNewScoreboard();
	@Getter
	private final Team Hunter = teamSB.registerNewTeam("Hunter");
	@Getter
	private final Team Runner = teamSB.registerNewTeam("Runner");

	public Game() {
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
		Hunter.setDisplayName(plugin.getConfig().getString("HunterName", "??????"));
		Hunter.color(NamedTextColor.RED);
		Hunter.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
		Hunter.setCanSeeFriendlyInvisibles(true);
		Hunter.setPrefix(ChatColor.RED + "[" + plugin.getConfig().getString("HunterName", "??????") + "]");
		Runner.setDisplayName(plugin.getConfig().getString("RunnerName", "?????????"));
		Runner.color(NamedTextColor.GREEN);
		Runner.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
		Runner.setCanSeeFriendlyInvisibles(true);
		Runner.setPrefix(ChatColor.GREEN + "[" + plugin.getConfig().getString("RunnerName", "?????????") + "]");
	}

	public void switchCompass(boolean unlocked) {
		if (this.CompassUnlocked == unlocked) {
			return;
		}
		this.CompassUnlocked = unlocked;
		if (unlocked) {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().addItem(new ItemStack(Material.COMPASS, 1)));
			Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "??????????????????????????????????????????????????????????????????");
		} else {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().remove(Material.COMPASS));
			Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "???????????????????????????????????????????????????????????????");
		}
		getPlayersAsRole(PlayerRole.RUNNER).forEach(p -> p.getInventory().remove(Material.COMPASS)); //?????????????????????????????????
	}
	
	/**
	 * ??????????????????
	 *
	 * @param player ??????
	 * @return ?????????Empty???????????????????????????????????????????????????????????????
	 */
	public Optional<PlayerRole> getPlayerRole(Player player) {
		if (status == GameStatus.Waiting) {
			return Optional.of(PlayerRole.WAITING);
		}
		if (!this.roleMapping.containsKey(player)) {
			return Optional.empty();
		}
		return Optional.of(this.roleMapping.get(player));
	}
	
	/**
	 * ????????????
	 *
	 * @param player ??????
	 * @return ??????????????????
	 */
	public boolean playerJoining(Player player) {
		reconnectTimer.remove(player);
		if (inGamePlayers.size() < maxPlayers) {
			inGamePlayers.add(player);
			return true;
		}
		return false;
	}

	/***
	 * ??????????????????
	 *
	 * @param player ??????
	 * @param role ??????
	 * @return ??????
	 */
	public String playerJoinTeam(Player player, PlayerRole role) {
		if (role == PlayerRole.RUNNER) {
			if (plugin.getGame().getPlayersAsRole(PlayerRole.RUNNER).size() >= plugin.getGame().getRunners()) {
				return ChatColor.RED + "???????????????";
			}
			plugin.getGame().getRoleMapping().put(player, PlayerRole.RUNNER);
			plugin.getGame().getNoRolesPlayers().remove(player);
			if (Hunter.hasEntry(player.getName())) {
				Hunter.removeEntry(player.getName());
			}
			Runner.addEntry(player.getName());
			return "?????????" + ChatColor.GREEN + "?????????";
		} else {
			plugin.getGame().getRoleMapping().put(player, PlayerRole.HUNTER);
			plugin.getGame().getNoRolesPlayers().remove(player);
			if (Runner.hasEntry(player.getName())) {
				Runner.removeEntry(player.getName());
			}
			Hunter.addEntry(player.getName());
			return "?????????" + ChatColor.GREEN + "??????";
		}
	}

	/**
	 * ????????????(?????????)
	 *
	 * @param player ??????
	 */
	public void playerLeaving(Player player) {
		if (status == GameStatus.Waiting) {
			this.inGamePlayers.remove(player);
		} else {
			this.reconnectTimer.put(player, System.currentTimeMillis());
		}
	}
	
	/**
	 * ????????????(?????????)
	 *
	 * @param player ??????
	 */
	public void playerLeft(Player player) {
		this.roleMapping.remove(player);
		this.inGamePlayers.remove(player);
		
		if (getPlayersAsRole(PlayerRole.RUNNER).isEmpty() || getPlayersAsRole(PlayerRole.HUNTER).isEmpty()) {
			Bukkit.broadcastMessage(prefix + Messages.GameExit);
			Bukkit.broadcastMessage(prefix + "?????????????????? 10 ????????????????????????");
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.shutdown();
				}
			}.runTaskLater(plugin, 200);
			return;
		}
		Bukkit.broadcastMessage(prefix + "?????????" + player.getName() + " ???????????????????????????????????????????????????????????????");
		Bukkit.broadcastMessage(prefix + ChatColor.RED + "??????: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
		Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "?????????: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
	}

	/**
	 * ????????????
	 */
	public void start() {
		if (status != GameStatus.Waiting) {
			return;
		}
		//??????????????????
		if (noRolesPlayers.size() > 0) {
			Bukkit.broadcastMessage(prefix + "????????????????????????????????????????????????...");
			Random random = new Random();
			for (int i = 0; i < runners - getPlayersAsRole(PlayerRole.RUNNER).size(); i++) {
				Player selected = noRolesPlayers.get(random.nextInt(noRolesPlayers.size()));
				playerJoinTeam(selected, PlayerRole.RUNNER);
				noRolesPlayers.remove(selected);
			}
			noRolesPlayers.forEach(p -> playerJoinTeam(p, PlayerRole.HUNTER));
		}

		Bukkit.broadcastMessage(prefix + "??????????????????????????????????????????????????????...");
		Location airDropLoc = airDrop(getPlayersAsRole(PlayerRole.RUNNER).get(0).getWorld().getSpawnLocation());
		getPlayersAsRole(PlayerRole.RUNNER).forEach(runner -> runner.teleport(airDropLoc));
		getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.teleport(p.getWorld().getSpawnLocation()));
		Bukkit.broadcastMessage(prefix + "????????????????????????...");
		inGamePlayers.forEach(p -> {
			p.setGameMode(GameMode.SURVIVAL);  //????????????
			p.setFoodLevel(40);                //??????
			p.setHealth(p.getMaxHealth());     //??????
			p.setExp(0.0f);                    //??????
			p.setCompassTarget(p.getWorld().getSpawnLocation());  //????????????????????????
			p.getInventory().clear();          //???????????????
		});
		//???????????????
		if (CompassUnlocked) {
			getPlayersAsRole(PlayerRole.HUNTER).forEach(p -> p.getInventory().addItem(new ItemStack(Material.COMPASS, 1)));
		}
		//??????????????????
		switchWorldRuleForReady(true);
		//????????????????????????
		if (KitManager.isEnable()) {
			Bukkit.broadcastMessage("????????????????????????");
			inGamePlayers.forEach(player -> {
				//???????????????
				ItemMeta im = KitManager.getKitItem().getItemMeta();
				List<String> lore = new ArrayList<>();
				if (im.getLore() != null) {
					im.getLore().forEach(s -> lore.add(s.replace("%s", KitManager.getPlayerKit(player).name)));
					im.setLore(lore);
				}
				KitManager.getKitItem().setItemMeta(im);
				player.getInventory().setItem(8, KitManager.getKitItem());
				//??????????????????
				for (int i = 0; i < KitManager.getKits().get(KitManager.getPlayerKits().get(player.getName())).kitItems.size(); i++) {
					ItemStack item = new ItemStack(Material.getMaterial(
							KitManager.getKits().get(KitManager.getPlayerKits().get(player.getName())).kitItems.get(i)));
					im = item.getItemMeta();
					List<String> itemLore = im.getLore();
					if (itemLore == null) {
						itemLore = List.of("KIT");
					} else {
						itemLore.add("KIT");
					}
					im.setLore(itemLore);
					im.setUnbreakable(true);  //????????????
					item.setItemMeta(im);
					player.getInventory().addItem(item);
				}
			});
		}
		//??????????????????????????????
		if (plugin.getConfig().getBoolean("showRunnerHealth")) {
			runnerHealth.setProgress(1.0);
			inGamePlayers.forEach(runnerHealth::addPlayer);
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
		Bukkit.broadcastMessage(prefix + "???????????????");
		for (int i = 0; i < Messages.GameInfo.size(); i++) {
			Bukkit.broadcastMessage(prefix + Messages.GameInfo.get(i).replace("%runner%", String.valueOf(runners)));
		}
		Bukkit.broadcastMessage(prefix + ChatColor.RED + "??????: " + Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
		Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "?????????: " + Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
		status = GameStatus.Running;
		this.registerWatchers();
		plugin.getGame().getProgressManager().unlockProgress(GameProgress.GAME_STARTING, null);
	}
	
	/***
	 * ????????????
	 *
	 * @param ready ???????????? -> ture??????||false??????
	 */
	public void switchWorldRuleForReady(boolean ready) {
		if (ready) {
			Bukkit.getWorlds().forEach(world -> {
				world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);  //????????????
				world.setGameRule(GameRule.DO_MOB_SPAWNING, true);    //????????????
				world.setGameRule(GameRule.DO_FIRE_TICK, true);       //?????????????????????
				world.setGameRule(GameRule.MOB_GRIEFING, true);       //????????????
				//world.setDifficulty(difficultyMap.getOrDefault(world, Difficulty.NORMAL));
				Difficulty diff = Difficulty.getByValue(plugin.getConfig().getInt("Difficult"));
				if (diff == null) {
					plugin.getLogger().warning("????????????????????????????????????");
					diff = Difficulty.NORMAL;
					plugin.getConfig().set("Difficult", 1);
					plugin.saveConfig();
				}
				world.setDifficulty(diff);
			});
		} else {
			Bukkit.getWorlds().forEach(world -> {
				world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);  //????????????
				world.setGameRule(GameRule.DO_MOB_SPAWNING, false);    //????????????
				world.setGameRule(GameRule.DO_FIRE_TICK, false);       //?????????????????????
				world.setGameRule(GameRule.MOB_GRIEFING, false);       //????????????
				//????????????????????????????????????????????????????????????
				//difficultyMap.put(world, world.getDifficulty());            //???????????????????????????
				world.setDifficulty(Difficulty.PEACEFUL);                     //??????
			});
		}
	}
	
	/***
	 * ????????????
	 *
	 * @param winner   ????????????
	 * @param location ???????????????
	 */
	public void stop(PlayerRole winner, Location location) {
		this.inGamePlayers.stream().filter(Player::isOnline).forEach(player -> {
			player.setGameMode(GameMode.SPECTATOR);
			player.teleport(location.clone().add(0, 3, 0));
			player.teleport(Util.lookAt(player.getEyeLocation(), location));
		});
		this.status = GameStatus.Ending;
		Bukkit.broadcastMessage(ChatColor.YELLOW + prefix + "????????????! ???????????????30?????????????????????");
		String runnerNames = Util.list2String(getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList()));
		String hunterNames = Util.list2String(getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList()));
		
		if (winner == PlayerRole.HUNTER) {
			Bukkit.broadcastMessage(ChatColor.GOLD + prefix + "??????????????????");
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + prefix + "?????????" + hunterNames);
			getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.GOLD + "??????", "????????????????????????", 0, 2000, 0));
			getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.RED + "????????????", "????????????", 0, 2000, 0));
		} else {
			Bukkit.broadcastMessage(ChatColor.GOLD + prefix + "?????????????????????");
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + prefix + "?????????" + runnerNames);
			getPlayersAsRole(PlayerRole.RUNNER).forEach(player -> player.sendTitle(ChatColor.GOLD + "??????", "????????????????????????", 0, 2000, 0));
			getPlayersAsRole(PlayerRole.HUNTER).forEach(player -> player.sendTitle(ChatColor.RED + "????????????", "???????????????????????????", 0, 2000, 0));
		}
		new MusicPlayer().playEnding();  //?????????
		Bukkit.getOnlinePlayers().stream().filter(p -> !inGamePlayers.contains(p)).forEach(p -> p.sendTitle(ChatColor.RED + "????????????", "The End", 0, 2000, 0));
		new BukkitRunnable() {
			@Override
			public void run() {
				//??????????????????
				StatisticsBaker baker = new StatisticsBaker();
				//???????????????????????????
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
	//????????????
	private void sendEndingAnimation() {
		double maxCanCost = 20000d;  //????????????
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
		
		int sleep = (int) maxCanCost;  //???????????????????????????
		
		//????????????
		if (StringUtils.isNotBlank(gameEndingData.getDragonKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GOLD + plugin.getConfig().getString("DragonKiller"), gameEndingData.getDragonKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//???????????????
		if (StringUtils.isNotBlank(gameEndingData.getRunnerKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.RED + plugin.getConfig().getString("RunnerKiller"), gameEndingData.getRunnerKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//????????????
		if (StringUtils.isNotBlank(gameEndingData.getDamageOutput())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.AQUA + plugin.getConfig().getString("DamageOutPut"), gameEndingData.getDamageOutput(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//????????????
		if (StringUtils.isNotBlank(gameEndingData.getDamageReceive())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.LIGHT_PURPLE + plugin.getConfig().getString("DamageReceive"), gameEndingData.getDamageReceive(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//????????????
		if (StringUtils.isNotBlank(gameEndingData.getTeamKiller())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.DARK_RED + plugin.getConfig().getString("TeamKiller"), gameEndingData.getTeamKiller(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//?????????
		if (StringUtils.isNotBlank(gameEndingData.getWalkMaster())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.YELLOW + plugin.getConfig().getString("WalkMaster"), gameEndingData.getWalkMaster(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//?????????
		if (StringUtils.isNotBlank(gameEndingData.getJumpMaster())) {
			Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GRAY + plugin.getConfig().getString("JumpMaster"), gameEndingData.getJumpMaster(), 0, 20000, 0));
			Thread.sleep(sleep);
		}
		//?????????
		Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + plugin.getConfig().getString("EndText1"), plugin.getConfig().getString("EndText2"), 0, 20000, 0));
		Thread.sleep(sleep);
		Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(ChatColor.GREEN + plugin.getConfig().getString("ServerName"), plugin.getConfig().getString("ServerGame"), 0, 20000, 0));
		Thread.sleep(sleep);
		Bukkit.getOnlinePlayers().forEach(Player::resetTitle);
		//??????
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
