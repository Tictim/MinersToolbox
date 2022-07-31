package tictim.minerstoolbox;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import tictim.minerstoolbox.config.Cfgs;
import tictim.minerstoolbox.network.ExplosionMsg;
import tictim.minerstoolbox.network.SyncProgressiveMiningConfigMsg;

import java.util.Optional;
import java.util.function.Supplier;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

public class MinersToolboxNetwork{
	public static final String VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> VERSION, VERSION::equals, VERSION::equals);

	public static void register(){
		CHANNEL.registerMessage(0, ExplosionMsg.class,
				ExplosionMsg::write, ExplosionMsg::read, MinersToolboxNetwork::handleExplosion,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(1, SyncProgressiveMiningConfigMsg.class,
				SyncProgressiveMiningConfigMsg::write, SyncProgressiveMiningConfigMsg::read, MinersToolboxNetwork::handleProgressiveMiningConfigMsg,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}
	private static void handleExplosion(ExplosionMsg msg, Supplier<NetworkEvent.Context> supplier){
		NetworkEvent.Context ctx = supplier.get();
		ctx.setPacketHandled(true);
		ctx.enqueueWork(() -> Client.handleExplosion(msg));
	}

	private static void handleProgressiveMiningConfigMsg(SyncProgressiveMiningConfigMsg msg, Supplier<NetworkEvent.Context> supplier){
		NetworkEvent.Context ctx = supplier.get();
		ctx.setPacketHandled(true);
		ctx.enqueueWork(() -> Cfgs.setRemoteProgressiveMiningConfig(msg.config()));
	}

	private static class Client{
		private static void handleExplosion(ExplosionMsg msg){
			Minecraft mc = Minecraft.getInstance();
			if(mc.level==null) return;

			BlockPos origin = msg.origin();
			mc.level.playLocalSound(origin.getX()+.5, origin.getY()+.5, origin.getZ()+.5, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4, (1+(mc.level.random.nextFloat()-mc.level.random.nextFloat())*.2f)*.7f, false);

			mc.level.addParticle(
					msg.radius()<2||msg.blockInteraction()==BlockInteraction.NONE ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER,
					origin.getX()+.5, origin.getY()+.5, origin.getZ()+.5, 1, 0, 0);
		}
	}
}
