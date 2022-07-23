package tictim.minerstoolbox.contents.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import tictim.minerstoolbox.config.ExplosionStat;
import tictim.minerstoolbox.config.ExplosionStats;
import tictim.minerstoolbox.contents.Contents;
import tictim.minerstoolbox.contents.block.MiningExplosiveBlock;
import tictim.minerstoolbox.explosion.PropagatingExploder;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class ExplosiveEntity extends Entity{
	public static final EntityDataSerializer<AttachFace> ATTACH_FACE_SERIALIZER = new EntityDataSerializer<>(){
		@Override public void write(FriendlyByteBuf buffer, AttachFace value){
			buffer.writeEnum(value);
		}
		@Override public AttachFace read(FriendlyByteBuf buffer){
			return buffer.readEnum(AttachFace.class);
		}
		@Override public AttachFace copy(AttachFace value){
			return value;
		}
	};

	static{
		EntityDataSerializers.registerSerializer(ATTACH_FACE_SERIALIZER);
	}

	public static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(ExplosiveEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Direction> HORIZONTAL_FACING = SynchedEntityData.defineId(ExplosiveEntity.class, EntityDataSerializers.DIRECTION);
	public static final EntityDataAccessor<AttachFace> ATTACH_FACE = SynchedEntityData.defineId(ExplosiveEntity.class, ATTACH_FACE_SERIALIZER);

	private int fuse = 80;
	@Nullable private LivingEntity owner;

	public ExplosiveEntity(EntityType<?> type, Level level){
		super(type, level);
		this.setNoGravity(true);
		this.blocksBuilding = true;
	}

	public abstract ExplosionStat getExplosionStat();
	public int getFuse(){
		return fuse;
	}
	public void setFuse(int fuse){
		this.fuse = fuse;
	}

	public Direction getHorizontalFacing(){
		return this.entityData.get(HORIZONTAL_FACING);
	}
	public void setHorizontalFacing(Direction horizontalFacing){
		this.entityData.set(HORIZONTAL_FACING, Objects.requireNonNull(horizontalFacing));
	}
	public AttachFace getAttachFace(){
		return this.entityData.get(ATTACH_FACE);
	}
	public void setAttachFace(AttachFace attachFace){
		this.entityData.set(ATTACH_FACE, Objects.requireNonNull(attachFace));
	}

	@Nullable public LivingEntity getOwner(){
		return owner;
	}
	public void setOwner(@Nullable LivingEntity owner){
		this.owner = owner;
	}

	public void setAttachFaceAndFacing(BlockState state){
		if(state.hasProperty(BlockStateProperties.ATTACH_FACE)&&state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
			setHorizontalFacing(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
			setAttachFace(state.getValue(BlockStateProperties.ATTACH_FACE));
		}
	}

	@Override protected void defineSynchedData(){
		this.entityData.define(FUSE, fuse);
		this.entityData.define(HORIZONTAL_FACING, Direction.EAST);
		this.entityData.define(ATTACH_FACE, AttachFace.WALL);
	}

	@Override public void tick(){
		int i = this.getFuse()-1;
		this.setFuse(i);
		if(i<=0){
			this.discard();
			if(!this.level.isClientSide){
				new PropagatingExploder(level, blockPosition(), getExplosionStat(), this)
						.fuckingExplode();
			}
		}else{
			this.updateInWaterStateAndDoFluidPushing();
			if(this.level.isClientSide){
				MiningExplosiveBlock.SmokePosition p = MiningExplosiveBlock.getSmokePosition(getAttachFace(), getHorizontalFacing());

				this.level.addParticle(ParticleTypes.SMOKE, this.getX()-.5+p.p1().x()/16, this.getY()+p.p1().y()/16, this.getZ()-.5+p.p1().z()/16, 0, 0, 0);
				this.level.addParticle(ParticleTypes.SMOKE, this.getX()-.5+p.p2().x()/16, this.getY()+p.p2().y()/16, this.getZ()-.5+p.p2().z()/16, 0, 0, 0);
			}
		}
	}

	@Override protected void readAdditionalSaveData(CompoundTag tag){
		this.fuse = tag.getInt("fuse");
	}
	@Override protected void addAdditionalSaveData(CompoundTag tag){
		tag.putInt("fuse", this.fuse);
	}
	@Override public Packet<?> getAddEntityPacket(){
		return new ClientboundAddEntityPacket(this);
	}

	@Override public boolean ignoreExplosion(){
		return true;
	}

	public static class Crude extends ExplosiveEntity{
		public Crude(EntityType<?> type, Level level){
			super(type, level);
		}
		public Crude(Level level){
			super(Contents.CRUDE_EXPLOSIVE_ENTITY.get(), level);
		}

		@Override public ExplosionStat getExplosionStat(){
			return ExplosionStats.CRUDE;
		}
	}

	public static class Improved extends ExplosiveEntity{
		public Improved(EntityType<?> type, Level level){
			super(type, level);
		}
		public Improved(Level level){
			super(Contents.IMPROVED_EXPLOSIVE_ENTITY.get(), level);
		}

		@Override public ExplosionStat getExplosionStat(){
			return ExplosionStats.IMPROVED;
		}
	}

	public static class Enhanced extends ExplosiveEntity{
		public Enhanced(EntityType<?> type, Level level){
			super(type, level);
		}
		public Enhanced(Level level){
			super(Contents.ENHANCED_EXPLOSIVE_ENTITY.get(), level);
		}

		@Override public ExplosionStat getExplosionStat(){
			return ExplosionStats.ENHANCED;
		}
	}

	public static class Superb extends ExplosiveEntity{
		public Superb(EntityType<?> type, Level level){
			super(type, level);
		}
		public Superb(Level level){
			super(Contents.SUPERB_EXPLOSIVE_ENTITY.get(), level);
		}

		@Override public ExplosionStat getExplosionStat(){
			return ExplosionStats.SUPERB;
		}
	}

	public static class Supercalifragilisticexpialidocious extends ExplosiveEntity{
		public Supercalifragilisticexpialidocious(EntityType<?> type, Level level){
			super(type, level);
		}
		public Supercalifragilisticexpialidocious(Level level){
			super(Contents.ENHANCED_EXPLOSIVE_ENTITY.get(), level);
		}

		@Override public ExplosionStat getExplosionStat(){
			return ExplosionStats.SUPERCALIFRAGILISTICEXPIALIDOCIOUS;
		}
	}
}
