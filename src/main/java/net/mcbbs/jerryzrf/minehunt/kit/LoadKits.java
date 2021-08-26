package net.mcbbs.jerryzrf.minehunt.kit;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class LoadKits {
	public static final String[] kits = {"Runner", "Miner"};
	private final MineHunt plugin = MineHunt.getInstance();
	
	public void Load() {
		File file = new File(plugin.getDataFolder(), "kits.yml");
		if (!file.exists()) {
			plugin.saveResource("kits.yml", false);
			//保存插件根目录下的message.yml到插件目录文件夹里
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		if (config.getInt("version", -1) != MineHunt.getVersionNum()) {
			plugin.getLogger().warning("错误的语言文件版本，已备份并覆盖");
			File newFile = new File(plugin.getDataFolder() + "\\kits_old.yml");
			file.renameTo(newFile);
			plugin.saveResource("kits.yml", false);
		}
		for (String kit : kits) {
			String name = config.getString(kit + ".name", "未能成功加载该职业");
			List<String> lore = config.getStringList(kit + ".lore");
			String material = config.getString(kit + ".material", "AIR");
			Kit.kitsName.add(name);
			Kit.kitsLore.add(lore);
			Kit.kitsMaterial.add(material);
			KitInfo ki = new KitInfo();
			ki.normalCD = config.getInt(kit + ".normal.cd");
			ki.normalDuration = config.getInt(kit + ".normal.duration");
			ki.normalLevel = config.getInt(kit + ".normal.level");
			ki.superCD = config.getInt(kit + ".super.cd");
			ki.superDuration = config.getInt(kit + ".super.duration");
			ki.superLevel = config.getInt(kit + ".super.level");
			Kit.kits.add(ki);
		}
	}
}
