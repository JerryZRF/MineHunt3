package net.mcbbs.jerryzrf.minehunt;

import net.mcbbs.jerryzrf.minehunt.api.GameStatus;
import net.mcbbs.jerryzrf.minehunt.api.PlayerRole;
import net.mcbbs.jerryzrf.minehunt.config.LoadKits;
import net.mcbbs.jerryzrf.minehunt.config.LoadProgress;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.kit.KitManager;
import net.mcbbs.jerryzrf.minehunt.kit.kitGUI;
import net.mcbbs.jerryzrf.minehunt.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commander implements TabExecutor {
	private final MineHunt plugin = MineHunt.getInstance();
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		/*
		 查看帮助
		 */
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
			sender.sendMessage("=====================MineHunt3=====================");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt kits " + ChatColor.AQUA + "打开职业菜单");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt copyright " + ChatColor.AQUA + "版权声明");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt tp {player}   " + ChatColor.AQUA + "旁观者传送");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt players   " + ChatColor.AQUA + "查看玩家");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt forcestart   " + ChatColor.AQUA + "强制开始");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt reload   " + ChatColor.AQUA + "重载配置文件");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt resetcountdown   " + ChatColor.AQUA + "重置倒计时");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt resetkit  " + ChatColor.AQUA + "重置职业倒计时");
			sender.sendMessage(ChatColor.YELLOW + "/minehunt join Hunter|Runner  " + ChatColor.AQUA + "强制加入");
			return true;
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
        /*
         强制加入已经开始的游戏
         权限: minehunt.join
         */
		if (args[0].equalsIgnoreCase("join")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "只有玩家才可以这么做！");
				return true;
			}
			if (plugin.getGame().getStatus() != GameStatus.Running) {
				sender.sendMessage(ChatColor.RED + "游戏不在运行中！");
				return true;
			}
			if (sender.hasPermission("minehunt.join")) {
				Player player = (Player) sender;
				if (args.length == 1) {
					player.sendMessage(Messages.UnknownTeam);
					return false;
				}
				if (args[1].equalsIgnoreCase("hunter")) {
					plugin.getGame().getRoleMapping().put(player, PlayerRole.HUNTER);
				} else if (args[1].equalsIgnoreCase("runner")) {
					plugin.getGame().getRoleMapping().put(player, PlayerRole.RUNNER);
				} else {
					player.sendMessage(Messages.UnknownTeam);
					return false;
				}
				plugin.getGame().getInGamePlayers().add(player);
				player.setGameMode(GameMode.SURVIVAL);
				Bukkit.broadcastMessage("玩家 " + sender.getName() + " 强制加入了游戏！ 身份：" + args[1]);
				if (KitManager.isEnable()) {
					KitManager.clearKitItems((Player) sender);
					kitGUI.openGUI((Player) sender);
				}
			} else {
				sender.sendMessage(Messages.NoPermission);
			}
			return true;
		}
        /*
         重置倒计时
         权限：minehunt.resetcountdown
         */
		if (args[0].equalsIgnoreCase("resetcountdown")) {
			if (plugin.getGame().getStatus() == GameStatus.Waiting) {
				if (sender.hasPermission("minehunt.reset")) {
					plugin.getCountDownWatcher().resetCountdown();
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
			Bukkit.broadcastMessage(ChatColor.YELLOW + "> 猎人 & 逃亡者 <");
			Bukkit.broadcastMessage(ChatColor.RED + "猎人: " + Util.list2String(MineHunt.getInstance().getGame().getPlayersAsRole(PlayerRole.HUNTER).stream().map(Player::getName).collect(Collectors.toList())));
			Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者: " + Util.list2String(MineHunt.getInstance().getGame().getPlayersAsRole(PlayerRole.RUNNER).stream().map(Player::getName).collect(Collectors.toList())));
			return true;
		}
        /*
         强制开始游戏
         权限：minehunt.start
         */
		if (args[0].equalsIgnoreCase("forcestart")) {
			if (plugin.getGame().getStatus() == GameStatus.Waiting) {
				if (sender.hasPermission("minehunt.start")) {
					if (plugin.getGame().getInGamePlayers().size() < 1) {
						sender.sendMessage("人数不足，至少需要2人");
						return true;
					}
					plugin.getGame().start();
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
				return true;
			}
			if (plugin.getGame().getStatus() == GameStatus.Running || plugin.getGame().getStatus() == GameStatus.Ending) {
				if (plugin.getGame().getPlayerRole((Player) sender).isEmpty()) {
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
			return true;
		}
		/*
		 选择职业
		 权限：无
	  	 */
		if (args[0].equalsIgnoreCase("kits")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "只有玩家才可以这么做！");
				return true;
			}
			if (plugin.getGame().getStatus() != GameStatus.Waiting) {
				sender.sendMessage(ChatColor.RED + "游戏已开始！");
				return true;
			}
			if (!KitManager.isEnable()) {
				sender.sendMessage(ChatColor.RED + "服务器已禁用该功能！");
				return true;
			}
			kitGUI.openGUI((Player) sender);
			return true;
		}
		/*
		 重置职业CD
		 权限： minehunt.resetkit
		 */
		if (args[0].equalsIgnoreCase("resetkit")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "只有玩家才可以这么做！");
				return true;
			}
			if (sender.hasPermission("minehunt.resetkit")) {
				KitManager.getUseKitTime().put(sender.getName(), 0L);
				sender.sendMessage(ChatColor.GOLD + "职业CD已归零");
			} else {
				sender.sendMessage(Messages.NoPermission);
			}
			return true;
		}
		/*
		 重载配置文件
		 权限：minehunt.reload
		 */
		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("minehunt.reload")) {
				sender.sendMessage(Messages.NoPermission);
				return true;
			}
			plugin.saveDefaultConfig();
			plugin.reloadConfig();
			Messages.LoadMessage();
			LoadKits.Load();
			LoadProgress.Load();
			sender.sendMessage("重载完成！");
			return true;
		}
		return false;
	}
	
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		final String[] teams = {"hunter", "runner"};
		final String[] commands = {"help", "forcestart", "players", "copyright", "resetcountdown", "join", "tp", "kits", "resetkit", "reload"};
		//列出游戏中玩家名称列表
		final List<String> players = new ArrayList<>();
		plugin.getGame().getInGamePlayers().forEach((player) -> players.add(player.getName()));
		if (args.length == 1) {
			return Arrays.asList(commands);
		}
		if (args.length >= 3) {
			return null;
		}
		return switch (args[0]) {
			case "join" -> Arrays.asList(teams);
			case "tp" -> players;
			default -> null;
		};
	}
}
