package net.mcbbs.jerryzrf.minehunt.config;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.kit.GUI;
import net.mcbbs.jerryzrf.minehunt.kit.Kit;
import net.mcbbs.jerryzrf.minehunt.kit.KitInfo;
import net.mcbbs.jerryzrf.minehunt.kit.KitMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LoadKits {
	private final MineHunt plugin = MineHunt.getInstance();
	
	public void Load() {
		File file = new File(plugin.getDataFolder(), "kits.yml");
		if (!file.exists()) {
			plugin.saveResource("kits.yml", false);
			//保存插件根目录下的kits.yml到插件目录文件夹里
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		if (config.getInt("version", -1) != MineHunt.getVersionNum()) {
			plugin.getLogger().warning("错误的语言文件版本，已备份并覆盖");
			File newFile = new File(plugin.getDataFolder() + "\\kits_old.yml");
			file.renameTo(newFile);
			plugin.saveResource("kits.yml", false);
		}
		GUI.setGrid(config.getInt("grid", 36));
		Kit.kitItem = new ItemStack(Material.getMaterial(config.getString("material", "NETHER_STAR")));
		Kit.setEnable(config.getBoolean("enable", true));
		List<Map<?, ?>> kitList = config.getMapList("kits");
		for (Map<?, ?> map : kitList) {
			KitInfo ki = new KitInfo();
			ki.name = (String) map.get("name");
			ki.lore = (List<String>) map.get("lore");
			ki.material = (String) map.get("material");
			ki.buff = (List<String>) map.get("buff");
			List<Map<?, ?>> mode = (List<Map<?, ?>>) map.get("mode");
			Kit.kitsItems.add((List<String>) map.get("item"));
			for (Map<?, ?> value : mode) {
				KitMode km = new KitMode();
				km.name = (String) value.get("name");
				km.CD = (int) value.get("cd");
				km.duration = (List<Integer>) value.get("duration");
				km.level = (List<Integer>) value.get("level");
				ki.mode.add(km);
			}
			Kit.kits.add(ki);
		}
	}
}
