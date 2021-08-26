package net.mcbbs.jerryzrf.minehunt.kit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static net.mcbbs.jerryzrf.minehunt.kit.LoadKits.kits;

public class GUI {
	public void openGUI(Player player) {
		Inventory inv = Bukkit.createInventory(player, 9, "职业");
		for (int i = 0; i < kits.length; i++) {
			ItemStack item = new ItemStack(Material.getMaterial(Kit.kitsMaterial.get(i)));
			ItemMeta im = item.getItemMeta();
			im.setLore(Kit.kitsLore.get(i));
			item.setItemMeta(im);
			inv.setItem(i, item);
		}
		player.openInventory(inv);
	}
}
