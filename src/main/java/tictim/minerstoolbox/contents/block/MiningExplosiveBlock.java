package tictim.minerstoolbox.contents.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tictim.minerstoolbox.MinersToolboxMod;
import tictim.minerstoolbox.config.ExplosionStat;
import tictim.minerstoolbox.config.ExplosionStats;
import tictim.minerstoolbox.contents.entity.ExplosiveEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static net.minecraft.ChatFormatting.*;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

// TODO how do I swap models on april fools? I have no goddamn idea
@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
public abstract class MiningExplosiveBlock extends FaceAttachedHorizontalDirectionalBlock{
	// horizontal direction (4) * attach face (3) * april fools shit (2)
	private static final VoxelShape[] shapes = new VoxelShape[24];
	private static final SmokePosition[] points = new SmokePosition[12];

	public record SmokePosition(FuckingPoint p1, FuckingPoint p2){}

	public record FuckingPoint(double x, double y, double z){
		public FuckingPoint rotateX(Rotation rotation){
			return switch(rotation){
				case NONE -> this;
				case CLOCKWISE_90 -> new FuckingPoint(x, z, 16-y);
				case CLOCKWISE_180 -> new FuckingPoint(x, 16-y, 16-z);
				case COUNTERCLOCKWISE_90 -> new FuckingPoint(x, 16-z, y);
			};
		}
		public FuckingPoint rotateY(Rotation rotation){
			return switch(rotation){
				case NONE -> this;
				case CLOCKWISE_90 -> new FuckingPoint(16-z, y, x);
				case CLOCKWISE_180 -> new FuckingPoint(16-x, y, 16-z);
				case COUNTERCLOCKWISE_90 -> new FuckingPoint(z, y, 16-x);
			};
		}
	}

	public record FuckingBox(FuckingPoint p1, FuckingPoint p2){
		public FuckingBox(FuckingPoint p1, FuckingPoint p2){
			this.p1 = new FuckingPoint(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
			this.p2 = new FuckingPoint(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
		}
		public FuckingBox(double x1, double y1, double z1, double x2, double y2, double z2){
			this(new FuckingPoint(x1, y1, z1), new FuckingPoint(x2, y2, z2));
		}

		public FuckingBox rotateX(Rotation rotation){
			return new FuckingBox(p1.rotateX(rotation), p2.rotateX(rotation));
		}
		public FuckingBox rotateY(Rotation rotation){
			return new FuckingBox(p1.rotateY(rotation), p2.rotateY(rotation));
		}
		public VoxelShape toShape(){
			return box(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
		}
	}

	static{
		FuckingBox aprilBox = new FuckingBox(5, 0, 2, 11, 1, 14);
		FuckingBox box = new FuckingBox(5, 0, 4, 11, 3, 14);
		for(AttachFace attachFace : AttachFace.values()){
			addShape(attachFace, Direction.SOUTH, box, aprilBox);
			addShape(attachFace, Direction.WEST, box, aprilBox);
			addShape(attachFace, Direction.NORTH, box, aprilBox);
			addShape(attachFace, Direction.EAST, box, aprilBox);
		}
	}

	private static void addShape(AttachFace attachFace, Direction direction, FuckingBox box, FuckingBox aprilBox){
		int i = attachFace.ordinal()*4+direction.get2DDataValue();
		shapes[i] = box.rotateX(getRotX(attachFace)).rotateY(getRotY(attachFace, direction)).toShape();
		shapes[12+i] = aprilBox.rotateX(getRotX(attachFace)).rotateY(getRotY(attachFace, direction)).toShape();
		points[i] = new SmokePosition(
				new FuckingPoint(6.5, 1.5, 3).rotateX(getRotX(attachFace)).rotateY(getRotY(attachFace, direction)),
				new FuckingPoint(9.5, 1.3, 3.5).rotateX(getRotX(attachFace)).rotateY(getRotY(attachFace, direction)));
	}

	private static Rotation getRotX(AttachFace attachFace){
		return attachFace==AttachFace.FLOOR ? Rotation.NONE : attachFace==AttachFace.WALL ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_180;
	}
	private static Rotation getRotY(AttachFace attachFace, Direction direction){
		int rot = (int)(attachFace==AttachFace.CEILING||attachFace==AttachFace.WALL ? direction : direction.getOpposite()).toYRot();
		return rot==0 ? Rotation.NONE : rot==90 ? Rotation.CLOCKWISE_90 : rot==180 ? Rotation.CLOCKWISE_180 : Rotation.COUNTERCLOCKWISE_90;
	}

	public static SmokePosition getSmokePosition(AttachFace attachFace, Direction direction){
		return points[attachFace.ordinal()*4+direction.get2DDataValue()];
	}

	public MiningExplosiveBlock(Properties p){
		super(p);
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
		BlockState stateForPlacement = super.getStateForPlacement(ctx);
		if(stateForPlacement==null) return null;
		FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
		return stateForPlacement.setValue(WATERLOGGED, fluid.getType()==Fluids.WATER);
	}
	@Override public void onCaughtFire(BlockState state, Level world, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter){
		explode(world, pos, igniter, 80);
	}

	@Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving){
		if(!oldState.is(state.getBlock())&&level.hasNeighborSignal(pos)){
			onCaughtFire(state, level, pos, null, null);
			level.removeBlock(pos, false);
		}
	}

	@Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving){
		if(level.hasNeighborSignal(pos)){
			onCaughtFire(state, level, pos, null, null);
			level.removeBlock(pos, false);
		}
	}

	@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult pHit){
		ItemStack stack = player.getItemInHand(hand);
		if(!stack.is(Items.FLINT_AND_STEEL)&&!stack.is(Items.FIRE_CHARGE)){
			return super.use(state, level, pos, player, hand, pHit);
		}else{
			onCaughtFire(state, level, pos, pHit.getDirection(), player);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
			Item item = stack.getItem();
			if(!player.isCreative()){
				if(stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				else stack.shrink(1);
			}

			player.awardStat(Stats.ITEM_USED.get(item));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
	}

	@Override public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile){
		if(!level.isClientSide){
			BlockPos blockpos = hit.getBlockPos();
			Entity entity = projectile.getOwner();
			if(projectile.isOnFire()&&projectile.mayInteract(level, blockpos)){
				onCaughtFire(state, level, blockpos, null, entity instanceof LivingEntity ? (LivingEntity)entity : null);
				level.removeBlock(blockpos, false);
			}
		}
	}

	@Override public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion){
		return false;
	}

	@Override public boolean dropFromExplosion(Explosion explosion){
		return false;
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b){
		b.add(FACING, FACE, WATERLOGGED);
	}

	@Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx){
		AttachFace attachFace = state.getValue(FACE);
		Direction direction = state.getValue(FACING);
		return shapes[(MinersToolboxMod.isAprilFools() ? 12 : 0)+attachFace.ordinal()*4+direction.get2DDataValue()];
	}

	@Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag){
		ExplosionStat stat = getExplosionStat();
		tooltip.add(new TranslatableComponent("block.minerstoolbox.explosive.tooltip.max_resistance",
				new TextComponent(""+stat.maxResistance()).withStyle(YELLOW)).withStyle(DARK_AQUA));
		tooltip.add(new TranslatableComponent("block.minerstoolbox.explosive.tooltip.force",
				new TextComponent(""+stat.force()).withStyle(YELLOW)).withStyle(DARK_AQUA));
		tooltip.add(new TranslatableComponent("block.minerstoolbox.explosive.tooltip.explosion_radius",
				new TextComponent(""+stat.explosionRadius()).withStyle(YELLOW)).withStyle(DARK_AQUA));
		if(stat.destroyDrop())
			tooltip.add(new TranslatableComponent("block.minerstoolbox.explosive.tooltip.destroy_drop").withStyle(RED));
	}

	@Override public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos){
		if(state.getValue(WATERLOGGED)){
			level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return getConnectedDirection(state).getOpposite()==facing&&!state.canSurvive(level, currentPos) ?
				Blocks.AIR.defaultBlockState() : state;
	}

	@Override public FluidState getFluidState(BlockState pState){
		return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
	}

	private void explode(Level level, BlockPos pos, @Nullable LivingEntity igniter, int fuse){
		if(level.isClientSide) return;
		BlockState state = level.getBlockState(pos);
		ExplosiveEntity explosiveEntity = createExplosiveEntity(level);
		explosiveEntity.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+.5);
		explosiveEntity.setAttachFaceAndFacing(state);
		explosiveEntity.setFuse(fuse);
		explosiveEntity.setOwner(igniter);
		level.addFreshEntity(explosiveEntity);
		level.playSound(null, pos.getX()+.5, pos.getY(), pos.getZ()+.5, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
	}

	protected abstract ExplosiveEntity createExplosiveEntity(Level level);
	protected abstract ExplosionStat getExplosionStat();

	public static class Crude extends MiningExplosiveBlock{
		public Crude(Properties p){
			super(p);
		}

		@Override protected ExplosiveEntity createExplosiveEntity(Level level){
			return new ExplosiveEntity.Crude(level);
		}
		@Override protected ExplosionStat getExplosionStat(){
			return ExplosionStats.CRUDE;
		}
	}

	public static class Improved extends MiningExplosiveBlock{
		public Improved(Properties p){
			super(p);
		}

		@Override protected ExplosiveEntity createExplosiveEntity(Level level){
			return new ExplosiveEntity.Improved(level);
		}
		@Override protected ExplosionStat getExplosionStat(){
			return ExplosionStats.IMPROVED;
		}
	}

	public static class Enhanced extends MiningExplosiveBlock{
		public Enhanced(Properties p){
			super(p);
		}

		@Override protected ExplosiveEntity createExplosiveEntity(Level level){
			return new ExplosiveEntity.Enhanced(level);
		}
		@Override protected ExplosionStat getExplosionStat(){
			return ExplosionStats.ENHANCED;
		}
	}

	public static class Superb extends MiningExplosiveBlock{
		public Superb(Properties p){
			super(p);
		}

		@Override protected ExplosiveEntity createExplosiveEntity(Level level){
			return new ExplosiveEntity.Superb(level);
		}
		@Override protected ExplosionStat getExplosionStat(){
			return ExplosionStats.SUPERB;
		}
	}

	public static class Supercalifragilisticexpialidocious extends MiningExplosiveBlock{
		public Supercalifragilisticexpialidocious(Properties p){
			super(p);
		}

		@Override protected ExplosiveEntity createExplosiveEntity(Level level){
			return new ExplosiveEntity.Supercalifragilisticexpialidocious(level);
		}
		@Override protected ExplosionStat getExplosionStat(){
			return ExplosionStats.SUPERCALIFRAGILISTICEXPIALIDOCIOUS;
		}
	}
}
