package tictim.minerstoolbox.network;

import net.minecraft.network.FriendlyByteBuf;
import tictim.minerstoolbox.config.ProgressiveMiningConfig;

public record SyncProgressiveMiningConfigMsg(ProgressiveMiningConfig config){
	public void write(FriendlyByteBuf buf){
		config.write(buf);
	}

	public static SyncProgressiveMiningConfigMsg read(FriendlyByteBuf buf){
		return new SyncProgressiveMiningConfigMsg(ProgressiveMiningConfig.read(buf));
	}
}
