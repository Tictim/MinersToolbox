package datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import tictim.minerstoolbox.contents.Contents;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

public class BlockStateGen extends BlockStateProvider{
	public BlockStateGen(DataGenerator gen, ExistingFileHelper exFileHelper){
		super(gen, MODID, exFileHelper);
	}

	@Override protected void registerStatesAndModels(){
		explosive(Contents.CRUDE_EXPLOSIVE.get(), "crude");
		explosive(Contents.IMPROVED_EXPLOSIVE.get(), "improved");
		explosive(Contents.ENHANCED_EXPLOSIVE.get(), "enhanced");
		explosive(Contents.SUPERB_EXPLOSIVE.get(), "superb");
		explosive(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE.get(), "supercalifragilisticexpialidocious");
	}

	private void explosive(Block block, String name){
		ModelFile model = models().singleTexture("block/explosives/"+name,
				new ResourceLocation(MODID, "block/explosive_template"),
				new ResourceLocation(MODID, "block/explosives/"+name));

		getVariantBuilder(block).forAllStates(state -> {
			Direction facing = state.getValue(ButtonBlock.FACING);
			AttachFace face = state.getValue(ButtonBlock.FACE);

			return ConfiguredModel.builder()
					.modelFile(model)
					.rotationX(face==AttachFace.FLOOR ? 0 : face==AttachFace.WALL ? 270 : 180)
					.rotationY((int)(face==AttachFace.CEILING||face==AttachFace.WALL ? facing : facing.getOpposite()).toYRot())
					.build();
		});
	}
}
