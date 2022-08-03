package tictim.minerstoolbox.progressivemining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import tictim.minerstoolbox.MinersToolboxMod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MiningProgressionTracker implements ICapabilitySerializable<CompoundTag>{
	public static final Capability<MiningProgressionTracker> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

	public static MiningProgressionTracker get(Level level, ChunkPos pos){
		return unwrap(level.getChunk(pos.x, pos.z));
	}
	public static MiningProgressionTracker get(Level level, BlockPos pos){
		return unwrap(level.getChunkAt(pos));
	}
	private static MiningProgressionTracker unwrap(ICapabilityProvider provider){
		return provider.getCapability(CAPABILITY).orElseThrow(() -> new RuntimeException("unexpected"));
	}

	private final Map<BlockPos, Progression> map = new HashMap<>();

	public void set(BlockPos pos, Block block, int substage, boolean save){
		MinersToolboxMod.LOGGER.debug("Adding {}: {} {} {}", pos, block.getRegistryName(), substage, save); // TODO
		if(substage<=0) map.remove(pos);
		else map.put(pos, new Progression(block, substage, save));
	}
	public Progression get(BlockPos pos){
		return map.get(pos);
	}
	public void remove(BlockPos pos){
		MinersToolboxMod.LOGGER.debug("Removing {}", pos); // TODO
		map.remove(pos);
	}

	public Map<BlockPos, Progression> map(){
		return Collections.unmodifiableMap(map);
	}

	@Nullable private LazyOptional<MiningProgressionTracker> self = null;

	@Nonnull @Override public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side){
		if(cap==CAPABILITY){
			if(self==null) self = LazyOptional.of(() -> this);
			return self.cast();
		}else return LazyOptional.empty();
	}

	@Override public CompoundTag serializeNBT(){
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		for(Map.Entry<BlockPos, Progression> e : map.entrySet()){
			if(!e.getValue().save) continue;
			CompoundTag tag2 = NbtUtils.writeBlockPos(e.getKey());
			e.getValue().write(tag2);
			list.add(tag2);
		}
		tag.put("list", list);
		return tag;
	}
	@Override public void deserializeNBT(CompoundTag tag){
		map.clear();
		ListTag list = tag.getList("list", Tag.TAG_COMPOUND);
		for(int i = 0; i<list.size(); i++){
			CompoundTag tag2 = list.getCompound(i);
			Progression p = Progression.read(tag2);
			if(p!=null) map.put(NbtUtils.readBlockPos(tag2), p);
		}
	}
}
