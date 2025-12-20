package forge.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileSection;
import forge.util.FileUtil;
import java.io.File;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.ai.ComputerUtilAbility;
import forge.ai.AIAgentClient;

import forge.GuiDesktop;
import forge.Singletons;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.util.ImageFetcher;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.item.PaperCard;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiGame;
import forge.gamemodes.match.HostedMatch;
import org.jupnp.UpnpServiceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.SwingUtilities;

import forge.game.event.*;
import forge.game.combat.Combat;
import forge.game.GameEntity;
import java.util.UUID;

import forge.StaticData;

import forge.deck.DeckgenUtil;
import forge.util.Aggregates;

public class ForgeHeadless {
    // ANSI Color Constants
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BOLD = "\u001B[1m";

    // Server & State
    private static final int PORT = 8081;
    private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private static volatile Game currentGame = null;
    private static volatile String currentPromptType = "none"; // "action", "target", "none"
    private static volatile JsonObject currentPromptData = new JsonObject();

    // AI Agent Configuration
    private static volatile String aiAgentEndpoint = null;
    private static volatile String gameId = null;
    private static volatile AIAgentClient aiAgentClient = null;
    private static volatile boolean condensedLogging = false;
    private static volatile String customDeckName = null;
    private static final java.util.Set<String> knownCardNames = new java.util.HashSet<>();

    public static void main(String[] args) {
        System.err.println("DEBUG: ForgeHeadless main started");

        // Parse command-line arguments
        boolean player1IsHuman = true; // default
        boolean player2IsHuman = false; // default
        boolean verboseLogging = false; // default
        boolean useGui = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--both-human")) {
                player1IsHuman = true;
                player2IsHuman = true;
            } else if (arg.equals("--both-ai")) {
                player1IsHuman = false;
                player2IsHuman = false;
            } else if (arg.equals("--p1-ai")) {
                player1IsHuman = false;
            } else if (arg.equals("--p2-human")) {
                player2IsHuman = true;
            } else if (arg.equals("--verbose")) {
                verboseLogging = true;
            } else if (arg.equals("--gui") || arg.equals("--watch-game")) {
                useGui = true;
            } else if (arg.equals("--condensed-log")) {
                condensedLogging = true;
            } else if ((arg.equals("--use-deck") || arg.equals("--deck")) && i + 1 < args.length) {
                customDeckName = args[++i];
            } else if (arg.startsWith("--use-deck=")) {
                customDeckName = arg.substring("--use-deck=".length());
            } else if (arg.startsWith("--deck=")) {
                customDeckName = arg.substring("--deck=".length());
            } else if (arg.equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (arg.equals("--ai-endpoint") && i + 1 < args.length) {
                aiAgentEndpoint = args[++i];
            } else if (arg.startsWith("--ai-endpoint=")) {
                aiAgentEndpoint = arg.substring("--ai-endpoint=".length());
            } else if (arg.equals("--game-id") && i + 1 < args.length) {
                gameId = args[++i];
            } else if (arg.startsWith("--game-id=")) {
                gameId = arg.substring("--game-id=".length());
            }
        }

        // Initialize AI Agent Client if endpoint is configured
        if (aiAgentEndpoint != null && !aiAgentEndpoint.isEmpty()) {
            aiAgentClient = new AIAgentClient(aiAgentEndpoint);
            System.out.println("AI Agent mode enabled. Endpoint: " + aiAgentEndpoint);
            if (gameId == null) {
                gameId = UUID.randomUUID().toString();
            }
            System.out.println("Game ID: " + gameId);
        }

        // Start HTTP Server (still needed for fallback and monitoring)
        startHttpServer();

        if (useGui) {
            GuiBase.setInterface(new GuiDesktop());
            Singletons.initializeOnce(true);
            Singletons.getControl().initialize();
        } else {
            GuiBase.setInterface(new HeadlessGui());
            FModel.initialize(null, null);
        }

        // Generate Decks
        Deck deck1 = null;
        
        // Check for custom deck name
        if (customDeckName != null && !customDeckName.isEmpty()) {
            System.out.println("Loading custom deck: " + customDeckName);
            deck1 = loadPreconstructedDeck(customDeckName);
            if (deck1 == null) {
                System.err.println("WARNING: Could not load deck '" + customDeckName + "'. Falling back to random.");
            }
        }
        
        if (deck1 == null) {
            deck1 = loadRandomPrecon();
        }
        
        if (deck1 == null) {
            System.err.println("FATAL ERROR: Could not load any preconstructed decks from " + ForgeConstants.QUEST_PRECON_DIR);
            System.exit(1);
        }
        System.out.println("Loaded Deck 1: " + deck1.getName());

        Deck deck2 = loadRandomPrecon();
        if (deck2 == null) {
            System.err.println("FATAL ERROR: Could not load any preconstructed decks from " + ForgeConstants.QUEST_PRECON_DIR);
            System.exit(1);
        }
        System.out.println("Loaded Deck 2: " + deck2.getName());

        // Setup Players based on configuration
        List<RegisteredPlayer> players = new ArrayList<>();

        if (player1IsHuman) {
            RegisteredPlayer rp1 = new RegisteredPlayer(deck1).setPlayer(new HeadlessLobbyPlayer("Player 1"));
            // rp1.setStartingLife(1000); // Only for debugging
            players.add(rp1);
        } else {
            RegisteredPlayer rp1 = new RegisteredPlayer(deck1)
                    .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 1", null));
            players.add(rp1);
        }

        if (player2IsHuman) {
            RegisteredPlayer rp2 = new RegisteredPlayer(deck2).setPlayer(new HeadlessLobbyPlayer("Player 2"));
            players.add(rp2);
        } else {
            RegisteredPlayer rp2 = new RegisteredPlayer(deck2)
                    .setPlayer(new forge.ai.LobbyPlayerAi("AI Player 2", null));
            players.add(rp2);
        }

        System.err.println("DEBUG: Player 1 - " + (player1IsHuman ? "Human" : "AI"));
        System.err.println("DEBUG: Player 2 - " + (player2IsHuman ? "Human" : "AI"));

        // Setup Match
        GameRules rules = new GameRules(GameType.Constructed);
        if (useGui) {
            System.out.println("Launching GUI Match...");
            HostedMatch hostedMatch = new HostedMatch();
            Singletons.getControl().addMatch(hostedMatch);
            
            // Start match with no local human GUIs (spectator mode)
            SwingUtilities.invokeLater(() -> {
                hostedMatch.startMatch(rules, null, players, new java.util.HashMap<RegisteredPlayer, IGuiGame>(), null);
            });
            
            // Wait a bit for game to initialize to set currentGame reference
            new Thread(() -> {
                try {
                    while (hostedMatch.getGame() == null) {
                        Thread.sleep(100);
                    }
                    currentGame = hostedMatch.getGame();
                    System.out.println("GUI Match started. Game ID captured.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } else {
            Match match = new Match(rules, players, "Headless Match");
            Game game = match.createGame();
            currentGame = game;

            runGame(match, game, player1IsHuman, player2IsHuman, verboseLogging);
        }
    }

    private static void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // GET /state
            server.createContext("/state", exchange -> {
                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 204, "");
                    return;
                }
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                if (currentGame == null) {
                    sendResponse(exchange, 503, "Game not started");
                    return;
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String response = gson.toJson(extractGameState(currentGame));
                sendResponse(exchange, 200, response);
            });

            // GET /input
            server.createContext("/input", exchange -> {
                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 204, "");
                    return;
                }
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("type", currentPromptType);
                response.add("data", currentPromptData);
                sendResponse(exchange, 200, response.toString());
            });

            // POST /action
            server.createContext("/action", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("index")) {
                        int index = json.get("index").getAsInt();
                        inputQueue.offer("play_action " + index);
                        sendResponse(exchange, 200, "Action queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'index' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            // POST /target
            server.createContext("/target", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("index")) {
                        int index = json.get("index").getAsInt();
                        inputQueue.offer(String.valueOf(index));
                        sendResponse(exchange, 200, "Target selection queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'index' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            // POST /control
            server.createContext("/control", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = readRequestBody(exchange);
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("command")) {
                        String cmd = json.get("command").getAsString();
                        inputQueue.offer(cmd);
                        sendResponse(exchange, 200, "Command queued");
                    } else {
                        sendResponse(exchange, 400, "Missing 'command' field");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Invalid JSON");
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("HTTP Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to start HTTP server. Exiting.");
            System.exit(1);
        }
    }

    private static Deck generateRandomStandardDeck() {
        try {
            int count = Aggregates.randomInt(1, 3);
            List<String> colors = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                colors.add("Random " + i);
            }
            Deck deck = DeckgenUtil.buildColorDeck(colors, FModel.getFormats().getStandard().getFilterPrinted(), true);

            // Validate the deck was actually generated with cards
            if (deck == null || deck.getMain().isEmpty()) {
                System.err.println("Generated deck is null or empty!");
                return null;
            }

            System.out.println("Successfully generated random deck with " + deck.getMain().countAll() + " cards");
            return deck;
        } catch (Exception e) {
            System.err.println("Error generating random deck: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Deck loadPreconstructedDeck(String deckName) {
        if (deckName == null) return null;
        
        // First, check the test-decks directory (for development/testing)
        File testDir = new File("test-decks");
        Deck testDeck = loadDeckFromDirectory(testDir, deckName);
        if (testDeck != null) {
            System.out.println("Loaded test deck from: " + testDir.getAbsolutePath());
            return testDeck;
        }
        
        // Then check the quest precons directory
        File preconDir = new File(ForgeConstants.QUEST_PRECON_DIR);
        Deck preconDeck = loadDeckFromDirectory(preconDir, deckName);
        if (preconDeck != null) {
            return preconDeck;
        }
        
        System.err.println("Could not find deck '" + deckName + "' in test-decks/ or quest precons");
        return null;
    }
    
    private static Deck loadDeckFromDirectory(File dir, String deckName) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        // Try exact match first (with and without .dck extension)
        File[] files = dir.listFiles((d, name) -> name.endsWith(".dck"));
        if (files != null) {
            for (File f : files) {
                String nameWithoutExt = f.getName().replace(".dck", "");
                if (nameWithoutExt.equalsIgnoreCase(deckName) || 
                    f.getName().equalsIgnoreCase(deckName) ||
                    f.getName().equalsIgnoreCase(deckName + ".dck")) {
                    try {
                        Deck deck = DeckSerializer.fromSections(FileSection.parseSections(FileUtil.readFile(f)));
                        if (deck != null) {
                            deck.setName(nameWithoutExt);
                            return deck;
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading deck " + f.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    private static Deck loadRandomPrecon() {
        File dir = new File(ForgeConstants.QUEST_PRECON_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
             System.err.println("Precon directory not found: " + dir.getAbsolutePath());
             return null;
        }
        
        // Recursive search for all .dck files
        List<File> allDecks = new ArrayList<>();
        findDeckFiles(dir, allDecks);
        
        if (allDecks.isEmpty()) {
             System.err.println("No .dck files found in " + dir.getAbsolutePath());
             return null;
        }
        
        // Try up to 10 times to find a valid constructed deck (>= 60 cards)
        for (int i = 0; i < 10; i++) {
            File randomDeckFile = allDecks.get(Aggregates.randomInt(0, allDecks.size() - 1));
            try {
                Deck d = DeckSerializer.fromSections(FileSection.parseSections(FileUtil.readFile(randomDeckFile)));
                // Ensure deck is a valid constructed deck (approx 60 cards)
                // Exclude Draft/Sealed (40) and Commander (100)
                if (d != null && d.getMain().countAll() >= 60 && d.getMain().countAll() <= 80) {
                    d.setName(randomDeckFile.getName().replace(".dck", ""));
                    return d;
                }
            } catch (Exception e) {
                System.err.println("Error loading deck " + randomDeckFile.getName() + ": " + e.getMessage());
            }
        }
        
        // Fallback: just return the last attempted valid deck even if small, or null
        return null;
    }

    private static void findDeckFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    findDeckFiles(f, results);
                } else if (f.getName().endsWith(".dck")) {
                    results.add(f);
                }
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        // Add CORS headers to allow browser access from file:// and other origins
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }

    private static void printUsage() {
        System.out.println("ForgeHeadless - Headless Magic: The Gathering Game Engine");
        System.out.println("\nUsage: java -cp <jar> forge.view.ForgeHeadless [options]");
        System.out.println("\nPlayer Options:");
        System.out.println("  --both-human    Both players are human-controlled (interactive)");
        System.out.println("  --both-ai       Both players are AI-controlled (simulation mode)");
        System.out.println("  --p1-ai         Player 1 is AI-controlled (default: human)");
        System.out.println("  --p2-human      Player 2 is human-controlled (default: AI)");
        System.out.println("\nAI Agent Options:");
        System.out.println("  --ai-endpoint <url>   URL of external AI agent for decision-making");
        System.out.println("  --game-id <id>        Unique game ID for tracking (auto-generated if not provided)");
        System.out.println("\nOther Options:");
        System.out.println("  --verbose       Enable verbose logging of game events");
        System.out.println("  --condensed-log Enable condensed logging (actions, state, decisions)");
        System.out.println("  --deck <name>   Load a specific deck from test-decks/ or precons");
        System.out.println("  --help          Show this help message");
        System.out.println("\nHTTP Server running on port " + PORT);
        System.out.println("\nAI Agent Mode:");
        System.out.println("  When --ai-endpoint is provided, the game will call out to the specified");
        System.out.println("  endpoint for all player decisions instead of waiting for HTTP input.");
        System.out.println("  The endpoint receives game state + action options and returns decisions.");
    }

    private static void initialize() {
        // FModel.initialize() is called in main
    }

    private static void runGame(Match match, Game game, boolean player1IsHuman, boolean player2IsHuman,
            boolean verboseLogging) {
        // Start Game
        if (verboseLogging) {
            HeadlessGameObserver observer = new HeadlessGameObserver();
            match.subscribeToEvents(observer);
            game.subscribeToEvents(observer);
        }
        match.startGame(game);
    }

    private static JsonObject getCardDefinition(String cardName) {
        JsonObject def = new JsonObject();
        PaperCard pc = StaticData.instance().getCommonCards().getCard(cardName);
        if (pc == null) {
            def.addProperty("name", cardName);
            def.addProperty("error", "Card not found in DB");
            return def;
        }

        def.addProperty("name", pc.getName());
        def.addProperty("mana_cost", pc.getRules().getManaCost().toString());
        def.addProperty("type", pc.getRules().getType().toString());
        def.addProperty("oracle_text", pc.getRules().getOracleText());
        
        if (pc.getRules().getType().isCreature()) {
            def.addProperty("power", pc.getRules().getPower().toString());
            def.addProperty("toughness", pc.getRules().getToughness().toString());
        }

        return def;
    }

    private static void addVisibleCardsToKnownSet(Game game, Player player) {
        // Player 1 Zones (Our Agent)
        for (Card c : player.getCardsIn(ZoneType.Hand)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
        for (Card c : player.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());

        // Player 2 Zones (Visible)
        Player opponent = player.getSingleOpponent(); 
        if (opponent != null) {
            for (Card c : opponent.getCardsIn(ZoneType.Graveyard)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Battlefield)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Exile)) knownCardNames.add(c.getName());
            for (Card c : opponent.getCardsIn(ZoneType.Command)) knownCardNames.add(c.getName());
        }

        // Stack
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            Card source = stackItem.getSpellAbility().getHostCard();
            if (source != null) {
                knownCardNames.add(source.getName());
            }
        }
    }

    private static JsonObject extractGameState(Game game) {
        // Find Player 1 (the one we are controlling/reporting for)
        Player player = null;
        for (Player p : game.getPlayers()) {
            if (p.getController() instanceof forge.ai.PlayerControllerRemote) {
                player = p;
                break;
            }
        }
        
        if (player != null) {
            addVisibleCardsToKnownSet(game, player);
        }

        JsonObject state = new JsonObject();

        // Add definitions for all known cards
        JsonObject cardDefinitions = new JsonObject();
        for (String cardName : knownCardNames) {
            cardDefinitions.add(cardName, getCardDefinition(cardName));
        }
        state.add("card_definitions", cardDefinitions);

        // General Game Info
        state.addProperty("turn", game.getPhaseHandler().getTurn());
        state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
        state.addProperty("activePlayerId", game.getPhaseHandler().getPlayerTurn().getId());
        state.addProperty("priorityPlayerId", game.getPhaseHandler().getPlayerTurn().getId()); // Approximate

        // Stack - show what spells/abilities are on the stack
        JsonArray stackArray = new JsonArray();
        for (forge.game.spellability.SpellAbilityStackInstance stackItem : game.getStack()) {
            JsonObject stackObj = new JsonObject();
            SpellAbility sa = stackItem.getSpellAbility();
            Card source = sa.getHostCard();
            stackObj.addProperty("card_name", source != null ? source.getName() : "Unknown");
            stackObj.addProperty("card_id", source != null ? source.getId() : -1);
            stackObj.addProperty("description", sa.getStackDescription());
            stackObj.addProperty("controller", sa.getActivatingPlayer().getName());
            stackArray.add(stackObj);
        }
        state.add("stack", stackArray);
        state.addProperty("stack_size", game.getStack().size());

        // Players
        JsonArray playersArray = new JsonArray();
        for (Player p : game.getPlayers()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("id", p.getId());
            playerObj.addProperty("name", p.getName());
            playerObj.addProperty("life", p.getLife());
            playerObj.addProperty("libraryCount", p.getCardsIn(ZoneType.Library).size());

            // Hand
            JsonArray handArray = new JsonArray();
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                JsonObject cardObj = new JsonObject();
                cardObj.addProperty("name", c.getName());
                cardObj.addProperty("id", c.getId());
                cardObj.addProperty("zone", "Hand");
                handArray.add(cardObj);
            }
            playerObj.add("hand", handArray);

            // Other Zones
            playerObj.add("graveyard", getZoneJson(p, ZoneType.Graveyard));
            playerObj.add("battlefield", getZoneJson(p, ZoneType.Battlefield));
            playerObj.add("exile", getZoneJson(p, ZoneType.Exile));

            playersArray.add(playerObj);
        }
        state.add("players", playersArray);

        // Combat state (if in combat)
        Combat combat = game.getCombat();
        if (combat != null && !combat.getAttackers().isEmpty()) {
            JsonObject combatState = new JsonObject();
            
            // Attackers and their blockers
            JsonArray attackersJson = new JsonArray();
            for (Card attacker : combat.getAttackers()) {
                JsonObject att = new JsonObject();
                att.addProperty("card_id", attacker.getId());
                att.addProperty("card_name", attacker.getName());
                att.addProperty("power", attacker.getNetPower());
                att.addProperty("toughness", attacker.getNetToughness());
                att.addProperty("controller", attacker.getController().getName());
                
                // What is this creature attacking?
                GameEntity defender = combat.getDefenderByAttacker(attacker);
                if (defender != null) {
                    att.addProperty("attacking_id", defender.getId());
                    att.addProperty("attacking_name", defender.getName());
                    att.addProperty("attacking_type", defender instanceof Player ? "player" : "planeswalker");
                } else {
                    att.addProperty("attacking_id", -1);
                    att.addProperty("attacking_name", "Unknown");
                    att.addProperty("attacking_type", "unknown");
                }
                
                // Blockers assigned to this attacker
                JsonArray blockersJson = new JsonArray();
                CardCollection blockers = combat.getBlockers(attacker);
                if (blockers != null) {
                    for (Card blocker : blockers) {
                        JsonObject blk = new JsonObject();
                        blk.addProperty("card_id", blocker.getId());
                        blk.addProperty("card_name", blocker.getName());
                        blk.addProperty("power", blocker.getNetPower());
                        blk.addProperty("toughness", blocker.getNetToughness());
                        blk.addProperty("controller", blocker.getController().getName());
                        blockersJson.add(blk);
                    }
                }
                att.add("blockers", blockersJson);
                
                attackersJson.add(att);
            }
            combatState.add("attackers", attackersJson);
            combatState.addProperty("attacker_count", combat.getAttackers().size());
            
            state.add("combat", combatState);
        }

        return state;
    }

    private static JsonArray getZoneJson(Player p, ZoneType zone) {
        JsonArray zoneArray = new JsonArray();
        for (Card c : p.getCardsIn(zone)) {
            JsonObject cardObj = new JsonObject();
            cardObj.addProperty("name", c.getName());
            cardObj.addProperty("id", c.getId());
            cardObj.addProperty("zone", zone.toString());
            zoneArray.add(cardObj);
        }
        return zoneArray;
    }

    private static JsonObject getPossibleActions(Player player, Game game) {
        JsonObject actions = new JsonObject();
        JsonArray actionsList = new JsonArray();

        // Get available lands to play
        CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(game, player);
        if (lands != null && !lands.isEmpty()) {
            for (Card land : lands) {
                JsonObject action = new JsonObject();
                action.addProperty("type", "play_land");
                action.addProperty("card_id", land.getId());
                action.addProperty("card_name", land.getName());
                actionsList.add(action);
            }
        }

        // Get available spells and abilities
        CardCollection availableCards = ComputerUtilAbility.getAvailableCards(game, player);
        List<SpellAbility> spellAbilities = ComputerUtilAbility.getSpellAbilities(availableCards, player);

        for (SpellAbility sa : spellAbilities) {
            // Filter to only abilities the player can actually activate
            if (sa.canPlay() && sa.getActivatingPlayer() == player) {
                JsonObject action = new JsonObject();
                Card source = sa.getHostCard();

                if (sa.isSpell()) {
                    action.addProperty("type", "cast_spell");
                } else {
                    action.addProperty("type", "activate_ability");
                }

                action.addProperty("card_id", source != null ? source.getId() : -1);
                action.addProperty("card_name", source != null ? source.getName() : "Unknown");
                action.addProperty("ability_description", sa.getDescription());
                action.addProperty("mana_cost", sa.getPayCosts() != null ? sa.getPayCosts().toSimpleString() : "");

                // Add target information
                if (sa.usesTargeting()) {
                    forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
                    if (tgt != null) {
                        action.addProperty("requires_targets", true);
                        action.addProperty("target_min", tgt.getMinTargets(sa.getHostCard(), sa));
                        action.addProperty("target_max", tgt.getMaxTargets(sa.getHostCard(), sa));
                        action.addProperty("target_zone", tgt.getZone() != null ? tgt.getZone().toString() : "any");
                    }
                } else {
                    action.addProperty("requires_targets", false);
                }

                actionsList.add(action);
            }
        }

        // Always available: pass priority
        JsonObject passAction = new JsonObject();
        passAction.addProperty("type", "pass_priority");
        actionsList.add(passAction);

        actions.add("actions", actionsList);
        actions.addProperty("count", actionsList.size());
        return actions;
    }

    private static class HeadlessLobbyPlayer extends forge.ai.LobbyPlayerAi {
        public HeadlessLobbyPlayer(String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(Game game, final int id) {
            Player ai = new Player(getName(), game, id);
            // Use PlayerControllerRemote with our static aiAgentClient
            if (aiAgentClient != null) {
                ai.setFirstController(new forge.ai.PlayerControllerRemote(game, ai, this, aiAgentClient));
            } else {
                // Fallback to basic AI if no agent configured
                ai.setFirstController(new forge.ai.PlayerControllerAi(game, ai, this));
            }
            return ai;
        }
    }

    // NOTE: HeadlessPlayerController has been removed.
    // All AI agent logic is now in PlayerControllerRemote (forge-ai module).
    // This ensures both start_headless_server.sh and watch_game.sh use the same code.

    private static class HeadlessGameObserver extends forge.game.event.IGameEventVisitor.Base<Void> {
        private PrintStream logStream;

        public HeadlessGameObserver() {
            try {
                logStream = new PrintStream(new FileOutputStream("headless_game.log"), true);
            } catch (IOException e) {
                System.err.println("Error creating log file: " + e.getMessage());
                logStream = System.out;
            }
        }

        private void log(String message) {
            logStream.println(message);
            // System.out.println(message); // Uncomment to also see in console
        }

        @com.google.common.eventbus.Subscribe
        public void receive(forge.game.event.GameEvent ev) {
            ev.visit(this);
        }

        @Override
        public Void visit(GameEventTurnBegan event) {
            log("\n" + ANSI_WHITE + "=== Turn " + event.turnNumber() + " - " + event.turnOwner().getName() + " ==="
                    + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventTurnPhase event) {
            log(ANSI_WHITE + "Phase: " + event.phase() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventGameOutcome event) {
            log("\n*** GAME OVER ***");
            log("Result: " + event.result().getOutcomeStrings());
            return null;
        }

        @Override
        public Void visit(GameEventSpellAbilityCast event) {
            log(ANSI_CYAN + "CAST: " + event.sa().getHostCard().getName() + " by "
                    + event.sa().getActivatingPlayer().getName() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventLandPlayed event) {
            log(ANSI_GREEN + "LAND: " + event.land().getName() + " played by " + event.player().getName() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventPlayerLivesChanged event) {
            log(ANSI_YELLOW + "LIFE: " + event.player().getName() + " is now at " + event.newLives() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventAttackersDeclared event) {
            if (!event.attackersMap().isEmpty()) {
                log(ANSI_RED + "COMBAT: Attackers declared by " + event.player().getName() + ANSI_RESET);
                event.attackersMap().asMap().forEach((target, attackers) -> {
                    log(ANSI_PURPLE + "  Target: " + target + ANSI_RESET);
                    for (Card attacker : attackers) {
                        log(ANSI_RED + "    - " + attacker.getName() + " (" + attacker.getNetPower() + "/"
                                + attacker.getNetToughness() + ")" + ANSI_RESET);
                    }
                });
            }
            return null;
        }

        @Override
        public Void visit(GameEventBlockersDeclared event) {
            if (!event.blockers().isEmpty()) {
                log(ANSI_RED + "COMBAT: Blockers declared by " + event.defendingPlayer().getName() + ANSI_RESET);
                event.blockers().forEach((defender, map) -> {
                    map.forEach((attacker, blockers) -> {
                        for (Card blocker : blockers) {
                            log(ANSI_RED + "    - " + blocker.getName() + " blocks " + attacker.getName() + ANSI_RESET);
                        }
                    });
                });
            }
            return null;
        }

        @Override
        public Void visit(GameEventPlayerDamaged event) {
            log(ANSI_BOLD + ANSI_RED + "DAMAGE: " + event.target().getName() + " took " + event.amount()
                    + " damage from " + event.source() + ANSI_RESET);
            return null;
        }

        @Override
        public Void visit(GameEventCardDamaged event) {
            log(ANSI_BOLD + ANSI_RED + "DAMAGE: " + event.card().getName() + " took " + event.amount() + " damage from "
                    + event.source() + ANSI_RESET);
            return null;
        }
    }

    private static class HeadlessGui implements IGuiBase {
        @Override
        public boolean isRunningOnDesktop() {
            return true;
        }

        @Override
        public boolean isLibgdxPort() {
            return false;
        }

        @Override
        public String getCurrentVersion() {
            return "Headless";
        }

        @Override
        public String getAssetsDir() {
            return "./forge-gui/";
        }

        @Override
        public ImageFetcher getImageFetcher() {
            return null;
        }

        @Override
        public void invokeInEdtNow(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtLater(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void invokeInEdtAndWait(Runnable proc) {
            proc.run();
        }

        @Override
        public boolean isGuiThread() {
            return true;
        }

        @Override
        public ISkinImage getSkinIcon(FSkinProp skinProp) {
            return null;
        }

        @Override
        public ISkinImage getUnskinnedIcon(String path) {
            return null;
        }

        @Override
        public ISkinImage getCardArt(PaperCard card) {
            return null;
        }

        @Override
        public ISkinImage getCardArt(PaperCard card, boolean backFace) {
            return null;
        }

        @Override
        public ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename,
                float opacity) {
            return null;
        }

        @Override
        public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        }

        @Override
        public void showImageDialog(ISkinImage image, String message, String title) {
        }

        @Override
        public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options,
                int defaultOption) {
            return defaultOption;
        }

        @Override
        public String showInputDialog(String message, String title, FSkinProp icon, String initialInput,
                List<String> inputOptions, boolean isNumeric) {
            return initialInput;
        }

        @Override
        public <T> List<T> getChoices(String message, int min, int max, java.util.Collection<T> choices,
                java.util.Collection<T> selected, java.util.function.Function<T, String> display) {
            return new ArrayList<>(selected);
        }

        @Override
        public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax,
                List<T> sourceChoices, List<T> destChoices) {
            return destChoices;
        }

        @Override
        public String showFileDialog(String title, String defaultDir) {
            return null;
        }

        @Override
        public java.io.File getSaveFile(java.io.File defaultFile) {
            return defaultFile;
        }

        @Override
        public void download(GuiDownloadService service, java.util.function.Consumer<Boolean> callback) {
            callback.accept(false);
        }

        @Override
        public void refreshSkin() {
        }

        @Override
        public void showCardList(String title, String message, List<PaperCard> list) {
        }

        @Override
        public boolean showBoxedProduct(String title, String message, List<PaperCard> list) {
            return true;
        }

        @Override
        public PaperCard chooseCard(String title, String message, List<PaperCard> list) {
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        public int getAvatarCount() {
            return 0;
        }

        @Override
        public int getSleevesCount() {
            return 0;
        }

        @Override
        public void copyToClipboard(String text) {
        }

        @Override
        public void browseToUrl(String url) throws java.io.IOException, java.net.URISyntaxException {
        }

        @Override
        public IAudioClip createAudioClip(String filename) {
            return null;
        }

        @Override
        public IAudioMusic createAudioMusic(String filename) {
            return null;
        }

        @Override
        public void startAltSoundSystem(String filename, boolean isSynchronized) {
        }

        @Override
        public void clearImageCache() {
        }

        @Override
        public void showSpellShop() {
        }

        @Override
        public void showBazaar() {
        }

        @Override
        public boolean isSupportedAudioFormat(java.io.File file) {
            return false;
        }

        @Override
        public IGuiGame getNewGuiGame() {
            return null;
        }

        @Override
        public HostedMatch hostMatch() {
            return null;
        }

        @Override
        public void runBackgroundTask(String message, Runnable task) {
            task.run();
        }

        @Override
        public String encodeSymbols(String str, boolean formatReminderText) {
            return str;
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
        }

        @Override
        public float getScreenScale() {
            return 1.0f;
        }

        @Override
        public UpnpServiceConfiguration getUpnpPlatformService() {
            return null;
        }
    }
}
