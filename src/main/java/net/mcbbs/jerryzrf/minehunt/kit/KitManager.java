package net.mcbbs.jerryzrf.minehunt.kit;

import lombok.Getter;
import lombok.Setter;
import net.mcbbs.jerryzrf.minehunt.api.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        ItemMeta im = kitItem.getItemMeta();
        List<String> lore = im.getLore();
        if (lore != null) {
            lore.add("KIT");
        } else {
            lore = List.of("KIT");
        }
        im.setLore(lore);
        kitItem.setItemMeta(im);
        kitItem.setDisplayName("职业工具");
    }

    public static Kit getPlayerKit(Player player) {
        return kits.get(playerKits.get(player.getName()));
    }

    public static void giveKitItems(Player player) {
        for (int i = 0; i < KitManager.kits.get(KitManager.playerKits.get(player.getName())).kitItems.size(); i++) {
            ItemStack item = new ItemStack(Material.getMaterial(KitManager.getPlayerKit(player).kitItems.get(i)));
            ItemMeta im = item.getItemMeta();
            im.setLore(List.of("KIT"));
            im.setUnbreakable(true);  //无法破坏
            item.setItemMeta(im);
            player.getInventory().addItem(item);
        }
    }

    public static void clearKitItems(Player player) {
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items) {
            if (item.getItemMeta().getLore() != null && item.getItemMeta().getLore().contains("KIT")) {
                player.getInventory().remove(item);
            }
        }
    }
}
