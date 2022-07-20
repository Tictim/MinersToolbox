package datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import tictim.minerstoolbox.contents.Contents;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

public class ItemModelGen extends ItemModelProvider{
	public ItemModelGen(DataGenerator generator, ExistingFileHelper existingFileHelper){
		super(generator, MODID, existingFileHelper);
	}

	@Override protected void registerModels(){
		explosive(Contents.CRUDE_EXPLOSIVE_ITEM.getId().getPath(), "crude");
		explosive(Contents.IMPROVED_EXPLOSIVE_ITEM.getId().getPath(), "improved");
		explosive(Contents.ENHANCED_EXPLOSIVE_ITEM.getId().getPath(), "enhanced");
		explosive(Contents.SUPERB_EXPLOSIVE_ITEM.getId().getPath(), "superb");
		explosive(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_ITEM.getId().getPath(), "supercalifragilisticexpialidocious");
		basicItem(Contents.DETONATOR.get())
				.override().predicate(new ResourceLocation("pushed"), 1).model(basicItem(new ResourceLocation(MODID, "detonator_pushed"))).end()
				.override().predicate(new ResourceLocation("marked"), 1).model(basicItem(new ResourceLocation(MODID, "detonator_on")));
		getBuilder(Contents.TERRAIN_INSPECTOR.getId().getPath())
				.parent(new ModelFile.ExistingModelFile(new ResourceLocation("item/handheld"), existingFileHelper))
				.texture("layer0", new ResourceLocation(MODID, "item/"+Contents.TERRAIN_INSPECTOR.getId().getPath()));

		basicItem(Contents.CRUDE_EXPLOSIVE_POWDER.get());
		basicItem(Contents.ENHANCED_EXPLOSIVE_POWDER.get());
		basicItem(Contents.SUPERB_EXPLOSIVE_POWDER.get());
		basicItem(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_POWDER.get());
	}

	private void explosive(String item, String name){
		getBuilder(item)
				.parent(new ModelFile.ExistingModelFile(new ResourceLocation(MODID, "item/explosive_template"), existingFileHelper))
				.texture("texture", new ResourceLocation(MODID, "block/explosives/"+name));
	}
}
