package net.barttolo.airships;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Four discrete 27-slot inventories, stored in AirshipEntity's NBT. */
public final class AirshipCargo implements Container {
    private final AirshipEntity ship;
    private final int chestIndex;
    public AirshipCargo(AirshipEntity ship, int chestIndex) {this.ship = ship;this.chestIndex = chestIndex;}
    private int offset(int slot) {return chestIndex * 27 + slot;}
    private NonNullList<ItemStack> contents() {return ship.items();}
    @Override public int getContainerSize() {return 27;}
    @Override public boolean isEmpty() {
        for (int i=0; i<27; i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) {return contents().get(offset(slot));}
    @Override public ItemStack removeItem(int slot,int amount) {
        ItemStack removed = ContainerHelper.removeItem(contents(), offset(slot), amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(contents(),offset(slot));
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public void setItem(int slot,ItemStack stack) {
        contents().set(offset(slot),stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public void setChanged() {ship.setChanged();}
    @Override public boolean stillValid(Player player) {
        return !ship.isRemoved() && ship.isStopped() && (player.getVehicle()==ship || (player.distanceToSqr(ship)<=45*45 && Math.abs(player.getY()-ship.getY())<=28)) && ship.hasChest(chestIndex);
    }
    @Override public void clearContent() {for(int i=0;i<27;i++)setItem(i,ItemStack.EMPTY);}
}
