package nl.townsandkingdoms.streamcontrol;
import net.minecraftforge.common.ForgeConfigSpec;
public final class ControlConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, WAND_ENABLED, OP_ONLY, PARTICLES;
    public static final ForgeConfigSpec.IntValue MAX_RADIUS, DEFAULT_RADIUS, VERTICAL, MAX_REGIONS, MAX_PLAYER_REGIONS;
    public static final ForgeConfigSpec.DoubleValue SPEED, MAX_SPEED;
    static {
        var b = new ForgeConfigSpec.Builder();
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
    public static int radius(int value) { return Math.max(1, Math.min(MAX_RADIUS.get(), value)); }
    public static double strength(double value) { return Double.isFinite(value) ? Math.max(0.05, Math.min(MAX_SPEED.get(), value)) : Math.min(1, MAX_SPEED.get()); }
}
