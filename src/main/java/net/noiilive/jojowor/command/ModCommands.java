package net.noiilive.jojowor.command;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.noiilive.jojowor.JoJoWoR;

@EventBusSubscriber(modid = JoJoWoR.MODID)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(JoJoWoR.MODID)
                        .then(StandCommand.build()));
    }
}
