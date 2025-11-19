package forge.env;

import forge.CardStorageReader;
import forge.LobbyPlayer;
import forge.StaticData;
import forge.deck.Deck;
import forge.deck.io.DeckStorage;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.util.MyRandom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Headless MTG Environment Wrapper for Forge.
 * Provides programmatic access to initialize and run MTG games without GUI.
 */
public final class ForgeEnv {
    private static final String DEFAULT_RES_DIR = "forge-gui/res";
    private static StaticData staticData;
    private static List<Deck> standardDecks;
    private static String resourceDirectory;
    private static final Random random = MyRandom.getRandom();
    private static boolean initialized = false;

    private ForgeEnv() {
        // Utility class, no instantiation
    }

    /**
     * Initialize the Forge environment.
     * Loads card database and prebuilt Standard deck metadata.
     * Must be called once before any newGame() calls.
     */
    public static void initialize() {
        initialize(DEFAULT_RES_DIR);
    }

    /**
     * Initialize the Forge environment with a custom resource directory.
     *
     * @param resDir Path to the Forge resource directory (e.g., "forge-gui/res")
     */
    public static void initialize(String resDir) {
        if (initialized) {
            return; // Already initialized
        }

        resourceDirectory = resDir;

        // Initialize card database
        initializeStaticData();

        // Load Standard prebuilt decks
        loadStandardDecks();

        initialized = true;
    }

    /**
     * Create and initialize a new 1v1 Standard game with random prebuilt decks.
     *
     * @return JSON string representation of the initial game state
     */
    public static String newGame() {
        if (!initialized) {
            throw new IllegalStateException("ForgeEnv must be initialized before calling newGame()");
        }

        // Select two random Standard decks
        Deck deck1 = selectRandomDeck();
        Deck deck2 = selectRandomDeck();

        // Ensure we select different decks if possible
        while (deck2.getName().equals(deck1.getName()) && standardDecks.size() > 1) {
            deck2 = selectRandomDeck();
        }

        // Create the game
        Game game = createGame(deck1, deck2);

        // Serialize to JSON
        GameStateSerializer serializer = new GameStateSerializer(game);
        return serializer.toJson();
    }

    private static void initializeStaticData() {
        File resDir = new File(resourceDirectory);
        if (!resDir.exists()) {
            throw new IllegalStateException("Resource directory not found: " + resourceDirectory);
        }

        File cardDataDir = new File(resDir, "cardsfolder");
        File editionsDir = new File(resDir, "editions");
        File blockDataDir = new File(resDir, "blockdata");

        CardStorageReader cardReader = new CardStorageReader(cardDataDir.getAbsolutePath(), null, false);

        staticData = new StaticData(
                cardReader,
                null, // No custom cards
                editionsDir.getAbsolutePath(),
                "", // No custom editions
                blockDataDir.getAbsolutePath(),
                "", // No card art preference
                false, // Don't enable unknown cards
                true // Load non-legal cards for broader compatibility
        );
    }

    private static void loadStandardDecks() {
        File preconDir = new File(resourceDirectory, "quest/precons");
        if (!preconDir.exists() || !preconDir.isDirectory()) {
            throw new IllegalStateException("Precon directory not found: " + preconDir.getAbsolutePath());
        }

        DeckStorage deckStorage = new DeckStorage(preconDir, resourceDirectory);
        standardDecks = new ArrayList<>();

        // Load all prebuilt decks - IStorage extends Iterable
        for (Deck deck : (Iterable<Deck>) deckStorage) {
            // Filter for Standard-legal decks (this is a simplified check)
            // In a production system, you'd verify against format legality
            if (deck != null && isStandardDeck(deck)) {
                standardDecks.add(deck);
            }
        }

        if (standardDecks.isEmpty()) {
            throw new IllegalStateException("No Standard decks found in " + preconDir.getAbsolutePath());
        }
    }

    private static boolean isStandardDeck(Deck deck) {
        // For V1, we'll accept all precon decks
        // In the future, this should verify against Standard format legality
        return deck.getMain() != null && !deck.getMain().isEmpty();
    }

    private static Deck selectRandomDeck() {
        int index = random.nextInt(standardDecks.size());
        return standardDecks.get(index);
    }

    private static Game createGame(Deck deck1, Deck deck2) {
        // Create lobby players
        LobbyPlayer lobby1 = new SimpleLobbyPlayer("Player 1");
        LobbyPlayer lobby2 = new SimpleLobbyPlayer("Player 2");

        // Create registered players with decks
        RegisteredPlayer rp1 = new RegisteredPlayer(deck1);
        rp1.setPlayer(lobby1);

        RegisteredPlayer rp2 = new RegisteredPlayer(deck2);
        rp2.setPlayer(lobby2);

        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(rp1);
        players.add(rp2);

        // Create game rules for Standard constructed
        GameRules rules = new GameRules(GameType.Constructed);
        rules.setGamesPerMatch(1);
        rules.setManaBurn(false);

        // Create match
        Match match = new Match(rules, players, "Headless Standard Match");

        // Create and start game - controllers are created automatically by SimpleLobbyPlayer
        Game game = match.createGame();

        // Start the game
        match.startGame(game);

        return game;
    }

    /**
     * Get the number of available Standard decks.
     *
     * @return Number of loaded Standard decks
     */
    public static int getStandardDeckCount() {
        return standardDecks != null ? standardDecks.size() : 0;
    }

    /**
     * Check if the environment is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset the environment (for testing purposes).
     */
    static void reset() {
        initialized = false;
        staticData = null;
        standardDecks = null;
    }
}
