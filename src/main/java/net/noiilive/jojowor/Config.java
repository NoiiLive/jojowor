package net.noiilive.jojowor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Whether to print extra debug information to the log")
            .define("debugLogging", false);

    public static final ModConfigSpec.BooleanValue GLOBAL_TIMESTOP = BUILDER
            .comment("Whether Time Stop affects the entire world instead of a radius around the user")
            .define("globalTimestop", false);

    public static final ModConfigSpec.IntValue TIMESTOP_RADIUS = BUILDER
            .comment("Radius in blocks that Time Stop affects when globalTimestop is disabled")
            .defineInRange("timestopRadius", 50, 1, 512);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
