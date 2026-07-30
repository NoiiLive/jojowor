package net.noiilive.jojowor.client.model;

import net.noiilive.jojowor.stand.StandPose;

public record StandAnimation(
        double ageTicks,
        float headYaw,
        float headPitch,
        float leanForward,
        float leanRight,
        float defensive,
        float combat,
        float leftPunch,
        float rightPunch,
        float throwTime,
        float barrage,
        float barrageTime,
        float uppercutTime,
        float slamTime,
        StandPose pose) {
}
