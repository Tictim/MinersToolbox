package datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Bus.MOD)
public class Datagen{
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event){
		DataGenerator gen = event.getGenerator();
		ExistingFileHelper ex = event.getExistingFileHelper();
		if(event.includeServer()){
			gen.addProvider(new LootGen(gen));
			gen.addProvider(new RecipeGen(gen));
		}
		if(event.includeClient()){
			gen.addProvider(new BlockStateGen(gen, ex));
			gen.addProvider(new ItemModelGen(gen, ex));
		}
	}
}
