package net.noiilive.jojowor.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.noiilive.jojowor.JoJoWoR;

public class StunEffect extends MobEffect {
    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0xF5D442);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stun_slowdown"),
                -0.6D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stun_attack_slowdown"),
                -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
