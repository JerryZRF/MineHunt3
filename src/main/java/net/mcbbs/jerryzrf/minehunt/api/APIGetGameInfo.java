package net.mcbbs.jerryzrf.minehunt.api;

import net.mcbbs.jerryzrf.minehunt.MineHunt;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class APIGetGameInfo implements IGetGameInfo {
    private final MineHunt plugin = MineHunt.getInstance();

    @Override
    public Optional<PlayerRole> getPlayerRole(Player player) {
        return plugin.getGame().getPlayerRole(player);
    }

    @Override
    public List<Player> getPlayersAsRole(PlayerRole role) {
        return plugin.getGame().getPlayersAsRole(role);
    }

    @Override
    public Set<Player> inGamePlayers() {
        return plugin.getGame().getInGamePlayers();
    }

    @Override
    public GameStatus getGameStatus() {
        return plugin.getGame().getStatus();
    }
}
