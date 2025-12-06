package forge.env;

import forge.LobbyPlayer;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

/**
 * Simple implementation of LobbyPlayer for headless environment.
 */
class SimpleLobbyPlayer extends LobbyPlayer implements IGameEntitiesFactory {
    public SimpleLobbyPlayer(String name) {
        super(name);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // No-op in headless mode
    }

    @Override
    public Player createIngamePlayer(Game game, int id) {
        Player player = new Player(getName(), game, id);
        PlayerControllerAi controller = new PlayerControllerAi(game, player, this);
        player.runWithController(() -> player.setFirstController(controller), controller);
        return player;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new PlayerControllerAi(master.getGame(), slave, this);
    }
}
