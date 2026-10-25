package net.barttolo.airships;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

/** Server-side authoritative: client sends only bounded integer direction inputs. */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BarttoloAirships.MOD_ID, "controls"), () -> VERSION, VERSION::equals, VERSION::equals);
    public static void register() {
        CHANNEL.registerMessage(0, Input.class, Input::write, Input::read, Input::handle);
    }
    public record Input(byte forward, byte turn, byte vertical, boolean dismount) {
        public void write(FriendlyByteBuf buf) {buf.writeByte(forward);buf.writeByte(turn);buf.writeByte(vertical);buf.writeBoolean(dismount);}
        public static Input read(FriendlyByteBuf buf) {return new Input(buf.readByte(),buf.readByte(),buf.readByte(),buf.readBoolean());}
        public static void handle(Input msg, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && player.getVehicle() instanceof AirshipEntity ship) {
                    if (msg.dismount()) ship.requestExit(player);
                    else ship.acceptInput(player, msg.forward(), msg.turn(), msg.vertical());
                }
            });
            context.setPacketHandled(true);
        }
    }
}
