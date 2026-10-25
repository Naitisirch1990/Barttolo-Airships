package net.barttolo.airships;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(BarttoloAirships.MOD_ID)
public final class BarttoloAirships {
    public static final String MOD_ID = "barttoloairships";
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<AirshipEntity>> AIRSHIP = ENTITY_TYPES.register("airship", () ->
        EntityType.Builder.<AirshipEntity>of(AirshipEntity::new, MobCategory.MISC)
            .sized(4.0F, 3.0F).clientTrackingRange(16).updateInterval(1)
            .build(new ResourceLocation(MOD_ID, "airship").toString()));

    public BarttoloAirships() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
        Net.register();
        MinecraftForge.EVENT_BUS.addListener(AirshipCommands::register);
        MinecraftForge.EVENT_BUS.register(new AirshipEvents());
    }
}
