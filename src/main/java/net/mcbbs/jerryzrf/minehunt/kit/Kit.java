package net.mcbbs.jerryzrf.minehunt.kit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {
	public static Map<Player, Integer> playerKits = new HashMap<>();
	public static List<KitInfo> kits = new ArrayList<>();
	public static List<String> kitsName = new ArrayList<>();
	public static List<List<String>> kitsLore = new ArrayList<>();
	public static List<String> kitsMaterial = new ArrayList<>();
	public static Map<Player, Long> useKitTime = new HashMap<>();
	public static boolean lastMode = false;  //0 普通 | 1超级
	public static boolean mode = false;  //0 普通 | 1超级
	public static ItemStack kitItem = new ItemStack(Material.NETHER_STAR);
	
	public static void Init() {
		kitItem.setDisplayName("职业工具");
	}
}
