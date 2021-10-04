package net.mcbbs.jerryzrf.minehunt.config;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import net.mcbbs.jerryzrf.minehunt.api.Kit;
import net.mcbbs.jerryzrf.minehunt.api.KitMode;
import net.mcbbs.jerryzrf.minehunt.kit.GUI;
import net.mcbbs.jerryzrf.minehunt.kit.KitManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
		KitManager.kitItem = new ItemStack(Material.getMaterial(config.getString("material", "NETHER_STAR")));
		KitManager.setEnable(config.getBoolean("enable", true));
		List<Map<?, ?>> kitList = config.getMapList("kits");
		ItemMeta im = KitManager.kitItem.getItemMeta();
		im.setLore(config.getStringList("lore"));
		KitManager.kitItem.setItemMeta(im);
		for (Map<?, ?> map : kitList) {
			Kit ki = new Kit();
			ki.name = (String) map.get("name");
			((List<String>) map.get("lore")).forEach(s -> ki.lore.add(s.replace("%s", ki.name)));
			ki.material = (String) map.get("material");
			ki.buff = (List<String>) map.get("buff");
			List<Map<?, ?>> mode = (List<Map<?, ?>>) map.get("mode");
			ki.permission = (String) map.get("permission");
			ki.kitItems = (List<String>) map.get("item");
			for (Map<?, ?> value : mode) {
				KitMode km = new KitMode();
				km.name = (String) value.get("name");
				km.CD = (int) value.get("cd");
				km.duration = (List<Integer>) value.get("duration");
				km.level = (List<Integer>) value.get("level");
				ki.mode.add(km);
			}
			KitManager.kits.add(ki);
		}
	}
}
