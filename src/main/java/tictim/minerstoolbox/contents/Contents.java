package tictim.minerstoolbox.contents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tictim.minerstoolbox.contents.block.MiningExplosiveBlock;
import tictim.minerstoolbox.contents.entity.ExplosiveEntity;
import tictim.minerstoolbox.contents.item.DetonatorItem;
import tictim.minerstoolbox.contents.item.TerrainInspectorItem;

import javax.annotation.Nonnull;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

public class Contents{
	public static final CreativeModeTab TAB = new CreativeModeTab(MODID){
		@Nonnull @Override public ItemStack makeIcon(){
			return new ItemStack(CRUDE_EXPLOSIVE_ITEM.get());
		}
	};

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

	public static final RegistryObject<Block> CRUDE_EXPLOSIVE = BLOCKS.register("crude_explosive", () -> new MiningExplosiveBlock.Crude(explosiveProperty()));
	public static final RegistryObject<Item> CRUDE_EXPLOSIVE_ITEM = ITEMS.register("crude_explosive", () -> new BlockItem(CRUDE_EXPLOSIVE.get(), p()));
	public static final RegistryObject<EntityType<ExplosiveEntity>> CRUDE_EXPLOSIVE_ENTITY = ENTITIES.register("crude_explosive", () ->
			EntityType.Builder.<ExplosiveEntity>of(ExplosiveEntity.Crude::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("crude_explosive"));

	public static final RegistryObject<Block> IMPROVED_EXPLOSIVE = BLOCKS.register("improved_explosive", () -> new MiningExplosiveBlock.Improved(explosiveProperty()));
	public static final RegistryObject<Item> IMPROVED_EXPLOSIVE_ITEM = ITEMS.register("improved_explosive", () -> new BlockItem(IMPROVED_EXPLOSIVE.get(), p()));
	public static final RegistryObject<EntityType<ExplosiveEntity>> IMPROVED_EXPLOSIVE_ENTITY = ENTITIES.register("improved_explosive", () ->
			EntityType.Builder.<ExplosiveEntity>of(ExplosiveEntity.Improved::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("improved_explosive"));

	public static final RegistryObject<Block> ENHANCED_EXPLOSIVE = BLOCKS.register("enhanced_explosive", () -> new MiningExplosiveBlock.Enhanced(explosiveProperty()));
	public static final RegistryObject<Item> ENHANCED_EXPLOSIVE_ITEM = ITEMS.register("enhanced_explosive", () -> new BlockItem(ENHANCED_EXPLOSIVE.get(), p()));
	public static final RegistryObject<EntityType<ExplosiveEntity>> ENHANCED_EXPLOSIVE_ENTITY = ENTITIES.register("enhanced_explosive", () ->
			EntityType.Builder.<ExplosiveEntity>of(ExplosiveEntity.Improved::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("enhanced_explosive"));

	public static final RegistryObject<Block> SUPERB_EXPLOSIVE = BLOCKS.register("superb_explosive", () -> new MiningExplosiveBlock.Superb(explosiveProperty()));
	public static final RegistryObject<Item> SUPERB_EXPLOSIVE_ITEM = ITEMS.register("superb_explosive", () -> new BlockItem(SUPERB_EXPLOSIVE.get(), p()));
	public static final RegistryObject<EntityType<ExplosiveEntity>> SUPERB_EXPLOSIVE_ENTITY = ENTITIES.register("superb_explosive", () ->
			EntityType.Builder.<ExplosiveEntity>of(ExplosiveEntity.Improved::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("superb_explosive"));

	public static final RegistryObject<Block> SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE = BLOCKS.register("supercalifragilisticexpialidocious_explosive", () -> new MiningExplosiveBlock.Supercalifragilisticexpialidocious(explosiveProperty()));
	public static final RegistryObject<Item> SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_ITEM = ITEMS.register("supercalifragilisticexpialidocious_explosive", () -> new BlockItem(SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE.get(), p()));
	public static final RegistryObject<EntityType<ExplosiveEntity>> SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_ENTITY = ENTITIES.register("supercalifragilisticexpialidocious_explosive", () ->
			EntityType.Builder.<ExplosiveEntity>of(ExplosiveEntity.Improved::new, MobCategory.MISC).sized(0.98F, 0.7F).clientTrackingRange(8).build("supercalifragilisticexpialidocious_explosive"));

	public static final RegistryObject<Item> DETONATOR = ITEMS.register("detonator", () -> new DetonatorItem(p().stacksTo(1)));
	public static final RegistryObject<Item> TERRAIN_INSPECTOR = ITEMS.register("terrain_inspector", () -> new TerrainInspectorItem(p().stacksTo(1)));

	public static final RegistryObject<Item> CRUDE_EXPLOSIVE_POWDER = ITEMS.register("crude_explosive_powder", () -> new Item(p()));
	public static final RegistryObject<Item> ENHANCED_EXPLOSIVE_POWDER = ITEMS.register("enhanced_explosive_powder", () -> new Item(p()));
	public static final RegistryObject<Item> SUPERB_EXPLOSIVE_POWDER = ITEMS.register("superb_explosive_powder", () -> new Item(p()));
	public static final RegistryObject<Item> SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_POWDER = ITEMS.register("supercalifragilisticexpialidocious_explosive_powder", () -> new Item(p()));

	public static final RegistryObject<SoundEvent> DETONATOR_SOUND = SOUND_EVENTS.register("detonator", () -> new SoundEvent(new ResourceLocation(MODID, "detonator")));

	public static final LootContextParam<Integer> SUBSTAGE_LOOT_PARAM = new LootContextParam<>(new ResourceLocation(MODID, "substage"));

	private static BlockBehaviour.Properties explosiveProperty(){
		return BlockBehaviour.Properties.of(Material.EXPLOSIVE).sound(SoundType.GRASS).instabreak().noCollission();
	}

	private static Item.Properties p(){
		return new Item.Properties().tab(TAB);
	}
}
