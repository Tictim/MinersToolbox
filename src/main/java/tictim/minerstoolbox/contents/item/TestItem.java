package tictim.minerstoolbox.contents.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class TestItem extends Item{
	public TestItem(Properties p){
		super(p);
	}

	@Override public InteractionResult useOn(UseOnContext ctx){
		if(!ctx.getLevel().isClientSide){
			Vec3 l = ctx.getClickLocation();
			ctx.getLevel().explode(null, l.x ,l.y,l.z, 30, Explosion.BlockInteraction.DESTROY);
		}
		return InteractionResult.PASS;
	}
}
