package tictim.minerstoolbox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import tictim.minerstoolbox.contents.entity.ExplosiveEntity;

import static net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class ExplosiveEntityRenderer extends EntityRenderer<ExplosiveEntity>{
	private final BlockState defaultState;
	public ExplosiveEntityRenderer(EntityRendererProvider.Context ctx, BlockState defaultState){
		super(ctx);
		this.defaultState = defaultState;
	}
	@Override public ResourceLocation getTextureLocation(ExplosiveEntity e){
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override public void render(ExplosiveEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight){
		pose.pushPose();
		pose.translate(0.0D, 0.5D, 0.0D);
		int fuse = entity.getFuse();
		if((float)fuse-partialTick+1.0F<10.0F){
			float wtf = 1.0F-((float)fuse-partialTick+1.0F)/10.0F;
			wtf = Mth.clamp(wtf, 0.0F, 1.0F);
			wtf *= wtf;
			wtf *= wtf;
			float scale = 1.0F+wtf*0.3F;
			pose.scale(scale, scale, scale);
		}

		pose.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
		pose.translate(-0.5D, -0.5D, 0.5D);
		pose.mulPose(Vector3f.YP.rotationDegrees(90.0F));
		TntMinecartRenderer.renderWhiteSolidBlock(defaultState
						.setValue(FACE, entity.getAttachFace())
						.setValue(HORIZONTAL_FACING, entity.getHorizontalFacing()),
				pose, buffer, packedLight, fuse/5%2==0);
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffer, packedLight);
	}
}
