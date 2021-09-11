package net.mcbbs.jerryzrf.minehunt.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IGetGameInfo {
    /**
     * 获取玩家的阵容
     *
     * @param player 玩家
     * @return 玩家阵容
     */
    Optional<PlayerRole> getPlayerRole(Player player);

    /**
     * 获取一个阵容的所有玩家
     *
     * @param role 阵容
     * @return 阵容的所有玩家
     */
    List<Player> getPlayersAsRole(PlayerRole role);

    /**
     * 获取在游戏内的玩家
     *
     * @return 玩家列表
     */
    Set<Player> inGamePlayers();

    /***
     * 返回游戏状态
     *
     * @return 游戏状态
     */
    GameStatus getGameStatus();
}
