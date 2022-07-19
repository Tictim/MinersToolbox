package tictim.minerstoolbox.contents.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.minecraft.ChatFormatting.YELLOW;

public class TerrainInspectorItem extends Item{
	public TerrainInspectorItem(Properties p){
		super(p);
	}

	@Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced){
		tooltip.add(new TranslatableComponent("item.minerstoolbox.terrain_inspector.tooltip").withStyle(YELLOW));
	}
}
