package tictim.minerstoolbox.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import tictim.minerstoolbox.config.ExplosionStats;
import tictim.minerstoolbox.contents.Contents;

import static net.minecraft.ChatFormatting.*;

public class MaterialInspectorOverlay implements IIngameOverlay{
	@Override public void render(ForgeIngameGui gui, PoseStack poseStack, float partialTick, int width, int height){
		Minecraft mc = Minecraft.getInstance();
		if(mc.level!=null&&
				mc.player!=null&&
				isHoldingInspector(mc.player)&&
				mc.hitResult!=null&&
				mc.hitResult instanceof BlockHitResult hit){
			BlockPos pos = hit.getBlockPos();
			BlockState state = mc.level.getBlockState(pos);
			if(state.isAir()) return;
			//noinspection deprecation
			float res = state.getBlock().getExplosionResistance();
			String s = I18n.get("item.minerstoolbox.terrain_inspector.overlay",
					GREEN+""+res+RESET, getExplosionTier(res));
			//noinspection IntegerDivisionInFloatingPointContext
			gui.getFont().draw(poseStack, s, width/2-gui.getFont().width(s)/2, height/2+10, 0xFFFFFFFF);
		}
	}

	private static boolean isHoldingInspector(Player player){
		return isInspector(player.getMainHandItem())||isInspector(player.getOffhandItem());
	}

	private static boolean isInspector(ItemStack stack){
		return !stack.isEmpty()&&stack.getItem()==Contents.TERRAIN_INSPECTOR.get();
	}

	private static String getExplosionTier(float res){
		if(res<ExplosionStats.CRUDE.maxResistance())
			return GOLD+I18n.get("item.minerstoolbox.terrain_inspector.overlay.tier.crude")+RESET;
		else if(res<ExplosionStats.IMPROVED.maxResistance())
			return RED+I18n.get("item.minerstoolbox.terrain_inspector.overlay.tier.improved")+RESET;
		else if(res<ExplosionStats.ENHANCED.maxResistance())
			return YELLOW+I18n.get("item.minerstoolbox.terrain_inspector.overlay.tier.enhanced")+RESET;
		else if(res<ExplosionStats.SUPERB.maxResistance())
			return DARK_PURPLE+I18n.get("item.minerstoolbox.terrain_inspector.overlay.tier.superb")+RESET;
		else
			return DARK_GRAY+I18n.get("item.minerstoolbox.terrain_inspector.overlay.tier.unbreakable")+RESET;
	}
}
