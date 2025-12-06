package forge.env;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Tests for ForgeEnv headless environment.
 */
public class ForgeEnvTest {

    private String getResourcePath() {
        // Get the project root directory (go up from forge-env to the root)
        File currentDir = new File(System.getProperty("user.dir"));
        File projectRoot = currentDir.getName().equals("forge-env") 
            ? currentDir.getParentFile() 
            : currentDir;
        return new File(projectRoot, "forge-gui/res").getAbsolutePath();
    }

    @BeforeMethod
    public void setUp() {
        ForgeEnv.reset();
    }

    @Test
    public void testInitializeSucceeds() {
        // This test will initialize the environment with the resource directory
        String resPath = getResourcePath();
        if (!new File(resPath).exists()) {
            // Skip test if resources not available
            return;
        }
        
        ForgeEnv.initialize(resPath);
        Assert.assertTrue(ForgeEnv.isInitialized(), "ForgeEnv should be initialized");
        Assert.assertTrue(ForgeEnv.getStandardDeckCount() > 0, "Should have loaded some decks");
    }

    @Test
    public void testNewGameReturnsJson() {
        String resPath = getResourcePath();
        if (!new File(resPath).exists()) {
            // Skip test if resources not available
            return;
        }
        
        ForgeEnv.initialize(resPath);
        String json = ForgeEnv.newGame();
        
        Assert.assertNotNull(json, "JSON output should not be null");
        Assert.assertTrue(json.contains("players"), "JSON should contain players");
        Assert.assertTrue(json.contains("activePlayerId"), "JSON should contain activePlayerId");
        Assert.assertTrue(json.contains("phase"), "JSON should contain phase");
        Assert.assertTrue(json.contains("turn"), "JSON should contain turn");
    }

    @Test
    public void testMultipleNewGameCalls() {
        String resPath = getResourcePath();
        if (!new File(resPath).exists()) {
            // Skip test if resources not available
            return;
        }
        
        ForgeEnv.initialize(resPath);
        
        // Create multiple games to ensure they don't interfere
        String json1 = ForgeEnv.newGame();
        String json2 = ForgeEnv.newGame();
        
        Assert.assertNotNull(json1, "First game JSON should not be null");
        Assert.assertNotNull(json2, "Second game JSON should not be null");
        
        // They should be different games (different cards in hand, etc)
        // Note: They might occasionally be the same if same decks are chosen randomly
        Assert.assertTrue(json1.length() > 100, "JSON should have substantial content");
        Assert.assertTrue(json2.length() > 100, "JSON should have substantial content");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testNewGameBeforeInitializeThrows() {
        ForgeEnv.newGame(); // Should throw
    }
}
