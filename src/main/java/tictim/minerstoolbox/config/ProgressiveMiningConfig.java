package tictim.minerstoolbox.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.Deserializers;
import tictim.minerstoolbox.MinersToolboxMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ProgressiveMiningConfig{
	public static final Gson LENIENT_GSON = Deserializers.createLootTableSerializer()
			.setLenient()
			.create();
	public static final Gson PRETTY_GSON = Deserializers.createLootTableSerializer()
			.setPrettyPrinting()
			.create();

	private final List<ProgressiveMiningRule> configs;
	private List<ProgressiveMiningRule> validConfigs = List.of();

	public ProgressiveMiningConfig(){
		this(new ArrayList<>());
	}
	public ProgressiveMiningConfig(List<ProgressiveMiningRule> configs){
		this.configs = configs;
	}

	public List<ProgressiveMiningRule> getConfigs(){
		return configs;
	}

	@Nullable public ProgressiveMiningRule getMatchingRule(BlockState state){
		for(ProgressiveMiningRule r : configs){
			if(r.block().testState(state)) return r;
		}
		return null;
	}

	public void load(String fileName, JsonObject object){
		try{
			JsonArray rules = GsonHelper.getAsJsonArray(object, "rules");
			for(int i = 0; i<rules.size(); i++){
				try{
					configs.add(ProgressiveMiningRule.read(rules.get(i).getAsJsonObject()));
				}catch(JsonParseException ex){
					MinersToolboxMod.LOGGER.debug("Invalid data in progressive mining config file \"{}\", entry {} ", fileName, i+1, ex);
				}
			}
		}catch(JsonParseException ex){
			MinersToolboxMod.LOGGER.debug("Invalid data in progressive mining config file \"{}\" ", fileName, ex);
		}
	}

	public void validate(){

	}

	public void clear(){
		this.configs.clear();
	}

	public void write(FriendlyByteBuf buf){
		buf.writeCollection(this.configs, (b, c) -> c.write(b));
	}
	public static ProgressiveMiningConfig read(FriendlyByteBuf buf){
		return new ProgressiveMiningConfig(buf.readList(ProgressiveMiningRule::read));
	}
}
