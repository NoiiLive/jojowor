package net.noiilive.jojowor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Whether to print extra debug information to the log")
            .define("debugLogging", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
