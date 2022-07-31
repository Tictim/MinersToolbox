package tictim.minerstoolbox.progressivemining;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class MiningProgressionTracker implements ICapabilitySerializable<CompoundTag>{
	public static final Capability<MiningProgressionTracker> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

	public static MiningProgressionTracker get(Level level, BlockPos pos){
		LevelChunk c = level.getChunkAt(pos);
		return c.getCapability(CAPABILITY).orElseThrow(() -> new RuntimeException("unexpected"));
	}

	private final Object2IntMap<BlockPos> posToSubstage = new Object2IntOpenHashMap<>();

	public void set(BlockPos pos, int substage){
		if(substage<=0) posToSubstage.removeInt(pos);
		else posToSubstage.put(pos, substage);
	}
	public int get(BlockPos pos){
		return posToSubstage.getInt(pos);
	}


	@Nonnull @Override public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side){
		return null;
	}
	@Override public CompoundTag serializeNBT(){
		return null;
	}
	@Override public void deserializeNBT(CompoundTag nbt){

	}
}
