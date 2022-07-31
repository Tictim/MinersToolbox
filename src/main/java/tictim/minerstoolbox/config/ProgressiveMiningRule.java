package tictim.minerstoolbox.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.storage.loot.LootPool;

import javax.annotation.Nullable;

import static tictim.minerstoolbox.config.ProgressiveMiningConfig.LENIENT_GSON;


public record ProgressiveMiningRule(
		BlockStateTest block,
		int substages,
		IntProvider initialSubstage,
		@Nullable LootPool allLoot,
		Int2ObjectMap<LootPool> lootPerStage,
		BlockStateTest changesTo
){
	public static ProgressiveMiningRule read(JsonObject json) throws JsonParseException{
		String block = GsonHelper.getAsString(json, "block");
		int substages = GsonHelper.getAsInt(json, "substages");
		if(substages<=0) throw new JsonParseException("Non-positive substages");

		IntProvider initialSubstage;

		if(json.has("initial_substage")){
			DataResult<IntProvider> initialSubstageResult = IntProvider.POSITIVE_CODEC.parse(JsonOps.INSTANCE, json.get("initial_substage"));
			initialSubstageResult.error().ifPresent(r -> {
				throw new JsonParseException("Invalid IntProvider: "+r.message());
			});
			initialSubstage = initialSubstageResult.result().get();
		}else initialSubstage = ConstantInt.ZERO;

		JsonObject lootsObj = GsonHelper.getAsJsonObject(json, "loots");

		JsonObject allLootObj = GsonHelper.getAsJsonObject(lootsObj, "all", null);
		LootPool allLoot = allLootObj!=null ? LENIENT_GSON.fromJson(allLootObj, LootPool.class) : null;
		Int2ObjectMap<LootPool> loots = new Int2ObjectOpenHashMap<>();
		for(int i = 1; i<=substages; i++){
			JsonObject lootObj = GsonHelper.getAsJsonObject(lootsObj, ""+i, null);
			if(lootObj==null) continue;
			loots.put(i, LENIENT_GSON.fromJson(lootObj, LootPool.class));
		}

		String changesTo = GsonHelper.getAsString(json, "changes_to");
		BlockStateTest blockTest = BlockStateTest.parseOrNull(block);
		if(blockTest==null) throw new JsonParseException("Invalid block field");
		BlockStateTest changesToTest = BlockStateTest.parseOrNull(changesTo);
		if(changesToTest==null) throw new JsonParseException("Invalid changes_to field");

		return new ProgressiveMiningRule(blockTest, substages, initialSubstage, allLoot, loots, changesToTest);
	}

	@Nullable public LootPool getLoot(int substage){
		return lootPerStage.getOrDefault(substage, allLoot);
	}

	public JsonObject writeToJson(){
		JsonObject o = new JsonObject();
		o.addProperty("block", this.block.toString());
		if(this.substages>0) o.addProperty("substages", substages);
		if(!(this.initialSubstage instanceof ConstantInt ci)||ci.getValue()!=0)
			o.add("initial_substage", IntProvider.POSITIVE_CODEC.encodeStart(JsonOps.INSTANCE, initialSubstage).getOrThrow(false, s -> {}));
		if(this.allLoot!=null||!lootPerStage.isEmpty()){
			JsonObject o2 = new JsonObject();
			if(this.allLoot!=null)
				o2.add("all", ProgressiveMiningConfig.PRETTY_GSON.toJsonTree(this.allLoot));
			for(Int2ObjectMap.Entry<LootPool> e : lootPerStage.int2ObjectEntrySet()){
				if(e.getIntKey()>=1&&e.getIntKey()<=this.substages)
					o2.add(e.getIntKey()+"", ProgressiveMiningConfig.PRETTY_GSON.toJsonTree(e.getValue()));
			}
			o.add("loots", o2);
		}
		o.addProperty("changes_to", this.changesTo.toString());
		return o;
	}

	public void write(FriendlyByteBuf buf){
		block.write(buf);
		buf.writeVarInt(substages);
		buf.writeWithCodec(IntProvider.CODEC, initialSubstage);
		buf.writeBoolean(allLoot!=null);
		if(allLoot!=null) buf.writeUtf(LENIENT_GSON.toJson(allLoot));
		buf.writeVarInt(lootPerStage.size());
		for(Int2ObjectMap.Entry<LootPool> e : lootPerStage.int2ObjectEntrySet()){
			buf.writeVarInt(e.getIntKey());
			buf.writeUtf(LENIENT_GSON.toJson(e.getValue()));
		}
		changesTo.write(buf);
	}

	public static ProgressiveMiningRule read(FriendlyByteBuf buf){
		return new ProgressiveMiningRule(
				BlockStateTest.read(buf),
				buf.readVarInt(),
				buf.readWithCodec(IntProvider.CODEC),
				buf.readBoolean() ? LENIENT_GSON.fromJson(buf.readUtf(), LootPool.class) : null,
				readLootPerStage(buf),
				BlockStateTest.read(buf));
	}

	private static Int2ObjectMap<LootPool> readLootPerStage(FriendlyByteBuf buf){
		Int2ObjectMap<LootPool> m = new Int2ObjectOpenHashMap<>();
		for(int i = 0, j = buf.readVarInt(); i<j; i++){
			m.put(buf.readVarInt(), LENIENT_GSON.fromJson(buf.readUtf(), LootPool.class));
		}
		return m;
	}
}
