package tictim.minerstoolbox.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.minerstoolbox.progressivemining.DestroyResult;
import tictim.minerstoolbox.progressivemining.ProgressiveMining;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode{
	@Inject(method = "net.minecraft.client.multiplayer.MultiPlayerGameMode.destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
			at = @At(value = "INVOKE",
					target = "net.minecraft.world.level.Level.getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"),
			cancellable = true)
	public void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci){
		Minecraft mc = Minecraft.getInstance();
		if(ProgressiveMining.destroyBlock(mc.level, pos, mc.player) instanceof DestroyResult.Success)
			ci.setReturnValue(true);
	}
}
