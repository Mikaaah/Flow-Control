package nl.townsandkingdoms.streamcontrol;
import net.minecraftforge.common.ForgeConfigSpec;
public final class ControlConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, WAND_ENABLED, OP_ONLY, PARTICLES;
    public static final ForgeConfigSpec.IntValue MAX_RADIUS, DEFAULT_RADIUS, VERTICAL, MAX_REGIONS, MAX_PLAYER_REGIONS, MAX_VERTICAL, LINK_RANGE, SENSOR_MAX_RADIUS, SENSOR_MAX_VERTICAL, SENSOR_INTERVAL, MOORING_MAX_RADIUS, MOORING_MAX_VERTICAL;
    public static final ForgeConfigSpec.DoubleValue SPEED, MAX_SPEED, RPM_FULL_STRENGTH, KINETIC_STRESS;
    static {
        var b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Master switch. Overrides remain saved when disabled.").define("enabled", true);
        WAND_ENABLED = b.comment("Enable the Flow Calibrator; legacy config key retained.").define("wandEnabled", true);
        OP_ONLY = b.define("wandRequiresOperator", false);
        PARTICLES = b.comment("Show Calibrator range outlines; legacy config key retained.").define("showWandFeedbackParticles", true);
        MAX_RADIUS = b.defineInRange("maxRadius", 16, 1, 32);
        DEFAULT_RADIUS = b.defineInRange("defaultRadius", 4, 1, 32);
        VERTICAL = b.comment("Blocks above and below the selected water or controller that can be affected.").defineInRange("verticalReach", 4, 0, 16);
        SPEED = b.comment("Current strength relative to a normal Streams current.").defineInRange("defaultStrength", 1.0, 0.05, 2.0);
        MAX_SPEED = b.defineInRange("maxStrength", 2.0, 0.05, 2.0);
        MAX_REGIONS = b.defineInRange("maxRegionsPerDimension", 2048, 1, 8192);
        MAX_PLAYER_REGIONS = b.defineInRange("maxWandRegionsPerPlayer", 256, 1, 2048);
        MAX_VERTICAL = b.defineInRange("maxControllerVerticalReach",16,0,16);
        LINK_RANGE = b.defineInRange("maxControllerLinkDistance",64,1,256);
        SENSOR_MAX_RADIUS = b.defineInRange("maxSensorRadius",16,1,16);
        SENSOR_MAX_VERTICAL = b.defineInRange("maxSensorVerticalReach",16,0,16);
        SENSOR_INTERVAL = b.defineInRange("sensorSampleIntervalTicks",20,5,200);
        MOORING_MAX_RADIUS = b.defineInRange("maxMooringRadius",8,1,16);
        MOORING_MAX_VERTICAL = b.defineInRange("maxMooringVerticalReach",8,0,16);
        RPM_FULL_STRENGTH = b.defineInRange("kineticRpmForFullStrength",128.0,1.0,1024.0);
        KINETIC_STRESS = b.defineInRange("kineticStressImpact",4.0,0.0,256.0);
        SPEC = b.build();
    }
    public static int radius(int value) { return nl.townsandkingdoms.streamcontrol.Numbers.clamp(value, 1, MAX_RADIUS.get()); }
    public static double strength(double value) { return Double.isFinite(value) ? nl.townsandkingdoms.streamcontrol.Numbers.clamp(value, 0.05, MAX_SPEED.get()) : Math.min(1, MAX_SPEED.get()); }
}
