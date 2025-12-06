package forge.env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * Represents a single game instance with step-by-step progression capability.
 */
public class GameInstance {
    private final Game game;
    private final GameStateSerializer serializer;
    
    public GameInstance(Game game) {
        this.game = game;
        this.serializer = new GameStateSerializer(game);
    }
    
    /**
     * Get current game state as JSON.
     * 
     * @return JSON string of current game state
     */
    public String getState() {
        return serializer.toJson();
    }
    
    /**
     * Check if the game has ended.
     * 
     * @return true if game is over
     */
    public boolean isTerminal() {
        return game.isGameOver();
    }
    
    /**
     * Get the winner of the game (if terminal).
     * 
     * @return player ID of winner, or -1 if no winner or game not over
     */
    public int getWinner() {
        if (!game.isGameOver() || game.getOutcome() == null) {
            return -1;
        }
        
        if (game.getOutcome().getWinningPlayer() == null) {
            return -1;
        }
        
        // Get the index of the winning registered player
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getRegisteredPlayer() == game.getOutcome().getWinningPlayer()) {
                return i + 1;
            }
        }
        
        return -1;
    }
    
    /**
     * Get all valid actions for the active player.
     * Returns a JSON string with available actions.
     * 
     * @return JSON string of valid actions
     */
    public String getValidActions() {
        JsonObject result = new JsonObject();
        JsonArray actions = new JsonArray();
        
        Player activePlayer = game.getPhaseHandler().getPlayerTurn();
        if (activePlayer == null) {
            result.add("actions", actions);
            return result.toString();
        }
        
        int actionId = 0;
        
        // Get playable cards from hand
        for (Card card : activePlayer.getCardsIn(ZoneType.Hand)) {
            FCollectionView<SpellAbility> abilities = card.getAllSpellAbilities();
            for (SpellAbility sa : abilities) {
                if (sa.canPlay()) {
                    JsonObject action = new JsonObject();
                    action.addProperty("id", actionId++);
                    action.addProperty("type", "play_spell");
                    action.addProperty("cardId", card.getId());
                    action.addProperty("cardName", card.getName());
                    action.addProperty("description", sa.getDescription());
                    actions.add(action);
                }
            }
        }
        
        // Get activated abilities from battlefield
        for (Card card : activePlayer.getCardsIn(ZoneType.Battlefield)) {
            FCollectionView<SpellAbility> abilities = card.getAllSpellAbilities();
            for (SpellAbility sa : abilities) {
                if (!sa.isSpell() && sa.canPlay()) {
                    JsonObject action = new JsonObject();
                    action.addProperty("id", actionId++);
                    action.addProperty("type", "activate_ability");
                    action.addProperty("cardId", card.getId());
                    action.addProperty("cardName", card.getName());
                    action.addProperty("description", sa.getDescription());
                    actions.add(action);
                }
            }
        }
        
        // Always include pass priority option
        JsonObject passAction = new JsonObject();
        passAction.addProperty("id", actionId++);
        passAction.addProperty("type", "pass_priority");
        passAction.addProperty("description", "Pass priority");
        actions.add(passAction);
        
        result.add("actions", actions);
        result.addProperty("activePlayerId", game.getPlayers().indexOf(activePlayer) + 1);
        result.addProperty("phase", serializer.getPhaseDisplayName(game.getPhaseHandler().getPhase()));
        result.addProperty("turn", game.getPhaseHandler().getTurn());
        
        return result.toString();
    }
    
    /**
     * Execute an action and progress the game.
     * 
     * @param actionJson JSON string describing the action to take
     * @return JSON string of new game state after action
     */
    public String step(String actionJson) {
        // For V1, we'll implement a simple action parser
        // Action format: {"type": "pass_priority"} or {"type": "play_spell", "cardId": 123}
        
        try {
            JsonParser parser = new JsonParser();
            JsonObject action = parser.parse(actionJson).getAsJsonObject();
            
            String actionType = action.get("type").getAsString();
            
            if ("pass_priority".equals(actionType)) {
                // Pass priority by choosing no spell ability
                Player activePlayer = game.getPhaseHandler().getPlayerTurn();
                if (activePlayer != null) {
                    activePlayer.getController().playChosenSpellAbility(null);
                }
                
            } else if ("play_spell".equals(actionType) || "activate_ability".equals(actionType)) {
                int cardId = action.get("cardId").getAsInt();
                
                Player activePlayer = game.getPhaseHandler().getPlayerTurn();
                
                // Find the card by ID in game
                Card targetCard = null;
                for (Card card : game.getCardsInGame()) {
                    if (card.getId() == cardId) {
                        targetCard = card;
                        break;
                    }
                }
                
                if (targetCard != null && activePlayer != null) {
                    // Find first playable spell ability
                    FCollectionView<SpellAbility> abilities = targetCard.getAllSpellAbilities();
                    for (SpellAbility sa : abilities) {
                        if (sa.canPlay()) {
                            // Play the spell ability
                            activePlayer.getController().playChosenSpellAbility(sa);
                            break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // If action parsing fails, just pass priority
            Player activePlayer = game.getPhaseHandler().getPlayerTurn();
            if (activePlayer != null) {
                activePlayer.getController().playChosenSpellAbility(null);
            }
        }
        
        // Return new state
        return getState();
    }
    
    /**
     * Get the underlying Game object.
     * 
     * @return the Game instance
     */
    public Game getGame() {
        return game;
    }
}
