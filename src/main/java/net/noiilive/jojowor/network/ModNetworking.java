package net.noiilive.jojowor.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.noiilive.jojowor.JoJoWoR;

@EventBusSubscriber(modid = JoJoWoR.MODID)
public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(
                SummonStandPayload.TYPE,
                SummonStandPayload.STREAM_CODEC,
                ServerPayloadHandler::handleSummonStand);

        registrar.playToServer(
                StandGuardPayload.TYPE,
                StandGuardPayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandGuard);

        registrar.playToServer(
                StandAttackPayload.TYPE,
                StandAttackPayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandAttack);

        registrar.playToServer(
                SetStandPosePayload.TYPE,
                SetStandPosePayload.STREAM_CODEC,
                ServerPayloadHandler::handleSetStandPose);

        registrar.playToServer(
                SetStandSkinPayload.TYPE,
                SetStandSkinPayload.STREAM_CODEC,
                ServerPayloadHandler::handleSetStandSkin);

        registrar.playToServer(
                StandThrowPayload.TYPE,
                StandThrowPayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandThrow);

        registrar.playToServer(
                StandBarragePayload.TYPE,
                StandBarragePayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandBarrage);

        registrar.playToServer(
                StandUppercutPayload.TYPE,
                StandUppercutPayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandUppercut);

        registrar.playToClient(
                StandUppercutEffectPayload.TYPE,
                StandUppercutEffectPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStandUppercutEffect);

        registrar.playToServer(
                StandSlamPayload.TYPE,
                StandSlamPayload.STREAM_CODEC,
                ServerPayloadHandler::handleStandSlam);

        registrar.playToClient(
                StandSlamEffectPayload.TYPE,
                StandSlamEffectPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStandSlamEffect);

        registrar.playToClient(
                StandBarrageEffectPayload.TYPE,
                StandBarrageEffectPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStandBarrageEffect);

        registrar.playToClient(
                StandAttackEffectPayload.TYPE,
                StandAttackEffectPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStandAttackEffect);

        registrar.playToClient(
                StandThrowEffectPayload.TYPE,
                StandThrowEffectPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStandThrowEffect);
    }
}
