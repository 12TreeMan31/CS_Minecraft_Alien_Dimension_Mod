package com.cs_club.minecraft_alien_mod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {
    public static final FoodProperties STRAWBERRY = new FoodProperties.Builder().nutrition(3).saturationModifier(0.25f)
            .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 400), 0.35f).build();
    public static final FoodProperties OLDFASHIONED = new FoodProperties.Builder().nutrition(-10).saturationModifier(0.25f).effect(()
            -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 400), 0.35f).build();
}