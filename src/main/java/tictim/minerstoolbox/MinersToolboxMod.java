package tictim.minerstoolbox;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tictim.minerstoolbox.client.ExplosiveEntityRenderer;
import tictim.minerstoolbox.client.MaterialInspectorOverlay;
import tictim.minerstoolbox.contents.Contents;
import tictim.minerstoolbox.contents.item.DetonatorItem;

@Mod(MinersToolboxMod.MODID)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class MinersToolboxMod{
	public static final String MODID = "minerstoolbox";
	public static final Logger LOGGER = LogManager.getLogger("MinersToolbox");

	//private static Boolean aprilFools;
//
	//public static boolean isAprilFools(){
	//	if(aprilFools==null){
	//		if(Cfgs.aprilFools) aprilFools = true;
	//		else{
	//		    Calendar c = Calendar.getInstance();
	//		    aprilFools = c.get(Calendar.MONTH)==Calendar.APRIL&&c.get(Calendar.DATE)==1;
	//		}
	//	}
	//	return aprilFools;
	//}
	public static boolean isAprilFools(){
		return false;
	}

	public MinersToolboxMod(){
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		Contents.BLOCKS.register(bus);
		Contents.ITEMS.register(bus);
		Contents.ENTITIES.register(bus);
		Contents.SOUND_EVENTS.register(bus);
		MinersToolboxNetwork.register();
	}

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event){
		OverlayRegistry.registerOverlayTop(MODID+".terrain_inspector", new MaterialInspectorOverlay());
		event.enqueueWork(() -> {
			EntityRenderers.register(Contents.CRUDE_EXPLOSIVE_ENTITY.get(), ctx -> new ExplosiveEntityRenderer(ctx, Contents.CRUDE_EXPLOSIVE.get().defaultBlockState()));
			EntityRenderers.register(Contents.IMPROVED_EXPLOSIVE_ENTITY.get(), ctx -> new ExplosiveEntityRenderer(ctx, Contents.IMPROVED_EXPLOSIVE.get().defaultBlockState()));
			EntityRenderers.register(Contents.ENHANCED_EXPLOSIVE_ENTITY.get(), ctx -> new ExplosiveEntityRenderer(ctx, Contents.ENHANCED_EXPLOSIVE.get().defaultBlockState()));
			EntityRenderers.register(Contents.SUPERB_EXPLOSIVE_ENTITY.get(), ctx -> new ExplosiveEntityRenderer(ctx, Contents.SUPERB_EXPLOSIVE.get().defaultBlockState()));
			EntityRenderers.register(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_ENTITY.get(), ctx -> new ExplosiveEntityRenderer(ctx, Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE.get().defaultBlockState()));
			ItemProperties.register(Contents.DETONATOR.get(), new ResourceLocation("pushed"),
					(stack, level, entity, seed) -> entity instanceof Player p&&p.getCooldowns().isOnCooldown(Contents.DETONATOR.get()) ? 1 : 0);
			ItemProperties.register(Contents.DETONATOR.get(), new ResourceLocation("marked"),
					(stack, level, entity, seed) -> {
						if(entity instanceof Player){
							DetonatorItem.Data data = stack.getCapability(DetonatorItem.CAPABILITY).orElse(null);
							if(data!=null&&!data.isEmpty()) return 1;
						}
						return 0;
					});
		});
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event){
		event.register(DetonatorItem.Data.class);
	}
}
