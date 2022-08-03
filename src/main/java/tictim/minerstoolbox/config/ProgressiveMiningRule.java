package tictim.minerstoolbox.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static tictim.minerstoolbox.MinersToolboxMod.MODID;
import static tictim.minerstoolbox.config.ProgressiveMiningConfig.LENIENT_GSON;


public record ProgressiveMiningRule(
		Index index,
		BlockStateTest block,
		int substages,
		@Nullable IntProvider initialSubstage,
		@Nullable LootTable allLoot,
		Int2ObjectMap<LootTable> lootPerStage,
		@Nullable BlockStateTest changesTo
){
	@Nullable public LootTable getLoot(int substage){
		return lootPerStage.getOrDefault(Mth.clamp(substage, 1, substages), allLoot);
	}
	public int generateInitialSubstage(Random random){
		return initialSubstage==null ? substages : initialSubstage.sample(random);
	}

	public boolean isValid(){
		return block.hasMatchingBlockStates()&&
				substages>0&&
				(initialSubstage==null||initialSubstage.getMinValue()>0&&initialSubstage.getMaxValue()<=substages)&&
				(changesTo==null||changesTo.matchesOnlyOneState());
	}

	public void getInvalidReasons(Consumer<String> errorConsumer){
		if(!block.isValid()) errorConsumer.accept("block: Invalid block "+block);
		else if(!block.hasMatchingBlockStates()) errorConsumer.accept("block: Block "+block+" has no matches");
		if(substages<=0) errorConsumer.accept("substages: Non-positive number ("+substages+")");
		else if(initialSubstage!=null){
			int min = initialSubstage.getMinValue(), max = initialSubstage.getMaxValue();
			if(min<=0||max>substages)
				errorConsumer.accept("initial_substage: Invalid range ("+
						(min==max ? min+"" : min+"~"+max)+", 1~"+substages+" expected)");
		}

		if(changesTo!=null){
			if(!changesTo.isValid()) errorConsumer.accept("changes_to: Invalid block "+changesTo);
			else if(!changesTo.matchesOnlyOneState()){
				if(changesTo.hasMatchingBlockStates())
					errorConsumer.accept("changes_to: Block "+changesTo+" is ambiguous between "+
							Objects.requireNonNull(changesTo.getMatchingBlockStates()).size()+" states: ["+
							changesTo.getMatchingBlockStates().stream().map(Object::toString)
									.collect(Collectors.joining(","))+"]");
				else errorConsumer.accept("changes_to: Block "+changesTo+" has no matches");
			}
		}
	}

	public void update(){
		block.updateCache();
		if(changesTo!=null) changesTo.updateCache();
	}

	public JsonObject writeToJson(){
		JsonObject o = new JsonObject();
		o.addProperty("block", this.block.toString());
		if(this.substages>0) o.addProperty("substages", substages);
		if(this.initialSubstage!=null)
			o.add("initial_substage", IntProvider.POSITIVE_CODEC.encodeStart(JsonOps.INSTANCE, initialSubstage).getOrThrow(false, s -> {}));
		if(this.allLoot!=null||!lootPerStage.isEmpty()){
			JsonObject o2 = new JsonObject();
			if(this.allLoot!=null)
				o2.add("all", ProgressiveMiningConfig.PRETTY_GSON.toJsonTree(this.allLoot));
			for(Int2ObjectMap.Entry<LootTable> e : lootPerStage.int2ObjectEntrySet()){
				if(e.getIntKey()>=1&&e.getIntKey()<=this.substages)
					o2.add(e.getIntKey()+"", ProgressiveMiningConfig.PRETTY_GSON.toJsonTree(e.getValue()));
			}
			o.add("loots", o2);
		}
		if(this.changesTo!=null) o.addProperty("changes_to", this.changesTo.toString());
		return o;
	}

	public void write(FriendlyByteBuf buf){
		block.write(buf);
		buf.writeVarInt(substages);
		buf.writeBoolean(changesTo!=null);
		if(changesTo!=null) changesTo.write(buf);
	}

	@Override
	public String toString(){
		return index.toString();
	}

	public static ProgressiveMiningRule read(Index index, JsonObject json) throws JsonParseException{
		String block = GsonHelper.getAsString(json, "block");
		int substages = GsonHelper.getAsInt(json, "substages");

		IntProvider initialSubstage = json.has("initial_substage") ?
				IntProvider.CODEC.parse(JsonOps.INSTANCE, json.get("initial_substage"))
						.getOrThrow(false, s -> {
							throw new JsonParseException("Invalid IntProvider: "+s);
						}) : null;

		JsonObject lootsObj = GsonHelper.getAsJsonObject(json, "loots");

		JsonObject allLootObj = GsonHelper.getAsJsonObject(lootsObj, "all", null);
		LootTable allLoot = allLootObj!=null ? loadLootTable(allLootObj, index, "all") : null;
		Int2ObjectMap<LootTable> loots = new Int2ObjectOpenHashMap<>();
		for(int i = 1; i<=substages; i++){
			JsonObject lootObj = GsonHelper.getAsJsonObject(lootsObj, ""+i, null);
			if(lootObj==null) continue;
			loots.put(i, loadLootTable(lootObj, index, i+""));
		}

		String changesTo = GsonHelper.getAsString(json, "changes_to", null);
		BlockStateTest blockTest = BlockStateTest.parseOrNull(block);
		if(blockTest==null) throw new JsonParseException("Couldn't parse block field");
		BlockStateTest changesToTest;
		if(changesTo!=null){
			changesToTest = BlockStateTest.parseOrNull(changesTo);
			if(changesToTest==null) throw new JsonParseException("Couldn't parse changes_to field");
		}else changesToTest = null;

		return new ProgressiveMiningRule(index, blockTest, substages, initialSubstage, allLoot, loots, changesToTest);
	}

	private static LootTable loadLootTable(JsonElement json, Index index, String postfix){
		return ForgeHooks.loadLootTable(LENIENT_GSON,
				new ResourceLocation(MODID, "progressive_mining_"+Integer.toHexString(index.fileName.hashCode()&0xFFFF)+"_"+index.index+"_"+postfix),
				json, true,
				Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Trying to load loot table with no server instance").getLootTables());
	}

	public static ProgressiveMiningRule read(FriendlyByteBuf buf){
		BlockStateTest block = BlockStateTest.read(buf);
		int substages = buf.readVarInt();
		BlockStateTest changesTo = buf.readBoolean() ? BlockStateTest.read(buf) : null;
		return new ProgressiveMiningRule(new Index("", 0),
				block,
				substages,
				null,
				null,
				Int2ObjectMaps.emptyMap(),
				changesTo);
	}

	public static record Index(String fileName, int index) implements Comparable<Index>{
		@Override public String toString(){
			return fileName+"#"+index;
		}
		@Override public int compareTo(@NotNull ProgressiveMiningRule.Index index){
			int i = String.CASE_INSENSITIVE_ORDER.compare(this.fileName, index.fileName);
			return i!=0 ? i : Integer.compare(this.index, index.index);
		}
	}
}
