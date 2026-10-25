package net.barttolo.airships;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** One immutable visual structure. No actual block placement; only the entity is saved. */
public final class AirshipEntity extends Entity {
    public static final int FRIEND_CAPACITY = 5;
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(AirshipEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHESTS = SynchedEntityData.defineId(AirshipEntity.class,EntityDataSerializers.INT);
    private static final double MAX_SPEED = 0.29; // approx 5.8 blocks/second, server limit
    private static final int MAX_FUEL = 20*60*60; // hard limit one hour
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(4*27,ItemStack.EMPTY);
    private UUID pilotUUID;
    private byte forward, turn, vertical;
    private int lastInputTick;
    private int tickCounter;
    private int lastPacketTick=-1;
    private UUID permittedDismountUUID;


    public AirshipEntity(EntityType<? extends AirshipEntity> type, Level level) {
        super(type,level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override protected void defineSynchedData() {this.entityData.define(FUEL,0); this.entityData.define(CHESTS,0);}
    public int getFuel() {return this.entityData.get(FUEL);}
    public boolean hasChest(int index) {return index >= 0 && index < 4 && (entityData.get(CHESTS) & (1<<index)) != 0;}
    public NonNullList<ItemStack> items() {return inventory;}
    /** Ask the world to persist cargo/fuel updates with the ship entity. */
    public void setChanged() {
        if (level() instanceof ServerLevel server && server.hasChunkAt(blockPosition())) {
            // The actual entity is saved by Minecraft's entity storage on world save;
            // dirtying the current level chunk also avoids relying on an empty hook.
            server.getChunkAt(blockPosition()).setUnsaved(true);
        }
    }
    public boolean isStopped() {return forward == 0 && turn == 0 && vertical == 0;}
    /** Virtual storage is accessible only to people actually near the ship. */
    private boolean canUseCargo(ServerPlayer player) {
        if (player.getVehicle() == this) return true;
        if (player.distanceToSqr(this) > 45.0 * 45.0 || Math.abs(player.getY()-getY()) > 28.0) {
            message(player,"Debes estar cerca del barco para usar sus sistemas.");
            return false;
        }
        return true;
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override public boolean isPickable() {return true;}
    @Override public boolean isInvulnerableTo(DamageSource source) {return true;}
    @Override public boolean hurt(DamageSource source, float amount) {return false;}
    @Override protected boolean canAddPassenger(Entity rider) {return rider instanceof Player && getPassengers().size() < (FRIEND_CAPACITY+1);}
    @Override protected boolean couldAcceptPassenger() {return getPassengers().size() < FRIEND_CAPACITY+1;}
    @Override public boolean isPushable() {return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance < 300.0 * 300.0;}

    @Override protected void positionRider(Entity rider, MoveFunction move) {
        if (!this.hasPassenger(rider)) return;
        boolean captain = pilotUUID != null && pilotUUID.equals(rider.getUUID());
        int friendIndex = 0;
        if (!captain) for (Entity other : this.getPassengers()) {
            if (other == rider) break;
            if (pilotUUID == null || !pilotUUID.equals(other.getUUID())) friendIndex++;
        }
        // Six positions near the schematic's central deck (y=15).
        double[][] seats = {{4.5,0}, {-5,2.5}, {-2,3.5}, {1.5,3.5}, {-3.5,6}, {0.5,6}};
        int slot = captain ? 0 : Math.min(5,friendIndex+1);
        double relX = seats[slot][0];
        double relZ = seats[slot][1];
        double angle = Math.toRadians(-getYRot());
        double dx = relX*Math.cos(angle)-relZ*Math.sin(angle);
        double dz = relX*Math.sin(angle)+relZ*Math.cos(angle);
        move.accept(rider,getX()+dx,getY()+1.25,getZ()+dz);
        // Other passengers may look around during flight; only the captain aligns with the helm.
        if (captain) rider.setYRot(getYRot());
    }

    public boolean hasKey(Player player) {
        for (ItemStack stack : player.getInventory().items) if (isKey(stack)) return true;
        for (ItemStack stack : player.getInventory().offhand) if (isKey(stack)) return true;
        return false;
    }
    public boolean isAuthorizedDismount(Player player) {
        return permittedDismountUUID != null && permittedDismountUUID.equals(player.getUUID());
    }
    /** Look for a real, already loaded landing square outside the broad visible hull.
     * No chunk generation and no changes to blocks. Coordinates are the player's FEET.
     */
    private BlockPos findSafeExit() {
        if (!(level() instanceof ServerLevel server)) return null;
        // Exterior locations around the 54x34 block envelope, not the ship's center.
        int[][] exits={{32,0},{-32,0},{0,22},{0,-22},{32,22},{-32,22},{32,-22},{-32,-22}};
        double radians=Math.toRadians(-getYRot());
        for (int[] offset : exits) {
            int px=Mth.floor(getX()+offset[0]*Math.cos(radians)-offset[1]*Math.sin(radians));
            int pz=Mth.floor(getZ()+offset[0]*Math.sin(radians)+offset[1]*Math.cos(radians));
            BlockPos probe=new BlockPos(px,Mth.floor(getY()),pz);
            if (!server.getWorldBorder().isWithinBounds(probe) || !server.getChunkSource().hasChunk(px>>4,pz>>4)) continue;
            BlockPos ground=server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,probe);
            double fall=getY()-(ground.getY()+1);
            if (fall>=0 && fall<=16 && ground.getY()>server.getMinBuildHeight()+1 && AirshipSafety.isSafeExit(server,ground)) {
                return ground.above();
            }
        }
        return null;
    }
    public boolean hasSafeLanding() {return findSafeExit()!=null;}
    private boolean isKey(ItemStack stack) {
        return stack.is(Items.TRIPWIRE_HOOK) && stack.hasTag() && stack.getTag().hasUUID("BarttoloShip")
            && getUUID().equals(stack.getTag().getUUID("BarttoloShip"));
    }
    public ItemStack makeKey() {
        ItemStack item = new ItemStack(Items.TRIPWIRE_HOOK);
        item.setHoverName(Component.literal("Llave del capitán • Barttolo").withStyle(ChatFormatting.GOLD));
        item.getOrCreateTag().putUUID("BarttoloShip",getUUID());
        item.getOrCreateTag().putBoolean("BarttoloGlow",true);
        net.minecraft.nbt.ListTag effects = new net.minecraft.nbt.ListTag();
        CompoundTag visualGlint = new CompoundTag();
        visualGlint.putString("id","minecraft:unbreaking");
        visualGlint.putShort("lvl",(short)1);
        effects.add(visualGlint);
        item.getOrCreateTag().put("Enchantments",effects);
        item.getOrCreateTag().putInt("HideFlags",1);
        return item;
    }
    /** A key-holding passenger can assume command when the captain left. */
    public boolean takeHelm(ServerPlayer player) {
        if (player.getVehicle()!=this) {
            message(player,"Primero aborda la nave.");
            return false;
        }
        if (!hasKey(player)) {
            message(player,"Hace falta la llave del capitán.");
            return false;
        }
        // Explicit takeover is allowed only when the previous captain is absent.
        if (pilotUUID!=null && getPassengers().stream().anyMatch(p->pilotUUID.equals(p.getUUID()))) {
            message(player,"El timón ya tiene un capitán.");
            return false;
        }
        pilotUUID=player.getUUID();
        forward=turn=vertical=0;
        message(player,"Ahora controlas el timón.");
        return true;
    }
    /** Remove an empty, installed cargo chest while safely stationary. */
    public void removeChest(ServerPlayer player,int index) {
        if (!canUseCargo(player)) return;
        if (index<0 || index>=4 || !hasChest(index)) {
            message(player,"Ese cofre no está instalado."); return;
        }
        if (!isStopped() || !hasSafeLanding()) {
            message(player,"Detén la nave cerca del suelo para retirar cofres."); return;
        }
        for (int slot=index*27;slot<(index+1)*27;slot++) {
            if (!inventory.get(slot).isEmpty()) {
                message(player,"Vacía primero el cofre para no perder objetos.");return;
            }
        }
        entityData.set(CHESTS,entityData.get(CHESTS)&~(1<<index));
        setChanged();
        ItemStack returned=new ItemStack(Items.CHEST);
        if (!player.getInventory().add(returned)) player.drop(returned,false);
        message(player,"Cofre retirado y devuelto al inventario.");
    }

    public boolean board(ServerPlayer player) {
        if (getPassengers().size() >= 6) {message(player,"Barco completo (1 piloto + 5 amigos)."); return false;}
        if (pilotUUID == null) {
            if (!hasKey(player)) {message(player,"Hace falta el capitán con su llave para salir.");return false;}
            if (player.startRiding(this,true)) {
                pilotUUID = player.getUUID();
                return true;
            }
            return false;
        }
        return player.startRiding(this,true);
    }
    @Override public InteractionResult interact(Player player,InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer operator)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            for (int i=0;i<4;i++) if (hasChest(i)) {openCargo(operator,i);return InteractionResult.CONSUME;}
            message(operator,"No hay cofres instalados. Coloca un cofre con clic derecho.");return InteractionResult.CONSUME;
        }
        if (stack.is(Items.CHEST)) {
            for(int i=0;i<4;i++) if (!hasChest(i)) {installChest(operator,i);return InteractionResult.CONSUME;}
            message(operator,"Ya hay cuatro cofres.");return InteractionResult.CONSUME;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK) || stack.is(Items.LAVA_BUCKET)) {
            addFuel(operator,stack);return InteractionResult.CONSUME;
        }
        board(operator);
        return InteractionResult.CONSUME;
    }
    public void installChest(ServerPlayer player,int index) {
        if (!canUseCargo(player)) return;
        if (index < 0 || index >= 4) return;
        if (!isStopped()) {message(player,"Detén el barco para colocar cofres.");return;}
        if (hasChest(index)) {message(player,"Ese lugar ya tiene un cofre.");return;}
        ItemStack held=player.getMainHandItem();
        if (!held.is(Items.CHEST)) {message(player,"Sostén un cofre en la mano principal.");return;}
        if (!player.getAbilities().instabuild) held.shrink(1);
        entityData.set(CHESTS,entityData.get(CHESTS)|(1<<index));
        setChanged();
        message(player,"Cofre "+(index+1)+" instalado (27 espacios).");
    }
    public void openCargo(ServerPlayer player,int index) {
        if (!canUseCargo(player)) return;
        if (index < 0 || index >= 4) return;
        if (!hasChest(index)) {message(player,"Primero instala el cofre "+(index+1)+".");return;}
        if (!isStopped()) {message(player,"Los cofres se abren sólo estando detenido.");return;}
        AirshipCargo cargo = new AirshipCargo(this,index);
        player.openMenu(new SimpleMenuProvider((id,inventory,user) -> ChestMenu.threeRows(id,inventory,cargo),
            Component.literal("Barco • Cofre "+(index+1))));
    }
    public void addFuel(ServerPlayer player,ItemStack held) {
        if (!canUseCargo(player)) return;
        if (!isStopped()) {message(player,"Detén el barco para recargar.");return;}
        int ticks;
        if (held.is(Items.LAVA_BUCKET)) ticks=20*60*15;
        else if (held.is(Items.COAL_BLOCK)) ticks=20*60*15;
        else if (held.is(Items.COAL) || held.is(Items.CHARCOAL)) ticks=20*60*2;
        else {message(player,"Admite carbón, carbón vegetal, bloque de carbón o lava.");return;}
        if (getFuel()+ticks>MAX_FUEL) {message(player,"Tanque demasiado lleno; consume combustible antes de recargar.");return;}
        boolean isLava=held.is(Items.LAVA_BUCKET);
        if (!player.getAbilities().instabuild) held.shrink(1);
        // Return an empty bucket on lava; never duplicate it when in creative mode.
        if (isLava && !player.getAbilities().instabuild) {
            ItemStack emptyBucket=new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(emptyBucket)) player.drop(emptyBucket,false);
        }
        entityData.set(FUEL,getFuel()+ticks);
        setChanged();
        message(player,"Combustible cargado: "+getFuel()/20+" segundos restantes.");
    }
    public void acceptInput(ServerPlayer player,int f,int t,int v) {
        if (pilotUUID == null || !pilotUUID.equals(player.getUUID()) || !hasKey(player) || player.getVehicle()!=this) return;
        // Input is accepted once per server tick, even for modified/spamming clients.
        if (tickCounter==lastPacketTick) return;
        lastPacketTick=tickCounter;
        this.forward = (byte)Mth.clamp(f,-1,1);
        this.turn = (byte)Mth.clamp(t,-1,1);
        this.vertical = (byte)Mth.clamp(v,-1,1);
        this.lastInputTick=tickCounter;
    }
    public void requestExit(ServerPlayer player) {
        if (!getPassengers().contains(player)) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (!isStopped() || !hasSafeLanding()) {
            message(player,"Detén la nave y aterriza cerca de suelo seguro para salir con V.");return;
        }
        BlockPos exitFeet=findSafeExit();
        if (exitFeet==null) {message(player,"No hay una salida segura.");return;}
        permittedDismountUUID=player.getUUID();
        try {player.stopRiding();}
        finally {permittedDismountUUID=null;}
        if (player.getVehicle()==this) return;
        player.teleportTo(exitFeet.getX()+0.5,exitFeet.getY(),exitFeet.getZ()+0.5);
        if (pilotUUID != null && pilotUUID.equals(player.getUUID())) {
            pilotUUID=null;forward=turn=vertical=0;
        }
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Safe fallback also protects against vanilla sneak-to-dismount.
        BlockPos exitFeet=findSafeExit();
        if(exitFeet!=null) return new Vec3(exitFeet.getX()+0.5,exitFeet.getY(),exitFeet.getZ()+0.5);
        return super.getDismountLocationForPassenger(passenger);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ++tickCounter;
        if (tickCounter-lastInputTick>8) forward=turn=vertical=0;
        if (pilotUUID!=null && getPassengers().stream().noneMatch(e->pilotUUID.equals(e.getUUID()))) {
            pilotUUID=null;forward=turn=vertical=0;
        }
        if (!(level() instanceof ServerLevel server)) return;
        boolean active=forward!=0 || turn!=0 || vertical!=0;
        if (getFuel() > 0 && active) {
            double heading=AirshipFlightMath.heading(getYRot(),turn);
            double dx=AirshipFlightMath.dx(heading,forward,MAX_SPEED);
            double dz=AirshipFlightMath.dz(heading,forward,MAX_SPEED);
            double dy=AirshipFlightMath.dy(vertical);
            double nx=getX()+dx, ny=Math.max(server.getMinBuildHeight()+18,Math.min(server.getMaxBuildHeight()-35,getY()+dy)), nz=getZ()+dz;
            // This is a 54 x 34 block ship, not a 1-block craft. Validate the whole
            // rotated visual envelope and the terrain beneath it. Do not force-load chunks.
            if (AirshipSafety.isAirspaceReady(server,nx,ny,nz,heading)) {
                setYRot((float)heading);
                setPos(nx,ny,nz);
                entityData.set(FUEL,getFuel()-1);
                if (tickCounter % 20 == 0) setChanged();
            } else {forward=turn=vertical=0;}
        } else if (getFuel() <= 0) {
            forward=turn=vertical=0;
            // No uncontrolled free fall. Ship holds position until refueled.
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(CHESTS,tag.getInt("InstalledChests") & 15);
        entityData.set(FUEL,Mth.clamp(tag.getInt("Fuel"),0,MAX_FUEL));
        ContainerHelper.loadAllItems(tag,inventory);
        pilotUUID=null; // no stale piloting after restart
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("InstalledChests",entityData.get(CHESTS));
        tag.putInt("Fuel",getFuel());
        ContainerHelper.saveAllItems(tag,inventory);
    }
    public static void message(ServerPlayer player,String message) {player.displayClientMessage(Component.literal("[Barttolo Airships] "+message),false);}
}
