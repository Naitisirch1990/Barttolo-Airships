package net.barttolo.airships;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class AirshipCommands {
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d=event.getDispatcher();
        d.register(Commands.literal("airship")
            .then(Commands.literal("crear").requires(src->src.hasPermission(4)).executes(ctx->create(ctx.getSource())))
            .then(Commands.literal("diagnostico").requires(src->src.hasPermission(2)).executes(ctx->diagnostics(ctx.getSource())))
            .then(Commands.literal("llave").requires(src->src.hasPermission(4)).executes(ctx->key(ctx.getSource())))
            .then(Commands.literal("eliminar").requires(src->src.hasPermission(4)).executes(ctx->remove(ctx.getSource())))
            .then(Commands.literal("abordar").executes(ctx->board(ctx.getSource())))
            .then(Commands.literal("tomarcontrol").executes(ctx->helm(ctx.getSource())))
            .then(Commands.literal("salir").executes(ctx->exit(ctx.getSource())))
            .then(Commands.literal("combustible").executes(ctx->fuel(ctx.getSource())))
            .then(Commands.literal("instalarcofre").then(Commands.argument("numero",IntegerArgumentType.integer(1,4)).executes(ctx->install(ctx.getSource(),IntegerArgumentType.getInteger(ctx,"numero")-1))))
            .then(Commands.literal("quitarcofre").then(Commands.argument("numero",IntegerArgumentType.integer(1,4)).executes(ctx->uninstall(ctx.getSource(),IntegerArgumentType.getInteger(ctx,"numero")-1))))
            .then(Commands.literal("cofre").then(Commands.argument("numero",IntegerArgumentType.integer(1,4)).executes(ctx->chest(ctx.getSource(),IntegerArgumentType.getInteger(ctx,"numero")-1))))
            .then(Commands.literal("estado").executes(ctx->status(ctx.getSource()))));
    }
    private static ServerPlayer player(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {return src.getPlayerOrException();}
    private static AirshipEntity near(ServerPlayer player) {
        if (player.getVehicle() instanceof AirshipEntity aboard) return aboard;
        return player.serverLevel().getEntitiesOfClass(AirshipEntity.class,
            AABB.ofSize(player.position(),180,150,180)).stream()
            .min((a,b)->Double.compare(a.distanceToSqr(player),b.distanceToSqr(player))).orElse(null);
    }
    private static AirshipEntity requireShip(ServerPlayer player) {
        AirshipEntity ship=near(player);
        if(ship!=null && player.getVehicle()!=ship &&
           (ship.distanceToSqr(player)>45*45 || Math.abs(ship.getY()-player.getY())>28)) ship=null;
        if(ship==null) AirshipEntity.message(player,"No se encontró ninguna nave al alcance (45 bloques).");
        return ship;
    }
    private static boolean owner(ServerPlayer user) {
        if (!user.createCommandSourceStack().hasPermission(4)) {AirshipEntity.message(user,"Solo el administrador propietario puede usar ese comando.");return false;}
        AirshipRegistry reg=AirshipRegistry.get(user.server);
        if(reg.owner != null && !reg.isOwner(user.getUUID())) {AirshipEntity.message(user,"Solo el propietario original puede gestionar la nave.");return false;}
        return true;
    }
    private static int create(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);
        if(!owner(user)) return 0;
        AirshipRegistry reg=AirshipRegistry.get(user.server);
        if(reg.ship != null) {AirshipEntity.message(user,"Ya existe una nave registrada. No se creó otra.");return 0;}
        ServerLevel level=user.serverLevel();
        AirshipEntity ship=BarttoloAirships.AIRSHIP.get().create(level);
        if(ship==null) return 0;
        // The model extends roughly 9 blocks below and 23 above its origin.
        // Create only where the complete rotated ship is over loaded, unobstructed terrain.
        double spawnY = AirshipSafety.findSpawnHeight(level,user.getX(),user.getY(),user.getZ(),user.getYRot());
        if (Double.isNaN(spawnY)) {
            int missing=AirshipSafety.missingFootprintChunks(level,user.getX(),user.getZ(),user.getYRot());
            if (missing>0) AirshipEntity.message(user,"No se creó: faltan "+missing+" chunks cargados alrededor del casco (54x34). Cambia a una zona abierta y espera a que cargue.");
            else AirshipEntity.message(user,"No se creó: no hay altura libre segura hasta 80 bloques sobre tu posición. Usa /airship diagnostico y ve a una explanada.");
            return 0;
        }
        ship.moveTo(user.getX(),spawnY,user.getZ(),user.getYRot(),0);
        if(!level.addFreshEntity(ship)) {AirshipEntity.message(user,"No se pudo registrar la entidad. Revisa latest.log.");return 0;}
        if(reg.owner==null) reg.owner=user.getUUID();
        reg.ship=ship.getUUID();reg.setDirty();
        ItemStack key=ship.makeKey();
        if(!user.getInventory().add(key)) user.drop(key,false);
        ship.fillStarterFuel(); // A mounted creator can always maneuver downward safely.
        boolean boarded=ship.board(user);
        AirshipEntity.message(user,"Barco creado en X="+ship.blockPosition().getX()+" Y="+ship.blockPosition().getY()+" Z="+ship.blockPosition().getZ()+". Combustible inicial: 90 s."+(boarded ? " Estás a bordo, usa WASD/Espacio/C." : " Usa /airship abordar para subir."));
        return 1;
    }
    private static int diagnostics(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);
        MinecraftServer server=user.server;
        AirshipRegistry reg=AirshipRegistry.get(server);
        AirshipEntity.message(user,"Diagnóstico Alfa 0.1.4: OP4="+src.hasPermission(4)+", propietario="+(reg.owner==null?"sin asignar":reg.isOwner(user.getUUID()))+", nave registrada="+(reg.ship!=null));
        if(reg.ship!=null) {
            boolean loaded=false;
            for(ServerLevel level:server.getAllLevels()) {
                if(level.getEntity(reg.ship) instanceof AirshipEntity ship) {
                    loaded=true;
                    AirshipEntity.message(user,"Barco existente en dimensión "+level.dimension().location()+", X="+ship.blockPosition().getX()+" Y="+ship.blockPosition().getY()+" Z="+ship.blockPosition().getZ());
                }
            }
            if(!loaded)AirshipEntity.message(user,"Hay una nave registrada, pero está en un chunk no cargado (o falta la entidad). No crees duplicados; revisa el mundo antes de recuperar.");
        }
        int missing=AirshipSafety.missingFootprintChunks(user.serverLevel(),user.getX(),user.getZ(),user.getYRot());
        double altitude=AirshipSafety.findSpawnHeight(user.serverLevel(),user.getX(),user.getY(),user.getZ(),user.getYRot());
        AirshipEntity.message(user,"Zona actual: chunks faltantes="+missing+", altura de creación="+(Double.isNaN(altitude)?"ninguna segura":String.valueOf((int)altitude))+". La nave necesita cielo despejado y chunks cargados.");
        return 1;
    }
    private static int key(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);
        if(!owner(user))return 0;
        AirshipEntity ship=requireShip(user);if(ship==null)return 0;
        ItemStack key=ship.makeKey();if(!user.getInventory().add(key))user.drop(key,false);
        AirshipEntity.message(user,"Llave codificada creada para esta nave.");return 1;
    }
    private static int remove(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);if(!owner(user))return 0;
        AirshipEntity ship=requireShip(user);if(ship==null)return 0;
        if(!ship.items().stream().allMatch(ItemStack::isEmpty)) {
            AirshipEntity.message(user,"Vacía los cofres antes de eliminar. No destruimos cargamento.");return 0;
        }
        if(!ship.getPassengers().isEmpty()) {
            AirshipEntity.message(user,"Primero desembarca a todos los pasajeros.");return 0;
        }
        ship.discard();
        AirshipRegistry reg=AirshipRegistry.get(user.server);reg.ship=null;reg.setDirty();
        AirshipEntity.message(user,"Nave eliminada; permisos del propietario conservados.");return 1;
    }
    private static int board(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        return ship!=null && ship.board(user) ? 1:0;
    }
    private static int exit(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);
        if(user.getVehicle() instanceof AirshipEntity ship) {ship.requestExit(user);return 1;}
        return 0;
    }
    private static int fuel(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        if(ship==null)return 0;
        ship.addFuel(user,user.getMainHandItem());return 1;
    }
    private static int install(CommandSourceStack src,int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        if(ship==null)return 0;
        ship.installChest(user,number);return 1;
    }
    private static int chest(CommandSourceStack src,int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        if(ship==null)return 0;
        ship.openCargo(user,number);return 1;
    }
    private static int helm(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);
        return user.getVehicle() instanceof AirshipEntity ship && ship.takeHelm(user) ? 1:0;
    }
    private static int uninstall(CommandSourceStack src,int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        if(ship==null)return 0;
        ship.removeChest(user,number);return 1;
    }
    private static int status(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user=player(src);AirshipEntity ship=requireShip(user);
        if(ship==null)return 0;
        AirshipEntity.message(user,"Combustible: "+ship.getFuel()/20+"s; pasajeros: "+ship.getPassengers().size()+"/6; una nave por servidor.");return 1;
    }
}
