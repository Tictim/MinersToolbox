package tictim.minerstoolbox.progressivemining;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public final class Progression{
	public final Block block;
	public int substage;
	public boolean save;

	public Progression(Block block, int substage, boolean save){
		this.block = block;
		this.substage = substage;
		this.save = save;
	}

	@Nullable public static Progression read(CompoundTag tag){
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("block"));
		if(id!=null){
			Block block = ForgeRegistries.BLOCKS.getValue(id);
			if(block!=null&&block!=Blocks.AIR){
				int substage = tag.getInt("substage");
				if(substage>0) return new Progression(block, substage, true);
			}
		}
		return null;
	}

	public void write(CompoundTag tag){
		ResourceLocation n = block.getRegistryName();
		if(n!=null){
			tag.putString("block", n.toString());
			tag.putInt("substage", substage);
		}
	}

	@Override public String toString(){
		return "["+block.getRegistryName()+", "+substage+"]";
	}
}
