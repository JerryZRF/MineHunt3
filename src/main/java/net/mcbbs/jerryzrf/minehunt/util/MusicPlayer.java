package net.mcbbs.jerryzrf.minehunt.util;

import lombok.SneakyThrows;
import net.mcbbs.jerryzrf.minehunt.MineHunt;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;


public class MusicPlayer {
    @SneakyThrows
    public void playEnding() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.MUSIC_DISC_WAIT, 1.0f, 1.0f));
            }
        }.runTaskLater(MineHunt.getInstance(), 1);
    }
}
