package tictim.minerstoolbox.progressivemining;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import tictim.minerstoolbox.MinersToolboxNetwork;
import tictim.minerstoolbox.config.Cfgs;
import tictim.minerstoolbox.config.ProgressiveMiningConfig;
import tictim.minerstoolbox.config.ProgressiveMiningRule;
import tictim.minerstoolbox.contents.Contents;
import tictim.minerstoolbox.network.SyncMiningProgressMsg;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class ProgressiveMining{
	private static final ResourceLocation MINING_PROGRESSION_TRACKER_KEY = new ResourceLocation(MODID, "mining_progression");

	@SubscribeEvent
	public static void attachChunkCapability(AttachCapabilitiesEvent<LevelChunk> event){
		event.addCapability(MINING_PROGRESSION_TRACKER_KEY, new MiningProgressionTracker());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event){
		Player p = event.getPlayer();
		if(p.isCreative()) return;
		updateTracker(p.level, event.getPos(), 1);
	}

	@SubscribeEvent
	public static void onChunkWatch(ChunkWatchEvent.Watch event){
		SyncMiningProgressMsg msg = new SyncMiningProgressMsg();
		for(Map.Entry<BlockPos, Progression> e : MiningProgressionTracker.get(event.getWorld(), event.getPos()).map().entrySet()){
			msg.add(e.getKey(), e.getValue().substage);
		}
		MinersToolboxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), msg);
	}

	public static boolean fuckDrops;

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockBreak(BlockEvent.BreakEvent event){
		// majority of the code is copy-paste of ServerPlayerGameMode shits
		if(!(event.getPlayer() instanceof ServerPlayer player)||
				!(player.level instanceof ServerLevel level)||
				player.isCreative()) return;
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof GameMasterBlock&&!player.canUseGameMasterBlocks())&&
				!player.getMainHandItem().onBlockStartBreak(pos, player)&&
				!player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())){

			BlockEntity blockEntity = level.getBlockEntity(pos);
			ItemStack heldItem = player.getMainHandItem();
			ItemStack tool = heldItem.copy();

			boolean canHarvest = state.canHarvestBlock(level, pos, player); // previously player.hasCorrectToolForDrops(blockstate)
			heldItem.mineBlock(level, state, pos, player);
			if(heldItem.isEmpty()&&!tool.isEmpty())
				ForgeEventFactory.onPlayerDestroyItem(player, tool, InteractionHand.MAIN_HAND);

			if(destroyBlock(level, pos, player) instanceof DestroyResult.Success s&&s.destroyed){
				if(canHarvest){
					LootTable loot = s.getLoot();
					if(loot!=null){
						LootContext b = new LootContext.Builder(level)
								.withRandom(level.random)
								.withParameter(LootContextParams.BLOCK_STATE, state)
								.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
								.withParameter(LootContextParams.TOOL, tool)
								.withOptionalParameter(LootContextParams.THIS_ENTITY, player)
								.withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
								.withOptionalParameter(Contents.SUBSTAGE_LOOT_PARAM, s.getHarvestedSubstage())
								.create(LootContextParamSets.BLOCK);

						for(ItemStack stack : loot.getRandomItems(b))
							Block.popResource(level, pos, stack); // TODO maybe spawning item inside block is not so good idea
					}

					if(s.fullyHarvested()){
						state.spawnAfterBreak(level, pos, tool);
						try{
							fuckDrops = true;
							state.getBlock().playerDestroy(level, player, pos, state, blockEntity, tool);
							// TODO maybe need to place block here because some blocks might rewrite blockstate inside playerDestroy()
							//      for instance, turtle eggs
						}finally{
							fuckDrops = false;
						}
					}
				}
				if(s.fullyHarvested()&&event.getExpToDrop()>0)
					state.getBlock().popExperience(level, pos, event.getExpToDrop());
			}
			event.setCanceled(true);
		}
	}

	public static void updateTracker(Level level, BlockPos pos, int updateRadius){
		if(level.isClientSide||updateRadius<0) return;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		ProgressiveMiningConfig cfg = Cfgs.getProgressiveMiningConfig();

		SyncMiningProgressMsg msg = new SyncMiningProgressMsg();
		Set<ChunkPos> chunks = new HashSet<>();
		// update
		for(int x = -updateRadius; x<=updateRadius; x++){
			for(int y = -updateRadius; y<=updateRadius; y++){
				for(int z = -updateRadius; z<=updateRadius; z++){
					mpos.set(pos).offset(x, y, z);
					if(!level.isLoaded(mpos)) continue;
					MiningProgressionTracker tracker = MiningProgressionTracker.get(level, mpos);
					BlockState state = level.getBlockState(mpos);
					Progression progression = tracker.get(mpos);

					int substageToSend = -1;

					// outdated progression tracker
					if(progression!=null&&progression.block!=state.getBlock()){
						progression = null;
						tracker.remove(mpos);
						substageToSend = 0;
					}

					ProgressiveMiningRule rule = cfg.getMatchingRule(state);

					if(rule!=null){
						if(progression==null){
							// create new tracker with generated initial substage
							substageToSend = rule.generateInitialSubstage(level.random);
							tracker.set(mpos.immutable(), state.getBlock(), substageToSend, false);
						}
					}else if(progression!=null){
						tracker.remove(mpos);
						substageToSend = 0;
						// probably outdated tracker
					}
					if(substageToSend!=-1){
						msg.add(mpos.immutable(), substageToSend);
						chunks.add(new ChunkPos(mpos));
					}
				}
			}
		}
		if(!msg.map().isEmpty()&&level.getChunkSource() instanceof ServerChunkCache scc){
			Packet<?> packet = MinersToolboxNetwork.CHANNEL.toVanillaPacket(msg, NetworkDirection.PLAY_TO_CLIENT);
			chunks.stream()
					.flatMap(c -> scc.chunkMap.getPlayers(c, false).stream())
					.distinct()
					.forEach(player -> player.connection.send(packet));
		}
	}

	public static DestroyResult destroyBlock(Level level, BlockPos pos, Player player){ // TODO better sync
		MiningProgressionTracker tracker = MiningProgressionTracker.get(level, pos);
		Progression progression = tracker.get(pos);
		if(progression==null) return DestroyResult.pass();
		if(player.isCreative()){
			tracker.remove(pos);
			return DestroyResult.pass();
		}

		BlockState state = level.getBlockState(pos);
		if(!level.isClientSide&&progression.block!=state.getBlock()){
			tracker.remove(pos);
			return DestroyResult.pass();
		}
		ProgressiveMiningRule rule = Cfgs.getProgressiveMiningConfig().getMatchingRule(state);
		if(rule==null){
			tracker.remove(pos);
			return DestroyResult.pass();
		}

		if(--progression.substage>0){
			// Block#spawnDestroyParticles, the only part of Block#onDestroyedByPlayer we want
			level.levelEvent(player, 2001, pos, Block.getId(state));
			progression.save = true;
			return DestroyResult.success(true, progression, rule);
		}
		tracker.remove(pos);

		FluidState fluid = level.getFluidState(pos);
		boolean destroyed = state.onDestroyedByPlayer(level, pos, player, false, fluid);
		if(destroyed){
			state.getBlock().destroy(level, pos, state);
			if(rule.changesTo()!=null){
				BlockState newState = rule.changesTo().getOnlyMatchingBlockState();
				if(newState!=null&&!newState.isAir())
					level.setBlock(pos, newState, level.isClientSide ? 11 : 3);
			}
		}
		return DestroyResult.success(destroyed, progression, rule);
	}
}
