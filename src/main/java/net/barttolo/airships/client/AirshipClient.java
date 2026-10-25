package net.barttolo.airships.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.barttolo.airships.AirshipEntity;
import net.barttolo.airships.BarttoloAirships;
import net.barttolo.airships.Net;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Physical client only. C is the safe default descend key: vanilla Shift ejects riders. */
public final class AirshipClient {
    private static final String CATEGORY="key.categories.barttoloairships";
    public static final KeyMapping DESCEND = new KeyMapping("key.barttoloairships.descend",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_C,CATEGORY);
    public static final KeyMapping LEAVE = new KeyMapping("key.barttoloairships.leave",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,CATEGORY);
    private static int ticker;

    @Mod.EventBusSubscriber(modid=BarttoloAirships.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(BarttoloAirships.AIRSHIP.get(),AirshipRenderer::new);
        }
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {event.register(DESCEND);event.register(LEAVE);}
    }

    @Mod.EventBusSubscriber(modid=BarttoloAirships.MOD_ID, value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if(event.phase!=TickEvent.Phase.END)return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null || !(mc.player.getVehicle() instanceof AirshipEntity))return;
            if(mc.screen!=null)return;
            if(LEAVE.consumeClick()) {
                Net.CHANNEL.sendToServer(new Net.Input((byte)0,(byte)0,(byte)0,true));return;
            }
            if(++ticker%2!=0)return;
            byte forward=(byte)((mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0));
            byte turn=(byte)((mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0));
            byte vertical=(byte)((mc.options.keyJump.isDown()?1:0)-(DESCEND.isDown()?1:0));
            Net.CHANNEL.sendToServer(new Net.Input(forward,turn,vertical,false));
        }
        @SubscribeEvent public static void renderStanding(RenderPlayerEvent.Pre event) {
            // An experimental visual hint, does not alter server passenger positions.
            if(event.getEntity().getVehicle() instanceof AirshipEntity)
                event.getRenderer().getModel().riding=false;
        }
    }
}
