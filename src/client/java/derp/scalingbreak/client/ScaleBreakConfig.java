package derp.scalingbreak.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ScaleBreakConfig {
    public static final boolean DEFAULT_ENABLED = true;
    public static final float DEFAULT_SHRINK_AMOUNT = 0.72F;
    public static final float DEFAULT_SHRINK_SPEED = 14.0F;
    public static final float DEFAULT_RECOVERY_SPEED = 6.0F;
    public static final boolean DEFAULT_MULTIBLOCKS = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("scalebreak.json");

    private static ScaleBreakConfig INSTANCE = defaults();

    public boolean enabled = DEFAULT_ENABLED;
    public float shrinkAmount = DEFAULT_SHRINK_AMOUNT;
    public float shrinkSpeed = DEFAULT_SHRINK_SPEED;
    public float recoverySpeed = DEFAULT_RECOVERY_SPEED;
    public boolean multiblocks = DEFAULT_MULTIBLOCKS;

    private ScaleBreakConfig() {
    }

    public static ScaleBreakConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            INSTANCE = defaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ScaleBreakConfig loaded = GSON.fromJson(reader, ScaleBreakConfig.class);
            INSTANCE = loaded != null ? loaded : defaults();
            INSTANCE.sanitize();
        } catch (Exception exception) {
            System.err.println("[ScaleBreak] Failed to load config: " + exception.getMessage());
            INSTANCE = defaults();
        }
    }

    public static void save() {
        INSTANCE.sanitize();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException exception) {
            System.err.println("[ScaleBreak] Failed to save config: " + exception.getMessage());
        }
    }

    public static void reset() {
        INSTANCE = defaults();
        save();
    }

    private static ScaleBreakConfig defaults() {
        return new ScaleBreakConfig();
    }

    private void sanitize() {
        shrinkAmount = clamp(shrinkAmount, 0.0F, 1F);
        shrinkSpeed = clamp(shrinkSpeed, 0.1F, 50.0F);
        recoverySpeed = clamp(recoverySpeed, 0.1F, 50.0F);
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
