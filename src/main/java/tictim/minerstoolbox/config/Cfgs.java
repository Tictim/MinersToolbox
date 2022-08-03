package tictim.minerstoolbox.config;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import tictim.minerstoolbox.MinersToolboxMod;
import tictim.minerstoolbox.MinersToolboxNetwork;
import tictim.minerstoolbox.network.SyncProgressiveMiningConfigMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class Cfgs{
	private static final ProgressiveMiningConfig local = new ProgressiveMiningConfig();
	private static ProgressiveMiningConfig remote = new ProgressiveMiningConfig();

	private static final String PROGRESSIVE_MINING_CONFIG_README = "Visit https://github.com/Tictim/MinersToolbox for more info.\n"; // TODO

	public static ProgressiveMiningConfig getProgressiveMiningConfig(){
		return FMLEnvironment.dist==Dist.DEDICATED_SERVER||ServerLifecycleHooks.getCurrentServer()!=null ? local : remote;
	}

	private static final Path defaultConfigPath = FMLPaths.GAMEDIR.get().resolve("config/"+MODID+"/progressive_mining");

	public static int loadProgressiveMiningConfig(){
		local.clear();
		if(Files.exists(defaultConfigPath)){
			try{
				Files.walk(defaultConfigPath).forEach(path -> {
					if(Files.isDirectory(path)) return;
					String ext = FileUtils.fileExtension(path.getFileName());
					if(!"json".equalsIgnoreCase(ext)) return;

					StringBuilder stb = new StringBuilder();
					boolean first = true;
					for(int i = path.getNameCount()-1, j = defaultConfigPath.getNameCount(); i>=j; i--){
						if(first){
							stb.append("/");
							first = false;
						}
						stb.append(path.getName(i));
					}
					String name = stb.toString();
					try(var r = Files.newBufferedReader(path)){
						local.load(name, ProgressiveMiningConfig.LENIENT_GSON.fromJson(r, JsonObject.class));
					}catch(IOException e){
						MinersToolboxMod.LOGGER.warn("Cannot read file \"{}\": ", name, e);
					}
				});
			}catch(IOException e){
				MinersToolboxMod.LOGGER.warn("Cannot load progressive mining config: ", e);
			}
		}else{
			try{
				Files.createDirectories(defaultConfigPath);
				Files.writeString(defaultConfigPath.resolve("readme.txt"), PROGRESSIVE_MINING_CONFIG_README, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			}catch(IOException e){
				MinersToolboxMod.LOGGER.warn("Cannot generate progressive mining config folder: ", e);
			}
		}
		local.updateAndValidate();
		local.printAllInvalidReasons(MinersToolboxMod.LOGGER);
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if(server!=null){
			MinersToolboxNetwork.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new SyncProgressiveMiningConfigMsg(local));
		}
		return local.getValidRules().size();
	}

	@SubscribeEvent
	public static void serverAboutToStart(ServerAboutToStartEvent event){
		loadProgressiveMiningConfig();
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
		if(event.getPlayer() instanceof ServerPlayer sp)
			MinersToolboxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncProgressiveMiningConfigMsg(local));
	}

	public static void setRemoteProgressiveMiningConfig(ProgressiveMiningConfig config){
		config.updateAndValidate();
		remote = config;
	}
}
