package tictim.minerstoolbox.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tictim.minerstoolbox.MinersToolboxMod;
import tictim.minerstoolbox.contents.item.DetonatorItem;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class CommonEventHandler{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlaced(BlockEvent.EntityPlaceEvent event){
		if(event.getEntity() instanceof Player p&&DetonatorItem.isExplosive(event.getPlacedBlock())){
			for(int i = 0; i<9; i++)
				markExplosive(p, p.getInventory().getItem(i), event.getBlockSnapshot().getPos());
			markExplosive(p, p.getInventory().getItem(Inventory.SLOT_OFFHAND), event.getBlockSnapshot().getPos());
		}
	}

	private static void markExplosive(Player p, ItemStack stack, BlockPos pos){
		if(stack.isEmpty()) return;
		DetonatorItem.Data data = stack.getCapability(DetonatorItem.CAPABILITY).orElse(null);
		if(data==null) return;
		data.add(p.level, pos);
	}

	@SubscribeEvent
	public static void debugExplosionDamage(LivingAttackEvent event){
		if(event.getSource().isExplosion()&&event.getEntity() instanceof Player player&&player.isCreative()){
			MinersToolboxMod.LOGGER.info("[{}] dmg: {}", player.position(), event.getAmount());
		}
	}

	@SubscribeEvent
	public static void attachChunkCapability(AttachCapabilitiesEvent<LevelChunk> event){

	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event){
		if(event.getPlayer().isCreative()||event.getPlayer().level.isClientSide) return;


	}
}
