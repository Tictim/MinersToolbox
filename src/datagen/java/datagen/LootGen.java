package datagen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;
import tictim.minerstoolbox.contents.Contents;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LootGen extends LootTableProvider{
	public LootGen(DataGenerator gen){
		super(gen);
	}

	@Override protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables(){
		return List.of(
				Pair.of(BlockTables::new, LootContextParamSets.BLOCK)
		);
	}

	@Override protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker){}

	public static class BlockTables extends BlockLoot{
		@Override protected void addTables(){
			dropSelf(Contents.CRUDE_EXPLOSIVE.get());
			dropSelf(Contents.IMPROVED_EXPLOSIVE.get());
			dropSelf(Contents.ENHANCED_EXPLOSIVE.get());
			dropSelf(Contents.SUPERB_EXPLOSIVE.get());
			dropSelf(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE.get());
		}

		@Override protected Iterable<Block> getKnownBlocks(){
			return Contents.BLOCKS.getEntries().stream().map(RegistryObject::get).collect(Collectors.toList());
		}
	}
}
