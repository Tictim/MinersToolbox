package tictim.minerstoolbox.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Explosion.BlockInteraction;

public record ExplosionMsg(BlockPos origin, float radius, BlockInteraction blockInteraction){
	public void write(FriendlyByteBuf buf){
		buf.writeBlockPos(origin);
		buf.writeFloat(radius);
		buf.writeEnum(blockInteraction);
	}

	public static ExplosionMsg read(FriendlyByteBuf buf){
		return new ExplosionMsg(buf.readBlockPos(), buf.readFloat(), buf.readEnum(BlockInteraction.class));
	}
}
