package net.barttolo.airships;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded, read-only safety queries. Never generates or modifies chunks. */
public final class AirshipSafety {
    private AirshipSafety() {}

    public static boolean isAirspaceReady(ServerLevel level,double x,double y,double z,double yaw) {
        if (!AirshipSafetyMath.isValidAltitude(y,level.getMinBuildHeight(),level.getMaxBuildHeight())) return false;
        int halfX=AirshipSafetyMath.extentX(yaw), halfZ=AirshipSafetyMath.extentZ(yaw);
        int lowX=(int)Math.floor(x-halfX),highX=(int)Math.ceil(x+halfX);
        int lowZ=(int)Math.floor(z-halfZ),highZ=(int)Math.ceil(z+halfZ);
        // All potentially visible chunks must already be loaded. Never call getChunk() here.
        for(int cx=lowX>>4;cx<=highX>>4;cx++) for(int cz=lowZ>>4;cz<=highZ>>4;cz++) {
            if(!level.getChunkSource().hasChunk(cx,cz))return false;
        }
        // 25 terrain probes around the large hull; do not claim exact block collision.
        // Probes are bounded and only run inside the already loaded footprint.
        for(int ax : AirshipSafetyMath.samples(lowX,highX)) {
            for(int az : AirshipSafetyMath.samples(lowZ,highZ)) {
                BlockPos point=new BlockPos(ax,(int)Math.floor(y),az);
                if(!level.getWorldBorder().isWithinBounds(point))return false;
                // Conservative terrain clearance; samples 9 points beneath the hull.
                BlockPos ground=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,point);
                if(!AirshipSafetyMath.hasClearance(ground.getY(),y))return false;
            }
        }
        return true;
    }
    /** Only disembark when two body blocks are free and the ground isn't liquid. */
    public static boolean isSafeExit(ServerLevel level,BlockPos ground) {
        BlockPos feet=ground.above(), head=feet.above();
        return level.getWorldBorder().isWithinBounds(ground)
            && level.hasChunkAt(ground)
            && level.getFluidState(ground.below()).isEmpty()
            && level.getFluidState(feet).isEmpty()
            && level.getFluidState(head).isEmpty()
            && level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()
            && level.getBlockState(head).getCollisionShape(level,head).isEmpty();
    }
}
