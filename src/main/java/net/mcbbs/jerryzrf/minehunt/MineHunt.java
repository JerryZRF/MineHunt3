package net.mcbbs.jerryzrf.minehunt;

import lombok.Getter;
import net.mcbbs.jerryzrf.minehunt.config.LoadKits;
import net.mcbbs.jerryzrf.minehunt.config.LoadProgress;
import net.mcbbs.jerryzrf.minehunt.config.Messages;
import net.mcbbs.jerryzrf.minehunt.game.Game;
import net.mcbbs.jerryzrf.minehunt.kit.Kit;
import net.mcbbs.jerryzrf.minehunt.listener.*;
import net.mcbbs.jerryzrf.minehunt.placeholder.placeholder;
import net.mcbbs.jerryzrf.minehunt.watcher.CountDownWatcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class MineHunt extends JavaPlugin {
	@Getter
	private final static int versionNum = 4;
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
		new LoadKits().Load();
		Kit.Init();
		getLogger().info("职业文件加载完成！");
		LoadProgress.Load();
		getLogger().info("进度文件加载完成！");
		
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
		Bukkit.getPluginManager().registerEvents(new PlayerItemListener(), this);
		Bukkit.getPluginManager().registerEvents(new ProgressDetectingListener(), this);
		Bukkit.getPluginManager().registerEvents(new GameWinnerListener(), this);
		Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
		getLogger().info("事件注册完成！");
		if (Bukkit.getPluginCommand("minehunt") != null) {
			Bukkit.getPluginCommand("minehunt").setExecutor(new Commander());
		}
		Objects.requireNonNull(Bukkit.getPluginCommand("minehunt")).setTabCompleter(new Commander());
		getLogger().info("命令注册完成！");
		
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
				System.out.println("地图清理完成");
			}));
		}
	}
	
	/***
	 * 删除文件/文件夹
	 *
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
}