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
	public static Map<Player, Long> useKitTime = new HashMap<>();
	public static Map<Player, Integer> lastMode = new HashMap<>();
	public static Map<Player, Integer> mode = new HashMap<>();
	public static ItemStack kitItem = new ItemStack(Material.NETHER_STAR);
	
	public static void Init() {
		kitItem.setDisplayName("职业工具");
	}
}
