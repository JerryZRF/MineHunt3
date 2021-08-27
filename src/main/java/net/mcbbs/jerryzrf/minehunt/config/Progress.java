package net.mcbbs.jerryzrf.minehunt.config;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.game.GameProgressManager;
import net.mcbbs.jerryzrf.minehunt.game.ProgressInfo;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Progress {
	public final static String[] progressList = {"STONE_AGE", "IRON_MINED", "COMPASS_UNLOCKED", "KILLED_DRAGON", "ENTER_NETHER", "GET_BLAZE_ROD", "GET_ENDER_PERAL", "ENTER_END"};
	private final static MineHunt plugin = MineHunt.getInstance();
	
	public static void Load() {
		File file = new File(plugin.getDataFolder(), "progress.yml");
		if (!file.exists()) {
			plugin.saveResource("progress.yml", false);
			//保存插件根目录下的message.yml到插件目录文件夹里
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		if (config.getInt("version", -1) != MineHunt.getVersionNum()) {
			plugin.getLogger().warning("错误的语言文件版本，已备份并覆盖");
			File newFile = new File(plugin.getDataFolder() + "\\progress_old.yml");
			file.renameTo(newFile);
			plugin.saveResource("progress.yml", false);
		}
		for (String p : progressList) {
			ProgressInfo pi = new ProgressInfo();
			List<Map<?, ?>> item = config.getMapList(p + ".item");
			for (Map<?, ?> value : item) {
				pi.itemMaterial.add((String) value.get("material"));
				pi.itemNum.add((Integer) value.get("num"));
			}
			List<Map<?, ?>> buff = config.getMapList(p + ".buff");
			for (Map<?, ?> value : buff) {
				pi.buffType.add((String) value.get("type"));
				pi.buffTime.add((Integer) value.get("time"));
				pi.buffLevel.add((Integer) value.get("level"));
			}
			GameProgressManager.progressInfo.put(p, pi);
		}
	}
}
