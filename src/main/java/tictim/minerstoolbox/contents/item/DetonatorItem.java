package tictim.minerstoolbox.contents.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tictim.minerstoolbox.contents.Contents;
import tictim.minerstoolbox.contents.block.MiningExplosiveBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DetonatorItem extends Item{
	public static final Capability<Data> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

	public DetonatorItem(Properties p){
		super(p);
	}

	@Override public InteractionResult useOn(UseOnContext ctx){
		BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
		if(isExplosive(state)){
			if(!ctx.getLevel().isClientSide){
				Data data = ctx.getItemInHand().getCapability(CAPABILITY).orElse(null);
				if(data!=null){
					data.add(ctx.getLevel(), ctx.getClickedPos());
					Player player = ctx.getPlayer();
					if(player!=null) player.displayClientMessage(new TranslatableComponent("item.minerstoolbox.detonator.msg.marked",
							new TextComponent(""+data.posList.size()).withStyle(ChatFormatting.YELLOW)), true);
				}
			}
			return InteractionResult.SUCCESS;
		}else return InteractionResult.PASS;
	}

	public static boolean isExplosive(BlockState state){
		Block block = state.getBlock();
		return block instanceof TntBlock||block instanceof MiningExplosiveBlock;
	}

	@Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
		if(!player.isCrouching()) return InteractionResultHolder.pass(player.getItemInHand(hand));
		if(!level.isClientSide){
			ItemStack stack = player.getItemInHand(hand);
			Data data = stack.getCapability(CAPABILITY).orElse(null);
			if(data!=null){
				ResourceLocation dimension = level.dimension().location();
				for(DimPos dimPos : data.posList){
					if(!dimension.equals(dimPos.dim)||!level.isLoaded(dimPos.pos)) continue;
					BlockState state = level.getBlockState(dimPos.pos);
					state.onCaughtFire(level, dimPos.pos, null, player);
					level.setBlock(dimPos.pos, Blocks.AIR.defaultBlockState(), 3);
				}
				data.clear();
			}
			player.getCooldowns().addCooldown(this, 7);
		}
		level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), Contents.DETONATOR_SOUND.get(), SoundSource.PLAYERS, 1.5f, 1);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Nullable @Override public CompoundTag getShareTag(ItemStack stack){
		CompoundTag tag = stack.getTag();
		Data data = stack.getCapability(CAPABILITY).orElse(null);
		if(data!=null&&!data.isEmpty()){
			tag = tag==null ? new CompoundTag() : tag.copy();
			tag.put("data", data.serializeNBT());
		}
		return tag;
	}
	@Override public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt){
		if(nbt!=null&&nbt.contains("data", Tag.TAG_LIST)){
			Data data = stack.getCapability(CAPABILITY).orElse(null);
			if(data!=null) data.deserializeNBT(nbt.getList("data", Tag.TAG_COMPOUND));
		}
	}

	@Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag isAdvanced){
		list.add(new TranslatableComponent("item.minerstoolbox.detonator.tooltip.1").withStyle(ChatFormatting.YELLOW));
		list.add(new TranslatableComponent("item.minerstoolbox.detonator.tooltip.2").withStyle(ChatFormatting.YELLOW));
	}

	@Nullable @Override public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt){
		return new Data();
	}

	public static final class Data implements INBTSerializable<ListTag>, @Nullable ICapabilityProvider{
		private static final int SIZE = 100;

		private final Set<DimPos> posSet = new HashSet<>();
		private final List<DimPos> posList = new ArrayList<>();

		public void add(Level level, BlockPos pos){
			add(new DimPos(level.dimension().location(), pos));
		}
		public void add(DimPos dimPos){
			if(posSet.add(dimPos)){
				if(posSet.size()>SIZE){
					DimPos dimPos2 = posList.remove(0);
					posSet.remove(dimPos2);
				}
				posList.add(dimPos);
			}
		}

		public void clear(){
			this.posSet.clear();
			this.posList.clear();
		}

		public boolean isEmpty(){
			return posList.isEmpty();
		}

		@Override public ListTag serializeNBT(){
			ListTag list = new ListTag();
			for(DimPos dimPos : posList){
				CompoundTag tag = new CompoundTag();
				if(!"minecraft".equals(dimPos.dim.getNamespace()))
					tag.putString("dimNamespace", dimPos.dim.getNamespace());
				tag.putString("dimPath", dimPos.dim.getPath());
				tag.put("pos", NbtUtils.writeBlockPos(dimPos.pos));
				list.add(tag);
			}
			return list;
		}

		@Override public void deserializeNBT(ListTag list){
			clear();
			for(int i = 0; i<list.size(); i++){
				CompoundTag tag = list.getCompound(i);
				DimPos dimPos = new DimPos(
						new ResourceLocation(
								tag.contains("dimNamespace", Tag.TAG_STRING) ? tag.getString("dimNamespace") : "minecraft",
								tag.getString("dimPath")),
						NbtUtils.readBlockPos(tag.getCompound("pos")));
				add(dimPos);
			}
		}

		private LazyOptional<Data> self;

		@NotNull @Override public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side){
			if(cap==CAPABILITY){
				if(self==null) self = LazyOptional.of(() -> this);
				return self.cast();
			}else return LazyOptional.empty();
		}
	}

	public record DimPos(ResourceLocation dim, BlockPos pos){}
}
