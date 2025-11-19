package forge.env;

import forge.LobbyPlayer;

/**
 * Simple implementation of LobbyPlayer for headless environment.
 */
class SimpleLobbyPlayer extends LobbyPlayer {
    public SimpleLobbyPlayer(String name) {
        super(name);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // No-op in headless mode
    }
}
