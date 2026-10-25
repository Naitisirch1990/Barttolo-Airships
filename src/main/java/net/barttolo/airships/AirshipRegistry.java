package net.barttolo.airships;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** Persistently locks creation to the first level-4 admin and one ship UUID. */
public final class AirshipRegistry extends SavedData {
    public UUID owner;
    public UUID ship;

    public static AirshipRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AirshipRegistry::load, AirshipRegistry::new, "barttolo_airships_registry");
    }
    public static AirshipRegistry load(CompoundTag tag) {
        AirshipRegistry result = new AirshipRegistry();
        if (tag.hasUUID("Owner")) result.owner = tag.getUUID("Owner");
        if (tag.hasUUID("Ship")) result.ship = tag.getUUID("Ship");
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        if (ship != null) tag.putUUID("Ship", ship);
        return tag;
    }
    public boolean isOwner(UUID uuid) {return owner != null && owner.equals(uuid);}
}
