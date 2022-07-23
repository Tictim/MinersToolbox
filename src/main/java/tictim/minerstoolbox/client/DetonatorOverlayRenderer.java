package tictim.minerstoolbox.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tictim.minerstoolbox.contents.item.DetonatorItem;

import java.util.List;
import java.util.stream.Collectors;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class DetonatorOverlayRenderer{
	private static final MultiBufferSource.BufferSource fucks = MultiBufferSource.immediate(new BufferBuilder(256));

	@SuppressWarnings("ConstantConditions")
	private static List<DetonatorItem.DimPos> getPositions(){
		Minecraft mc = Minecraft.getInstance();
		if(mc.player!=null&&mc.level!=null){
			DetonatorItem.Data data = mc.player.getMainHandItem().getCapability(DetonatorItem.CAPABILITY).orElseGet(
					() -> mc.player.getOffhandItem().getCapability(DetonatorItem.CAPABILITY).orElse(null));
			if(data==null||data.isEmpty()) return List.of();
			ResourceLocation dim = mc.level.dimension().location();
			return data.positions().stream().filter(dimPos -> dim.equals(dimPos.dim())).collect(Collectors.toList());
		}
		return List.of();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelLast(RenderLevelLastEvent event){
		Minecraft mc = Minecraft.getInstance();
		if(mc.level==null) return;
		List<DetonatorItem.DimPos> positions = getPositions();
		if(positions.isEmpty()) return;

		PoseStack pose = event.getPoseStack();
		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		for(DetonatorItem.DimPos dimPos : positions){
			if(!mc.level.dimension().location().equals(dimPos.dim())) continue;
			BlockPos pos = dimPos.pos();
			if(!mc.level.isLoaded(pos)) continue;
			BlockState state = mc.level.getBlockState(pos);
			if(!DetonatorItem.isExplosive(state)) continue;
			pose.pushPose();
			pose.translate(pos.getX()-cameraPos.x, pos.getY()-cameraPos.y, pos.getZ()-cameraPos.z);
			mc.getBlockRenderer().renderSingleBlock(state,
					pose, fucks,
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
			pose.popPose();
		}
		fucks.endBatch();
	}
}
