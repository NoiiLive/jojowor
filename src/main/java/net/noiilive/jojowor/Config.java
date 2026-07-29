package net.noiilive.jojowor;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for JoJo: Winds of Requiem.
 *
 * <p>Remember to add a translation for each option to {@code en_us.json}
 * using the {@code jojowor.configuration.<key>} format so it reads nicely on
 * the in-game config screen.</p>
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Whether to print extra debug information to the log")
            .define("debugLogging", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
