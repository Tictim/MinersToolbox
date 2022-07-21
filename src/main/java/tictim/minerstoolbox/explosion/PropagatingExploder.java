package tictim.minerstoolbox.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import tictim.minerstoolbox.MinersToolboxNetwork;
import tictim.minerstoolbox.config.ExplosionStat;
import tictim.minerstoolbox.contents.entity.ExplosiveEntity;
import tictim.minerstoolbox.network.ExplosionMsg;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;

public final class PropagatingExploder{
	private static final boolean TEST = false;

	private static final double SQRT_2 = Math.sqrt(2)-1;
	private static final double SQRT_3 = Math.sqrt(3)-1;

	private static final byte ALL_DIRECTION = 0;
	private static final byte POSITIVE_X = 1<<1;
	private static final byte NEGATIVE_X = 1<<2;
	private static final byte POSITIVE_Y = 1<<3;
	private static final byte NEGATIVE_Y = 1<<4;
	private static final byte POSITIVE_Z = 1<<5;
	private static final byte NEGATIVE_Z = 1<<6;

	private static final byte X = POSITIVE_X|NEGATIVE_X, Y = POSITIVE_Y|NEGATIVE_Y, Z = POSITIVE_Z|NEGATIVE_Z;

	private final Level level;
	private final BlockPos origin;
	private final float maxResistance;
	private final float originForce;
	private final float radiusSq;

	@Nullable private final Entity sourceEntity;
	private final Explosion explosion;

	private final ArrayDeque<Cell> cells = new ArrayDeque<>();

	public PropagatingExploder(Explosion explosion){
		this.level = explosion.level;
		this.origin = new BlockPos(explosion.getPosition());
		this.maxResistance = explosion.radius;
		this.originForce = explosion.radius;
		this.radiusSq = explosion.radius*explosion.radius-.05f;
		this.sourceEntity = explosion.getExploder();
		this.explosion = explosion;
	}
	public PropagatingExploder(Level level, BlockPos origin, ExplosionStat stat, @Nullable Entity source){
		this(level, origin, stat, source, null, null);
	}
	public PropagatingExploder(Level level, BlockPos origin, ExplosionStat stat, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator){
		this.level = level;
		this.origin = origin;
		this.maxResistance = stat.maxResistance();
		this.originForce = stat.force();
		this.radiusSq = stat.explosionRadius()*stat.explosionRadius()-.05f;
		this.sourceEntity = source;
		this.explosion = new Explosion(level, source instanceof ExplosiveEntity ex ? ex.getOwner() : source, damageSource, damageCalculator,
				origin.getX()+.5, origin.getY()+.5, origin.getZ()+.5,
				stat.explosionRadius(), false, stat.destroyDrop() ? BlockInteraction.DESTROY : BlockInteraction.BREAK);
	}

	public void fuckingExplode(){
		if(level.isClientSide) return;
		cells.add(new Cell(origin, originForce, (int)explosion.radius, ALL_DIRECTION));

		ServerLevel serverLevel = this.level instanceof ServerLevel ? (ServerLevel)this.level : null;
		ArrayList<BlockPos> toBlow = serverLevel!=null ? new ArrayList<>() : null;

		while(!cells.isEmpty()){
			Cell cell = cells.removeFirst();
			if(level.isInWorldBounds(cell.pos)){
				BlockState state = level.getBlockState(cell.pos);
				FluidState fluid = level.getFluidState(cell.pos);

				if((cell.pos.getY()==level.getMinBuildHeight()||cell.pos.getY()==level.getMaxBuildHeight())&&
						state.getDestroySpeed(level, cell.pos)<0)
					continue; // prevent breaking world borders

				Optional<Float> optionalRes = explosion.damageCalculator.getBlockExplosionResistance(explosion, level, cell.pos, state, fluid);
				float force = cell.force;
				if(optionalRes.isPresent()){
					float res = optionalRes.get();
					if(res>maxResistance) force = 0;
					else force -= (res+.3f)*.3f;
				}
				if(force>0){
					boolean shouldExplode = toBlow!=null&&(TEST||!state.isAir())&&explosion.damageCalculator.shouldBlockExplode(explosion, level, cell.pos, state, force);
					if(shouldExplode) toBlow.add(cell.pos);
					if(cell.rangeLeft>0&&(shouldExplode||state.isAir())) x(cell, force);
				}
			}
		}

		MinersToolboxNetwork.CHANNEL.send(
				PacketDistributor.NEAR.with(TargetPoint.p(origin.getX()+.5, origin.getY()+.5, origin.getZ()+.5, 4096, level.dimension())),
				new ExplosionMsg(origin, explosion.radius, explosion.blockInteraction));

		float entityHitRadius = Math.min(explosion.radius*2, explosion.radius+4); // I believe radius times two is too big
		int x1 = Mth.floor(origin.getX()-0.5-entityHitRadius);
		int x2 = Mth.floor(origin.getX()+1.5+entityHitRadius);
		int y1 = Mth.floor(origin.getY()-0.5-entityHitRadius);
		int y2 = Mth.floor(origin.getY()+1.5+entityHitRadius);
		int z1 = Mth.floor(origin.getZ()-0.5-entityHitRadius);
		int z2 = Mth.floor(origin.getZ()+1.5+entityHitRadius);
		Vec3 originVec3 = this.explosion.getPosition();

		for(Entity entity : this.level.getEntities(sourceEntity, new AABB(x1, y1, z1, x2, y2, z2),
				e -> e.isAlive()&&!e.isSpectator()&&!e.ignoreExplosion()&&
						(!(e instanceof Player player)||(!player.isCreative()||!player.getAbilities().flying))&&
						(explosion.blockInteraction==BlockInteraction.DESTROY||!(e instanceof ItemEntity)))){
			double distanceProportion = Math.sqrt(entity.distanceToSqr(originVec3))/entityHitRadius;
			if(distanceProportion<=1){
				double x = entity.getX()-originVec3.x;
				double y = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY())-originVec3.y;
				double z = entity.getZ()-originVec3.z;
				double dist = Math.sqrt(x*x+y*y+z*z);
				if(dist!=0){
					x /= dist;
					y /= dist;
					z /= dist;
					double seenPercent = Explosion.getSeenPercent(originVec3, entity);
					double what = (1-distanceProportion)*seenPercent;
					entity.hurt(explosion.getDamageSource(), (int)((what*what+what)/2.0*7.0*entityHitRadius+1.0));
					double idk = entity instanceof LivingEntity livingEntity ?
							ProtectionEnchantment.getExplosionKnockbackAfterDampener(livingEntity, what) :
							what;

					entity.setDeltaMovement(entity.getDeltaMovement().add(x*idk, y*idk, z*idk));
				}
			}
		}

		if(toBlow!=null){
			DropAccumulator accumulator = new DropAccumulator();
			for(BlockPos pos : toBlow){
				BlockState state = this.level.getBlockState(pos);
				if(TEST){
					level.setBlock(pos, origin.equals(pos) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.GLASS.defaultBlockState(), 3);
				}else{
					if(state.canDropFromExplosion(this.level, pos, explosion)){
						LootContext.Builder b = new LootContext.Builder(serverLevel)
								.withRandom(this.level.random)
								.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
								.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
								.withOptionalParameter(LootContextParams.BLOCK_ENTITY, state.hasBlockEntity() ? this.level.getBlockEntity(pos) : null)
								.withOptionalParameter(LootContextParams.THIS_ENTITY, explosion.getExploder());
						if(explosion.blockInteraction==BlockInteraction.DESTROY)
							b.withParameter(LootContextParams.EXPLOSION_RADIUS, originForce);

						state.getDrops(b).forEach(s -> accumulator.accumulate(pos, s));
					}
					state.onBlockExploded(this.level, pos, this.explosion);
				}
			}
			accumulator.spawn(this.level);
		}
	}

	private void x(Cell cell, float force){
		if(cell.directionFlag==ALL_DIRECTION){
			y(cell, force, 1);
			y(cell, force, -1);
		}else if((cell.directionFlag&X)!=0){
			if((cell.directionFlag&POSITIVE_X)!=0) y(cell, force, 1);
			if((cell.directionFlag&NEGATIVE_X)!=0) y(cell, force, -1);
		}
		y(cell, force, 0);
	}
	private void y(Cell cell, float force, int xDir){
		if(cell.directionFlag==ALL_DIRECTION){
			z(cell, force, xDir, 1);
			z(cell, force, xDir, -1);
		}else if((cell.directionFlag&Y)!=0){
			if((cell.directionFlag&POSITIVE_Y)!=0) z(cell, force, xDir, 1);
			if((cell.directionFlag&NEGATIVE_Y)!=0) z(cell, force, xDir, -1);
		}
		z(cell, force, xDir, 0);
	}
	private void z(Cell cell, float force, int xDir, int yDir){
		if(cell.directionFlag==ALL_DIRECTION){
			addCell(cell, force, xDir, yDir, 1);
			addCell(cell, force, xDir, yDir, -1);
		}else if((cell.directionFlag&Z)!=0){
			if((cell.directionFlag&POSITIVE_Z)!=0) addCell(cell, force, xDir, yDir, 1);
			if((cell.directionFlag&NEGATIVE_Z)!=0) addCell(cell, force, xDir, yDir, -1);
		}
		addCell(cell, force, xDir, yDir, 0);
	}
	private void addCell(Cell cell, float force, int xDir, int yDir, int zDir){
		int direction = 0;
		if(xDir!=0) direction++;
		if(yDir!=0) direction++;
		if(zDir!=0) direction++;
		if(direction==0) return;
		boolean costsAdditionalRange = switch(direction){
			case 2 -> level.random.nextDouble()+level.random.nextDouble()<SQRT_2*2;
			case 3 -> level.random.nextDouble()+level.random.nextDouble()<SQRT_3*2;
			default -> false;
		};
		if(cell.rangeLeft<(costsAdditionalRange ? 2 : 1)) return;
		BlockPos offset = cell.pos.offset(xDir, yDir, zDir);
		if(origin.distSqr(offset)>radiusSq) return;
		cells.add(new Cell(offset, force,
				cell.rangeLeft-(costsAdditionalRange ? 2 : 1),
				(byte)((xDir==1 ? POSITIVE_X : xDir==-1 ? NEGATIVE_X : 0)|
						(yDir==1 ? POSITIVE_Y : yDir==-1 ? NEGATIVE_Y : 0)|
						(zDir==1 ? POSITIVE_Z : zDir==-1 ? NEGATIVE_Z : 0))));
	}

	public record Cell(
			BlockPos pos,
			float force,
			int rangeLeft,
			byte directionFlag
	){}
}
