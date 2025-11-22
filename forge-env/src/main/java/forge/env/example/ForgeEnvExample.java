package forge.env.example;

import forge.env.ForgeEnv;

/**
 * Example usage of the Forge Headless Environment.
 * 
 * This demonstrates how to:
 * 1. Initialize the environment
 * 2. Create games
 * 3. Work with the JSON output
 */
public final class ForgeEnvExample {
    
    private ForgeEnvExample() {
        // Utility class
    }
    
    public static void main(String[] args) {
        // Example 1: Basic usage
        basicUsage();
        
        // Example 2: Multiple games
        multipleGames();
    }
    
    /**
     * Basic usage: initialize and create a single game.
     */
    public static void basicUsage() {
        System.out.println("=== Basic Usage Example ===\n");
        
        // Initialize with default resource path (forge-gui/res)
        // For custom path, use: ForgeEnv.initialize("/path/to/resources");
        ForgeEnv.initialize();
        
        System.out.println("Environment initialized");
        System.out.println("Number of available decks: " + ForgeEnv.getStandardDeckCount());
        
        // Create a new game
        String gameJson = ForgeEnv.newGame();
        
        System.out.println("\nGame state JSON:");
        System.out.println(gameJson);
        System.out.println();
    }
    
    /**
     * Create multiple games to demonstrate reusability.
     */
    public static void multipleGames() {
        System.out.println("=== Multiple Games Example ===\n");
        
        // Ensure environment is initialized
        if (!ForgeEnv.isInitialized()) {
            ForgeEnv.initialize();
        }
        
        // Create three games
        for (int i = 1; i <= 3; i++) {
            System.out.println("Creating game " + i + "...");
            String json = ForgeEnv.newGame();
            
            // Extract just the deck names from JSON (simple string parsing for demo)
            String[] lines = json.split("\n");
            for (String line : lines) {
                if (line.contains("\"deckName\"")) {
                    System.out.println("  " + line.trim());
                }
            }
            System.out.println();
        }
    }
}
