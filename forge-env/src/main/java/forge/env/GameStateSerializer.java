package forge.env;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Serializes game state to JSON format.
 */
class GameStateSerializer {
    private final Game game;
    private final Gson gson;

    public GameStateSerializer(Game game) {
        this.game = game;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Convert the game state to JSON.
     *
     * @return JSON string representation of the game state
     */
    public String toJson() {
        JsonObject root = new JsonObject();

        // Add players array
        JsonArray playersArray = new JsonArray();
        for (Player player : game.getPlayers()) {
            playersArray.add(serializePlayer(player));
        }
        root.add("players", playersArray);

        // Add active player ID
        Player activePlayer = game.getPhaseHandler().getPlayerTurn();
        int activePlayerId = game.getPlayers().indexOf(activePlayer) + 1;
        root.addProperty("activePlayerId", activePlayerId);

        // Add current phase
        PhaseType currentPhase = game.getPhaseHandler().getPhase();
        root.addProperty("phase", getPhaseDisplayName(currentPhase));

        // Add turn number
        root.addProperty("turn", game.getPhaseHandler().getTurn());

        return gson.toJson(root);
    }

    private JsonObject serializePlayer(Player player) {
        JsonObject playerObj = new JsonObject();

        // Player ID (1-indexed)
        int playerId = game.getPlayers().indexOf(player) + 1;
        playerObj.addProperty("id", playerId);

        // Player name
        playerObj.addProperty("name", player.getName());

        // Life total
        playerObj.addProperty("life", player.getLife());

        // Deck name (from registered player)
        String deckName = player.getRegisteredPlayer() != null && player.getRegisteredPlayer().getDeck() != null
                ? player.getRegisteredPlayer().getDeck().getName()
                : "Unknown Deck";
        playerObj.addProperty("deckName", deckName);

        // Library count (hidden information)
        int libraryCount = player.getZone(ZoneType.Library).size();
        playerObj.addProperty("libraryCount", libraryCount);

        // Hand
        JsonArray handArray = new JsonArray();
        for (Card card : player.getZone(ZoneType.Hand)) {
            handArray.add(serializeCard(card));
        }
        playerObj.add("hand", handArray);

        // Graveyard
        JsonArray graveyardArray = new JsonArray();
        for (Card card : player.getZone(ZoneType.Graveyard)) {
            graveyardArray.add(serializeCard(card));
        }
        playerObj.add("graveyard", graveyardArray);

        // Exile
        JsonArray exileArray = new JsonArray();
        for (Card card : player.getZone(ZoneType.Exile)) {
            exileArray.add(serializeCard(card));
        }
        playerObj.add("exile", exileArray);

        // Battlefield
        JsonArray battlefieldArray = new JsonArray();
        for (Card card : player.getZone(ZoneType.Battlefield)) {
            battlefieldArray.add(serializeCard(card));
        }
        playerObj.add("battlefield", battlefieldArray);

        return playerObj;
    }

    private JsonObject serializeCard(Card card) {
        JsonObject cardObj = new JsonObject();

        // Card instance ID
        cardObj.addProperty("id", card.getId());

        // Card name
        cardObj.addProperty("cardName", card.getName());

        return cardObj;
    }

    private String getPhaseDisplayName(PhaseType phase) {
        if (phase == null) {
            return "Unknown";
        }

        switch (phase) {
            case UNTAP:
                return "Untap";
            case UPKEEP:
                return "Upkeep";
            case DRAW:
                return "Draw";
            case MAIN1:
                return "Main Phase 1";
            case COMBAT_BEGIN:
                return "Beginning of Combat";
            case COMBAT_DECLARE_ATTACKERS:
                return "Declare Attackers";
            case COMBAT_DECLARE_BLOCKERS:
                return "Declare Blockers";
            case COMBAT_FIRST_STRIKE_DAMAGE:
                return "First Strike Damage";
            case COMBAT_DAMAGE:
                return "Combat Damage";
            case COMBAT_END:
                return "End of Combat";
            case MAIN2:
                return "Main Phase 2";
            case END_OF_TURN:
                return "End of Turn";
            case CLEANUP:
                return "Cleanup";
            default:
                return phase.toString();
        }
    }
}
