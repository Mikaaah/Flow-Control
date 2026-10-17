package nl.townsandkingdoms.streamcontrol;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ControlConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, WAND_ENABLED, OP_ONLY, PARTICLES;
    public static final ModConfigSpec.IntValue MAX_RADIUS, DEFAULT_RADIUS, VERTICAL, MAX_REGIONS, MAX_PLAYER_REGIONS;
    public static final ModConfigSpec.DoubleValue SPEED, MAX_SPEED;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Master switch. Overrides remain saved when disabled.").define("enabled", true);
        WAND_ENABLED = b.define("wandEnabled", true);
        OP_ONLY = b.define("wandRequiresOperator", false);
        PARTICLES = b.define("showWandFeedbackParticles", true);
        MAX_RADIUS = b.defineInRange("maxRadius", 16, 1, 32);
        DEFAULT_RADIUS = b.defineInRange("defaultRadius", 4, 1, 32);
        VERTICAL = b.comment("Blocks above and below the selected water or controller that can be affected.").defineInRange("verticalReach", 4, 0, 16);
        SPEED = b.comment("Current strength relative to a normal Streams current.").defineInRange("defaultStrength", 1.0, 0.05, 2.0);
        MAX_SPEED = b.defineInRange("maxStrength", 2.0, 0.05, 2.0);
        MAX_REGIONS = b.defineInRange("maxRegionsPerDimension", 2048, 1, 8192);
        MAX_PLAYER_REGIONS = b.defineInRange("maxWandRegionsPerPlayer", 256, 1, 2048);
        SPEC = b.build();
    }
    public static int radius(int value) { return Math.clamp(value, 1, MAX_RADIUS.get()); }
    public static double strength(double value) { return Double.isFinite(value) ? Math.clamp(value, 0.05, MAX_SPEED.get()) : Math.min(1, MAX_SPEED.get()); }
}
