package datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import tictim.minerstoolbox.contents.Contents;

import java.util.function.Consumer;

import static net.minecraft.data.recipes.ShapedRecipeBuilder.shaped;
import static net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless;
import static tictim.minerstoolbox.MinersToolboxMod.MODID;

public class RecipeGen extends RecipeProvider{
	public RecipeGen(DataGenerator gen){
		super(gen);
	}

	@Override protected void buildCraftingRecipes(Consumer<FinishedRecipe> c){
		shapeless(Contents.CRUDE_EXPLOSIVE_ITEM.get())
				.requires(Items.PAPER)
				.requires(Contents.CRUDE_EXPLOSIVE_POWDER.get())
				.requires(Tags.Items.SAND)
				.unlockedBy("a", has(Items.PAPER))
				.save(c);
		shapeless(Contents.IMPROVED_EXPLOSIVE_ITEM.get())
				.requires(Items.PAPER)
				.requires(Items.GUNPOWDER)
				.requires(Tags.Items.SAND)
				.unlockedBy("a", has(Items.PAPER))
				.save(c);
		shapeless(Contents.ENHANCED_EXPLOSIVE_ITEM.get())
				.requires(Items.PAPER)
				.requires(Contents.ENHANCED_EXPLOSIVE_POWDER.get())
				.requires(Ingredient.of(Items.SOUL_SAND, Items.SOUL_SOIL))
				.unlockedBy("a", has(Items.PAPER))
				.save(c);
		shapeless(Contents.SUPERB_EXPLOSIVE_ITEM.get())
				.requires(Items.PAPER)
				.requires(Contents.SUPERB_EXPLOSIVE_POWDER.get())
				.requires(Ingredient.of(Items.SOUL_SAND, Items.SOUL_SOIL))
				.unlockedBy("a", has(Items.PAPER))
				.save(c);
		shapeless(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_ITEM.get())
				.requires(Items.PAPER)
				.requires(Contents.SUPERCALIFRAGILISTICEXPIALIDOCIOUS_EXPLOSIVE_POWDER.get())
				.requires(Items.POISONOUS_POTATO)
				.unlockedBy("a", has(Items.PAPER))
				.save(c);

		shapeless(Contents.CRUDE_EXPLOSIVE_POWDER.get(), 4)
				.requires(ItemTags.COALS)
				.requires(Items.SUGAR)
				.unlockedBy("a", has(ItemTags.COALS))
				.save(c);
		shapeless(Contents.CRUDE_EXPLOSIVE_POWDER.get(), 32)
				.requires(Items.GUNPOWDER)
				.requires(Items.SUGAR)
				.unlockedBy("a", has(Items.GUNPOWDER))
				.save(c, new ResourceLocation(MODID, "crude_explosive_powder_from_gunpowder"));
		shapeless(Contents.ENHANCED_EXPLOSIVE_POWDER.get(), 6)
				.requires(Items.BLAZE_POWDER)
				.requires(Items.REDSTONE)
				.requires(Items.GLOWSTONE_DUST)
				.unlockedBy("a", has(Items.BLAZE_POWDER))
				.save(c);
		shapeless(Contents.SUPERB_EXPLOSIVE_POWDER.get(), 16)
				.requires(Items.NETHERITE_SCRAP)
				.requires(Items.GHAST_TEAR)
				.unlockedBy("a", has(Items.NETHERITE_SCRAP))
				.save(c);

		shaped(Contents.TERRAIN_INSPECTOR.get())
				.pattern(" 1 ")
				.pattern("121")
				.pattern("31 ")
				.define('1', Items.FLINT)
				.define('2', Tags.Items.GLASS_PANES)
				.define('3', Items.STICK)
				.unlockedBy("a", has(Items.FLINT))
				.save(c);
	}
}
