package net.barttolo.airships;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Deny accidental Shift dismount in the air. V still performs validated exit. */
public final class AirshipEvents {
    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof AirshipEntity ship)) return;
        if (!(event.getEntityMounting() instanceof ServerPlayer passenger)) return;
        if (passenger.isAlive() && !passenger.isRemoved()
                && !ship.isAuthorizedDismount(passenger) && !ship.hasSafeLanding()) {
            event.setCanceled(true);
        }
    }
}
