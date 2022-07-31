package tictim.minerstoolbox;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tictim.minerstoolbox.config.Cfgs;
import tictim.minerstoolbox.config.ExplosionStat;
import tictim.minerstoolbox.explosion.PropagatingExploder;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class MinersToolboxCommand{
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event){
		event.getDispatcher().register(literal(MODID)
				.then(literal("explosion").requires(c -> c.hasPermission(2)).then(explosionCommand(false)))
				.then(literal("testExplosion").requires(c -> c.hasPermission(2)).then(explosionCommand(true)))
				.then(literal("reloadProgressiveMining").requires(c -> c.hasPermission(2)).executes(context -> {
					int i = Cfgs.loadProgressiveMiningConfig();
					context.getSource().sendSuccess(new TextComponent("Read "+i+" rules"), true);
					return i;
				}))
		);
	}

	private static ArgumentBuilder<CommandSourceStack, ?> explosionCommand(boolean test){
		return argument("position", BlockPosArgument.blockPos())
				.then(argument("maxResistance", FloatArgumentType.floatArg(0))
						.then(argument("force", FloatArgumentType.floatArg(0))
								.then(argument("radius", IntegerArgumentType.integer(1))
										.executes(ctx -> explode(ctx, true, test))
										.then(argument("destroyDrop", BoolArgumentType.bool())
												.executes(ctx -> explode(ctx, BoolArgumentType.getBool(ctx, "destroyDrop"), test))
										)
								)
						)
				);
	}

	private static int explode(CommandContext<CommandSourceStack> ctx, boolean destroyDrop, boolean test) throws CommandSyntaxException{
		BlockPos position = BlockPosArgument.getLoadedBlockPos(ctx, "position");
		float maxResistance = FloatArgumentType.getFloat(ctx, "maxResistance");
		float force = FloatArgumentType.getFloat(ctx, "force");
		int radius = IntegerArgumentType.getInteger(ctx, "radius");

		CommandSourceStack src = ctx.getSource();
		ServerLevel level = src.getLevel();
		PropagatingExploder exploder = new PropagatingExploder(level, position,
				new ExplosionStat.Record(maxResistance, force, radius, destroyDrop),
				ctx.getSource().getEntity());
		exploder.setTest(test);

		exploder.fuckingExplode();
		return SINGLE_SUCCESS;
	}
}
