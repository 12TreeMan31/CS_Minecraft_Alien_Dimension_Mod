package com.cs_club.minecraft_alien_mod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

class CopperSwordItem extends SwordItem {

    public CopperSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // Only run on the server side to spawn the projectile
        if (!player.level().isClientSide()) {
            shootFireball(player);
        }
        // Return false to allow the normal attack damage to still apply
        return false;
    }

    public void shootFireball(Player player) {
        Level level = player.level();
        if (level.isClientSide()) return;

        Vec3 look = player.getLookAngle();
        Vec3 spawn = player.getEyePosition().add(look.scale(0.5)); // nudge out of the head
        Vec3 velocity = look.scale(1.5);                            // actual speed

        SmallFireball fireball = new SmallFireball(
                level,
                spawn.x, spawn.y, spawn.z,
                velocity
        );
        level.addFreshEntity(fireball);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
@Mod(minecraftalienmod.MODID)
public class minecraftalienmod {

    public static final String MODID = "minecraftalienmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred registers
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Example block
    public static final DeferredBlock<Block> EXAMPLE_BLOCK =
            BLOCKS.registerSimpleBlock(
                    "example_block",
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
            );

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Example food item
    public static final DeferredItem<Item> EXAMPLE_ITEM =
            ITEMS.registerSimpleItem(
                    "example_item",
                    new Item.Properties().food(
                            new FoodProperties.Builder()
                                    .alwaysEdible()
                                    .nutrition(1)
                                    .saturationModifier(2f)
                                    .build()
                    )
            );

    // Copper harvesting tags
    public static final TagKey<Block> NEEDS_COPPER_TOOL =
            TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            MODID,
                            "needs_copper_tool"
                    )
            );

    public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL =
            TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            MODID,
                            "incorrect_for_copper_tool"
                    )
            );

    // Copper tier
    public static final Tier COPPER_TIER = new SimpleTier(
            INCORRECT_FOR_COPPER_TOOL,
            200,    // Durability
            5.0F,   // Mining speed
            1.5F,   // Attack damage bonus
            20,     // Enchantability
            () -> Ingredient.of(Tags.Items.INGOTS_COPPER)
    );

    // Copper sword
    public static final DeferredItem<CopperSwordItem> COPPER_SWORD =
            ITEMS.register("copper_sword", () -> new CopperSwordItem(
                    COPPER_TIER,
                    new Item.Properties().attributes(
                            SwordItem.createAttributes(COPPER_TIER, 3, -2.4F)
                    )
            ));

    // Copper axe
    public static final Supplier<AxeItem> COPPER_AXE =
            ITEMS.register(
                    "copper_axe",
                    () -> new AxeItem(
                            COPPER_TIER,
                            new Item.Properties().attributes(
                                    AxeItem.createAttributes(
                                            COPPER_TIER,
                                            5.0F,
                                            -3.0F
                                    )
                            )
                    )
            );

    // Copper pickaxe
    public static final Supplier<PickaxeItem> COPPER_PICKAXE =
            ITEMS.register(
                    "copper_pickaxe",
                    () -> new PickaxeItem(
                            COPPER_TIER,
                            new Item.Properties().attributes(
                                    PickaxeItem.createAttributes(
                                            COPPER_TIER,
                                            1.0F,
                                            -2.8F
                                    )
                            )
                    )
            );

    // Copper shovel
    public static final Supplier<ShovelItem> COPPER_SHOVEL =
            ITEMS.register(
                    "copper_shovel",
                    () -> new ShovelItem(
                            COPPER_TIER,
                            new Item.Properties().attributes(
                                    ShovelItem.createAttributes(
                                            COPPER_TIER,
                                            1.5F,
                                            -3.0F
                                    )
                            )
                    )
            );

    // Copper hoe
    public static final Supplier<HoeItem> COPPER_HOE =
            ITEMS.register(
                    "copper_hoe",
                    () -> new HoeItem(
                            COPPER_TIER,
                            new Item.Properties().attributes(
                                    HoeItem.createAttributes(
                                            COPPER_TIER,
                                            -2.0F,
                                            -1.0F
                                    )
                            )
                    )
            );

    // Example creative tab  <-- MOVED BELOW so every referenced field is already defined
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register(
                    "example_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.minecraftalienmod"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(EXAMPLE_ITEM.get());
                                output.accept(EXAMPLE_BLOCK_ITEM.get());
                                output.accept(COPPER_SWORD.get());
                                output.accept(COPPER_AXE.get());
                                output.accept(COPPER_PICKAXE.get());
                                output.accept(COPPER_SHOVEL.get());
                                output.accept(COPPER_HOE.get());
                            })
                            .build()
            );

    public record ShootFireballPacket() implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "shoot_fireball");
        public static final CustomPacketPayload.Type<ShootFireballPacket> TYPE =
                new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ShootFireballPacket> CODEC =
                StreamCodec.unit(new ShootFireballPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        // Handler that runs on the server
        public void handle(IPayloadContext context) {
            context.enqueueWork(() -> {
                Player player = context.player();
                if (player.getMainHandItem().getItem() instanceof CopperSwordItem sword) {
                    sword.shootFireball(player);
                }
            });
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);
        registrar.playToServer(
                ShootFireballPacket.TYPE,
                ShootFireballPacket.CODEC,
                ShootFireballPacket::handle
        );
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public class EventHandler {
        @SubscribeEvent
        public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
            Entity entity = event.getEntity();

            if (!entity.level().isClientSide()) {
                Vec3 x = entity.getEyePosition().add(new Vec3(0, 10, 0));
                entity.setPos(x);
                // DamageSources source = entity.level().damageSources();
                // entity.hurt(source.anvil(entity), 1);
            }
        }
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public class ClientEvents {
        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getEntity().getMainHandItem().getItem() instanceof CopperSwordItem) {
                // Send the packet to the server
                PacketDistributor.sendToServer(new ShootFireballPacket());
            }
        }
    }

    // Mod constructor
    public minecraftalienmod(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
        modEventBus.addListener(this::registerPayloads);
    }

    // Common setup
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info(
                "DIRT BLOCK >> {}",
                BuiltInRegistries.BLOCK.getKey(Blocks.DIRT)
        );

        LOGGER.info(
                "{}{}",
                Config.MAGIC_NUMBER_INTRODUCTION.get(),
                Config.MAGIC_NUMBER.getAsInt()
        );

        Config.ITEM_STRINGS.get().forEach(
                item -> LOGGER.info("ITEM >> {}", item)
        );
    }

    // Add items to vanilla creative tabs
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(COPPER_AXE.get());
            event.accept(COPPER_PICKAXE.get());
            event.accept(COPPER_SHOVEL.get());
            event.accept(COPPER_HOE.get());
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(COPPER_SWORD.get());
        }
    }

    // Server starting event
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}