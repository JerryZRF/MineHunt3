package net.mcbbs.jerryzrf.minehunt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class Messages {
    public static String WarnDistanceSafe;
    public static String WarnDistanceClose;
    public static String resetCountdown;
    public static String runnerNether;
    public static String runnerTheEnd;
    public static String prefix;
    public static String GameExit;
    public static String HunterHurtDragon;
    public static String NoCompass;
    public static String GameFull;
    public static String GameStart;
    public static String Waiting;
    public static String PlayerEnough;
    public static String NoPermission;
    public static String UnknownTeam;
    public static String DifferentWorld;
    public static List<String> GameInfo;
    public static String FindRunner;
    
    private final MineHunt plugin = MineHunt.getInstance();
    
    public void LoadMessage() {
        File file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            plugin.saveResource("message.yml", false);
            //保存插件根目录下的message.yml到插件目录文件夹里
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        if (config.getInt("version", -1) != MineHunt.getVersionNum()) {
            plugin.getLogger().warning("错误的语言文件版本，已备份并覆盖");
            File newFile = new File(plugin.getDataFolder() + "\\message_old.yml");
            file.renameTo(newFile);
            plugin.saveResource("message.yml", false);
        }
        
        WarnDistanceSafe = config.getString("WarnDistanceSafe");
        WarnDistanceClose = config.getString("WarnDistanceClose");
        resetCountdown = config.getString("resetCountdown");
        runnerNether = config.getString("runnerNether");
        runnerTheEnd = config.getString("runnerTheEnd");
        prefix = config.getString("prefix");
        GameExit = config.getString("GameExit");
        HunterHurtDragon = config.getString("HunterHurtDragon");
        NoCompass = config.getString("NoCompass");
        GameFull = config.getString("GameFull");
        GameStart = config.getString("GameStart");
        Waiting = config.getString("Waiting");
        PlayerEnough = config.getString("PlayerOK");
        NoPermission = config.getString("NoPermission");
        UnknownTeam = config.getString("UnknownTeam");
        DifferentWorld = config.getString("DifferentWorld");
        GameInfo = config.getStringList("GameInfo");
        FindRunner = config.getString("FindRunner");
    }
}
