package tictim.minerstoolbox.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class SyncMiningProgressMsg{
	private final Object2IntOpenHashMap<BlockPos> map = new Object2IntOpenHashMap<>();

	public static SyncMiningProgressMsg read(FriendlyByteBuf buf){
		SyncMiningProgressMsg msg = new SyncMiningProgressMsg();
		for(int i = 0, j = buf.readVarInt(); i<j; i++)
			msg.add(buf.readBlockPos(), buf.readVarInt());
		return msg;
	}

	public Object2IntMap<BlockPos> map(){
		return Object2IntMaps.unmodifiable(map);
	}

	public void add(BlockPos pos, int progression){
		map.put(pos, progression);
	}

	public void write(FriendlyByteBuf buf){
		buf.writeVarInt(map.size());
		for(Object2IntMap.Entry<BlockPos> e : map.object2IntEntrySet()){
			buf.writeBlockPos(e.getKey());
			buf.writeVarInt(e.getIntValue());
		}
	}
}
