package net.barttolo.airships.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.barttolo.airships.AirshipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.Mth;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Render the user's exact 54 x 49 x 34 source as fixed voxels, never place them as world blocks. */
public final class AirshipRenderer extends EntityRenderer<AirshipEntity> {
    private static final String ROOT="/assets/barttoloairships/models/";
    private static final record Voxel(int x,int y,int z,int type) {}
    private static final List<Voxel> VOXELS = new ArrayList<>();
    private static BlockState[] palette;
    private static int width=54,length=34;

    public AirshipRenderer(EntityRendererProvider.Context context) {
        super(context);
        if (palette == null) loadModel();
        this.shadowRadius=0;
    }
    private static void loadModel() {
        try (InputStream p=AirshipRenderer.class.getResourceAsStream(ROOT+"airship_palette.txt");
             InputStream model=AirshipRenderer.class.getResourceAsStream(ROOT+"airship_model.bin")) {
            if (p==null || model==null) throw new IllegalStateException("Missing embedded airship geometry");
            String[] lines=new String(p.readAllBytes(), StandardCharsets.UTF_8).trim().split("\\R");
            palette=new BlockState[lines.length];
            for (int i=0;i<lines.length;i++) palette[i]=parseBlock(lines[i]);
            DataInputStream data=new DataInputStream(model);
            width=data.readUnsignedShort();data.readUnsignedShort();length=data.readUnsignedShort();
            int count=data.readInt();
            if(count>10000)throw new IllegalStateException("Abnormally large schematic");
            for(int j=0;j<count;j++)VOXELS.add(new Voxel(data.readUnsignedByte(),data.readUnsignedByte(),data.readUnsignedByte(),data.readUnsignedByte()));
        } catch (Exception e) {throw new IllegalStateException("Cannot load Barttolo's embedded airship model",e);}
    }
    private static BlockState parseBlock(String token) {
        int begin=token.indexOf('[');
        String id=begin<0?token:token.substring(0,begin);
        Block block=BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
        BlockState state=block.defaultBlockState();
        if(begin>0 && token.endsWith("]")) {
            String props=token.substring(begin+1,token.length()-1);
            for(String assignment:props.split(",")) {
                String[] parts=assignment.split("=",2);
                if(parts.length==2) {
                    Property<?> prop=block.getStateDefinition().getProperty(parts[0]);
                    if(prop!=null)state=applyProperty(state,prop,parts[1]);
                }
            }
        }
        return state;
    }
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state,Property<T> prop,String value) {
        return prop.getValue(value).map(x->state.setValue(prop,x)).orElse(state);
    }
    @Override public void render(AirshipEntity ship,float entityYaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
        if(palette==null)loadModel();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick,ship.yRotO,ship.getYRot())));
        pose.translate(-width/2.0,-15.0,-length/2.0);
        // Model is fixed, never placed into RingWorld. Draw fewer voxels at medium distance.
        double distSquared=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(ship.position());
        final int lod=distSquared>140.0*140.0 ? 2 : (distSquared>75.0*75.0 ? 1 : 0);
        for(Voxel v:VOXELS) {
            // Frame budget: near 2474 voxels; medium approximately 1477; far ~458.
            // No gameplay or block changes; distant geometry is sampled for rendering only.
            if (lod == 1 && !((v.type==17 && (v.x+v.z)%2==0) ||
                              (v.type!=17 && (v.x%2==0 || v.z%2==0)))) continue;
            if (lod == 2 && !((v.type==17 && v.x%3==0 && v.y%2==0) ||
                              (v.type!=17 && v.y%3==0 && v.z%2==0))) continue;
            if(v.type<=0 || v.type>=palette.length)continue;
            pose.pushPose();
            pose.translate(v.x,v.y,v.z);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(palette[v.type],pose,buffers,
                LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        // Display 0-4 portable chests at fixed, protected deck attachment points.
        int[][] chestPoints={{23,16,22},{26,16,22},{29,16,22},{32,16,22}};
        for(int i=0;i<4;i++) if(ship.hasChest(i)) {
            int[] spot=chestPoints[i];
            pose.pushPose();
            pose.translate(spot[0]+0.5,spot[1]+0.5,spot[2]+0.5);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.CHEST),
                ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,
                pose,buffers,ship.level(),ship.getId()+i);
            pose.popPose();
        }
        pose.popPose();
        super.render(ship,entityYaw,partialTick,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(AirshipEntity ship) {return InventoryMenu.BLOCK_ATLAS;}
    @Override public boolean shouldRender(AirshipEntity ship, net.minecraft.client.renderer.culling.Frustum frustum, double x,double y,double z) {
        // The model extends beyond the entity's small interaction hitbox.
        return x*x+y*y+z*z < 250*250;
    }
}
