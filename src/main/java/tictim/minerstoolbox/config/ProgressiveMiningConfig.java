package tictim.minerstoolbox.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.slf4j.Logger;
import tictim.minerstoolbox.MinersToolboxMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProgressiveMiningConfig{
	public static final Gson LENIENT_GSON = Deserializers.createLootTableSerializer()
			.registerTypeHierarchyAdapter(NumberProvider.class, NumberProviders.createGsonAdapter())
			.setLenient()
			.create();
	public static final Gson PRETTY_GSON = Deserializers.createLootTableSerializer()
			.registerTypeHierarchyAdapter(NumberProvider.class, NumberProviders.createGsonAdapter())
			.setPrettyPrinting()
			.create();

	private final List<ProgressiveMiningRule> rules;
	private final Map<ProgressiveMiningRule.Index, Either<ProgressiveMiningRule, Exception>> rulesMap = new HashMap<>();
	private List<ProgressiveMiningRule> validRules = List.of();

	public ProgressiveMiningConfig(){
		this(new ArrayList<>());
	}
	public ProgressiveMiningConfig(List<ProgressiveMiningRule> rules){
		this.rules = rules;
		for(ProgressiveMiningRule r : rules)
			this.rulesMap.putIfAbsent(r.index(), Either.left(r));
	}

	@Nullable public ProgressiveMiningRule getMatchingRule(BlockState state){
		if(state.isAir()) return null;
		for(ProgressiveMiningRule r : validRules){
			if(r.block().testState(state)) return r;
		}
		return null;
	}

	/**
	 * @return Unmodifiable list of all rules, including both valid and invalid ones<br>
	 * Note that the rules that failed to load due to JSON syntax error or such will not be included in this list
	 */
	public List<ProgressiveMiningRule> getRules(){
		return Collections.unmodifiableList(rules);
	}
	/**
	 * @return Unmodifiable list of all valid rules<br>
	 * @see ProgressiveMiningConfig#updateAndValidate()
	 */
	public List<ProgressiveMiningRule> getValidRules(){
		return validRules;
	}

	/**
	 * @return Set of all known rule indices, including both valid and invalid ones
	 */
	public Set<ProgressiveMiningRule.Index> getAllKnownIndices(){
		return Collections.unmodifiableSet(this.rulesMap.keySet());
	}

	public RuleValidity getRuleValidity(ProgressiveMiningRule.Index index){
		Either<ProgressiveMiningRule, Exception> either = rulesMap.get(index);
		if(either==null) return RuleValidity.NOT_FOUND;
		if(either.left().isPresent()&&either.left().get().isValid()) return RuleValidity.VALID;
		return RuleValidity.INVALID;
	}

	public void load(String fileName, JsonObject object){
		load(fileName, object, true);
	}
	public void load(String fileName, JsonObject object, boolean reportError){
		try{
			JsonArray rules = GsonHelper.getAsJsonArray(object, "rules");
			for(int i = 0; i<rules.size(); i++){
				ProgressiveMiningRule.Index index = new ProgressiveMiningRule.Index(fileName, i+1);
				try{
					ProgressiveMiningRule rule = ProgressiveMiningRule.read(index, rules.get(i).getAsJsonObject());
					this.rules.add(rule);
					this.rulesMap.putIfAbsent(index, Either.left(rule));
				}catch(RuntimeException ex){
					if(reportError)
						MinersToolboxMod.LOGGER.debug("Invalid data in progressive mining config file \"{}\", entry {} ", fileName, i+1, ex);
					this.rulesMap.put(index, Either.right(ex));
				}
			}
		}catch(JsonParseException ex){
			if(reportError)
				MinersToolboxMod.LOGGER.debug("Invalid data in progressive mining config file \"{}\" ", fileName, ex);
		}
	}

	public void updateAndValidate(){
		for(ProgressiveMiningRule config : rules) config.update();
		validRules = List.copyOf(rules.stream().filter(ProgressiveMiningRule::isValid).collect(Collectors.toList()));
	}

	public void printAllInvalidReasons(Logger logger){
		this.rulesMap.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(e -> e.getValue().ifLeft(r -> {
					if(r.isValid()) return;
					logger.warn("Rule {} was discarded:", e.getKey());
					r.getInvalidReasons(s -> logger.warn("  {}", s));
				}).ifRight(ex -> {
					logger.warn("Rule {} was discarded:", e.getKey());
					logger.warn("  Invalid JSON data: ", ex);
				}));
	}

	public void clear(){
		this.rules.clear();
		this.rulesMap.clear();
		this.validRules = List.of();
	}

	public void write(FriendlyByteBuf buf){
		buf.writeCollection(this.validRules, (b, c) -> c.write(b));
	}

	@Override public String toString(){
		return "ProgressiveMiningConfig["+this.rulesMap.keySet().stream().map(Object::toString).collect(Collectors.joining(","))+"]";
	}

	public static ProgressiveMiningConfig read(FriendlyByteBuf buf){
		return new ProgressiveMiningConfig(buf.readList(ProgressiveMiningRule::read));
	}

	public enum RuleValidity{
		NOT_FOUND,
		VALID,
		INVALID
	}
}
