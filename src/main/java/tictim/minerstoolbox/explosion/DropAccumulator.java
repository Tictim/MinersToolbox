package tictim.minerstoolbox.explosion;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public final class DropAccumulator{
	private final Long2ObjectMap<Column> map = new Long2ObjectOpenHashMap<>();

	public void accumulate(BlockPos pos, ItemStack stack){
		map.computeIfAbsent(xz(pos), xz -> new Column()).add(pos, stack);
	}

	public void spawn(Level level){
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for(Long2ObjectMap.Entry<Column> e : map.long2ObjectEntrySet()){
			mpos.set(x(e.getLongKey()), e.getValue().minY, z(e.getLongKey()));
			for(ItemStack stack : e.getValue().stacks)
				Block.popResource(level, mpos, stack);
		}
	}

	private static long xz(BlockPos pos){
		return Integer.toUnsignedLong(pos.getX())<<32|Integer.toUnsignedLong(pos.getZ());
	}
	private static int x(long xz){
		return (int)(xz >> 32);
	}
	private static int z(long xz){
		return (int)xz;
	}

	public static final class Column{
		private int minY = Integer.MAX_VALUE;
		private final List<ItemStack> stacks = new ArrayList<>();

		public void add(BlockPos pos, ItemStack stack){
			if(minY>pos.getY()) minY = pos.getY();
			for(int i = 0; i<stacks.size(); i++){
				if(ItemEntity.areMergable(stacks.get(i), stack)){
					stacks.set(i, ItemEntity.merge(stacks.get(i), stack, 64));
					if(stack.isEmpty()) return;
				}
			}
			stacks.add(stack);
		}
	}
}
