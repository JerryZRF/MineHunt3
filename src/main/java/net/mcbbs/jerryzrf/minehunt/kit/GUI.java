package net.mcbbs.jerryzrf.minehunt.kit;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUI {
	@Setter
	private static int grid = 36;
	
	public void openGUI(Player player) {
		Inventory inv = Bukkit.createInventory(player, grid, "职业");
		for (int i = 0; i < Kit.kits.size(); i++) {
			KitInfo ki = Kit.kits.get(i);
			ItemStack item = new ItemStack(Material.getMaterial(ki.material));
			ItemMeta im = item.getItemMeta();
			if (im == null) {
				continue;
			}
			ki.lore.add(player.hasPermission(ki.permission) ? ChatColor.GREEN + "已拥有" : ChatColor.RED + "未拥有");
			im.setLore(ki.lore);
			im.setDisplayName(ki.name);
			item.setItemMeta(im);
			inv.setItem(i, item);
		}
		player.openInventory(inv);
	}
}
