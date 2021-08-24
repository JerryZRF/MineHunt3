package net.mcbbs.jerryzrf.minehunt;

import lombok.Getter;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.game.Game;
import net.mcbbs.jerryzrf.minehunt.game.GameStatus;
import net.mcbbs.jerryzrf.minehunt.game.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.listener.*;
import net.mcbbs.jerryzrf.minehunt.placeholder.placeholder;
import net.mcbbs.jerryzrf.minehunt.util.Util;
import net.mcbbs.jerryzrf.minehunt.watcher.CountDownWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.stream.Collectors;

public final class MineHunt extends JavaPlugin {
	@Getter
	private final static int versionNum = 1;
	@Getter
	private static MineHunt instance;
	@Getter
	private Game game;
	@Getter
	private CountDownWatcher countDownWatcher;
	
	@Override
	public void onLoad() {
		instance = this;
	}
	
	@Override
	public void onEnable() {
		// Plugin startup logic
		getLogger().info("正在加载MineHunt3插件");
		
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		new Messages().LoadMessage();
		getLogger().info("语言文件加载完成！");
		
		game = new Game();
		countDownWatcher = new CountDownWatcher();
		
		Plugin pluginPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
		if (pluginPlaceholderAPI != null) {
			getLogger().info("检测到PlaceHolderAPI插件，变量功能已启用！");
			new placeholder(this).register();
		}
		
		game.switchWorldRuleForReady(false);
		Bukkit.getPluginManager().registerEvents(new PlayerServerListener(), this);
		Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(), this);
		Bukkit.getPluginManager().registerEvents(new PlayerCompassListener(), this);
		Bukkit.getPluginManager().registerEvents(new ProgressDetectingListener(), this);
		Bukkit.getPluginManager().registerEvents(new GameWinnerListener(), this);
		Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
		getLogger().info("事件注册完成！");
		
		if (getConfig().getInt("version", -1) != versionNum) {
			getLogger().warning("错误的配置文件版本，已备份并覆盖");
			File newFile = new File(getDataFolder() + "\\config_old.yml");
			File oldFile = new File(getDataFolder(), "config.yml");
			oldFile.renameTo(newFile);
			saveDefaultConfig();
		}
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
		if (getConfig().getBoolean("changeMap")) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				File world = new File(getDataFolder(), "..\\..\\world");
				DeleteFile(world);
				world = new File(getDataFolder(), "..\\..\\world_nether");
				DeleteFile(world);
				world = new File(getDataFolder(), "..\\..\\world_the_end");
				DeleteFile(world);
			}));
		}
	}
	
	/***
	 * 删除文件/文件夹
	 * @param file 文件
	 */
	public void DeleteFile(File file) {
		if (file.isDirectory()) {
			//是目录
			if (file.delete()) {
				return;
			}
			File[] files = file.listFiles();
			for (File value : files) {
				DeleteFile(value);
			}
		} else {
			//是文件
			file.delete();
		}
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		//格式错误
		if (args.length < 1) {
			return false;
		}
		
		//禁止删除本行版权声明
		//墨守吐槽：如果有人想在我这搞分支就顺着往下写就好了~
		if (args[0].equalsIgnoreCase("copyright")) {
			sender.sendMessage("Copyright - Minecraft of gamerteam. 版权所有.");
			sender.sendMessage("Fork by MossCG 这是墨守的分支版本~");
			sender.sendMessage("Fork by JerryZRF 这是JerryZRF的分支版本");
			return true;
		}
		
		//不安全命令 完全没做检查，确认你会用再执行
		//墨守吐槽：挺安全的起码我没用出啥问题，有空我改改2333
		//强制加入已经开始的游戏，需要权限minehunt.join
        /*
         强制加入已经开始的游戏
         权限: minehunt.join
         */
		if (args[0].equalsIgnoreCase("join")) {
			if (sender.hasPermission("minehunt.join")) {
				Player player = (Player) sender;
				this.getGame().getInGamePlayers().add(player);
				if (args.length == 1) {
					player.sendMessage(Messages.UnknownTeam);
				}
				if (args[1].equalsIgnoreCase("hunter")) {
					this.getGame().getRoleMapping().put(player, PlayerRole.HUNTER);
				} else if (args[1].equalsIgnoreCase("runner")) {
					this.getGame().getRoleMapping().put(player, PlayerRole.RUNNER);
				} else {
					player.sendMessage(Messages.UnknownTeam);
				}
				player.setGameMode(GameMode.SURVIVAL);
				Bukkit.broadcastMessage("玩家 " + sender.getName() + " 强制加入了游戏！ 身份：" + args[0]);
			} else {
				sender.sendMessage(Messages.NoPermission);
			}
			return true;
		}
        /*
         重置倒计时
         权限：minehunt.reset
         */
		if (args[0].equalsIgnoreCase("resetcountdown")) {
			if (this.getGame().getStatus() == GameStatus.WAITING_PLAYERS) {
				if (sender.hasPermission("minehunt.reset")) {
					this.getCountDownWatcher().resetCountdown();
				} else {
					sender.sendMessage(Messages.NoPermission);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "游戏已开始！");
			}
			return true;
		}
        /*
         查看玩家分组
         权限：无
         */
		if (args[0].equalsIgnoreCase("players")) {
			if (getGame().getStatus() == GameStatus.GAME_STARTED) {
				Bukkit.broadcastMessage(ChatColor.YELLOW + "> 猎人 & 逃亡者 <");
				Bukkit.broadcastMessage(ChatColor.RED + "猎人: " + Util.list2String(MineHunt.getInstance().getGame().getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
				Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(MineHunt.getInstance().getGame().getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
			}
			return true;
		}
        /*
         强制开始游戏
         权限：minehunt.start
         */
		if (args[0].equalsIgnoreCase("forcestart")) {
			if (this.getGame().getStatus() == GameStatus.WAITING_PLAYERS) {
				if (sender.hasPermission("minehunt.start")) {
					if (game.getInGamePlayers().size() <= 1) {
						sender.sendMessage("人数不足，至少需要2人");
						return true;
					}
					game.start();
				} else {
					sender.sendMessage(Messages.NoPermission);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "游戏已开始！");
			}
			return true;
		}
		/*
		 旁观者传送到指定玩家
		 权限：无
		*/
		if (args[0].equalsIgnoreCase("tp")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "只有玩家才可以这样做！");
			}
			if (this.getGame().getStatus() == GameStatus.GAME_STARTED) {
				if (this.getGame().getPlayerRole((Player) sender).isEmpty()) {
					//是旁观者
					if (args.length == 1) {
						//语法错误
						return false;
					}
					Player player = Bukkit.getPlayer(args[1]);
					if (player == null) {
						sender.sendMessage(ChatColor.RED + "不存在指定玩家！");
					}
					((Player) sender).teleport(player);
				} else {
					sender.sendMessage(ChatColor.RED + "只有旁观者才可以这么做！");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "游戏尚未开始，你不能这么做！");
			}
		}
		return false;
	}
}
