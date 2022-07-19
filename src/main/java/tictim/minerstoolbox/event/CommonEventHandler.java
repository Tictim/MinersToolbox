package tictim.minerstoolbox.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tictim.minerstoolbox.contents.item.DetonatorItem;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class CommonEventHandler{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlaced(BlockEvent.EntityPlaceEvent event){
		if(event.getEntity() instanceof Player p&&DetonatorItem.isExplosive(event.getPlacedBlock())){
			for(int i = 0; i<9; i++){
				ItemStack item = p.getInventory().getItem(i);
				if(item.isEmpty()) continue;
				DetonatorItem.Data data = item.getCapability(DetonatorItem.CAPABILITY).orElse(null);
				if(data==null) continue;
				data.add(p.level, event.getBlockSnapshot().getPos());
			}
		}
	}
}
