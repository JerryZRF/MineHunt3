package net.mcbbs.jerryzrf.minehunt.kit;

import lombok.Getter;
import lombok.Setter;
import net.mcbbs.jerryzrf.minehunt.api.Kit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitManager {
    public static Map<String, Integer> playerKits = new HashMap<>();
    public static List<Kit> kits = new ArrayList<>();
    public static Map<String, Long> useKitTime = new HashMap<>();
    public static Map<String, Integer> lastMode = new HashMap<>();
    public static Map<String, Integer> mode = new HashMap<>();
    public static ItemStack kitItem;
    @Setter
    @Getter
    private static boolean enable;

    public static void Init() {
        kitItem.setDisplayName("职业工具");
    }

    public static Kit getPlayerKit(Player player) {
        return kits.get(playerKits.get(player.getName()));
    }
}
