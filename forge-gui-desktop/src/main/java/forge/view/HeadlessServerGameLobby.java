package forge.view;

import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.server.ServerGameLobby;

import java.util.Collections;

/**
 * Headless server game lobby that automatically includes AI opponent.
 * Slot 0: Remote human player (connecting from GUI)
 * Slot 1: AI opponent (always present)
 */
public class HeadlessServerGameLobby extends ServerGameLobby {
    
    public HeadlessServerGameLobby() {
        super();
        // Remove the default OPEN slot from parent
        // Parent adds: Slot 0 = LOCAL, Slot 1 = OPEN
        // We want: Slot 0 = OPEN (for remote human), Slot 1 = AI
        
        // Clear all slots and recreate with our configuration
        // Add safety limit to prevent infinite loop if removeSlot fails silently
        int maxAttempts = 10;
        int attempts = 0;
        while (getNumberOfSlots() > 0 && attempts < maxAttempts) {
            removeSlot(0);
            attempts++;
            if (attempts >= maxAttempts) {
                System.err.println("WARNING: Failed to remove all slots after " + maxAttempts + " attempts");
                break;
            }
        }
        
        // Slot 0: OPEN for remote human player
        addSlot(new LobbySlot(LobbySlotType.OPEN, null, -1, -1, 0, false, false, Collections.emptySet()));
        
        // Slot 1: AI opponent (always ready)
        final LobbySlot aiSlot = new LobbySlot(LobbySlotType.AI, "AI Opponent", 0, 0, 1, true, false, Collections.emptySet());
        addSlot(aiSlot);
        
        System.out.println("HeadlessServerGameLobby initialized with AI opponent in slot 1");
    }
    
    @Override
    public int connectPlayer(final String name, final int avatarIndex, final int sleeveIndex) {
        // Only allow connection to slot 0 (human player slot)
        final LobbySlot slot = getSlot(0);
        if (slot != null && slot.getType() == LobbySlotType.OPEN) {
            slot.setType(LobbySlotType.REMOTE);
            slot.setName(name);
            slot.setAvatarIndex(avatarIndex);
            slot.setSleeveIndex(sleeveIndex);
            slot.setIsReady(false);
            updateView(false);
            System.out.println("Player '" + name + "' connected to headless server (slot 0)");
            return 0;
        }
        System.err.println("Failed to connect player '" + name + "' - slot 0 not available");
        return -1;
    }
    
    @Override
    public void disconnectPlayer(final int index) {
        if (index == 0) {
            final LobbySlot slot = getSlot(0);
            if (slot != null) {
                slot.setType(LobbySlotType.OPEN);
                slot.setName(null);
                slot.setIsReady(false);
                updateView(false);
                System.out.println("Player disconnected from headless server (slot 0)");
            }
        }
        // Never disconnect the AI in slot 1
    }
    
    @Override
    public boolean mayRemove(final int index) {
        // Only protect the AI slot (slot 1) from removal
        return index != 1;
    }
    
    @Override
    public boolean mayEdit(final int index) {
        // Don't allow editing the AI slot
        if (index == 1) {
            return false;
        }
        return super.mayEdit(index);
    }
}
