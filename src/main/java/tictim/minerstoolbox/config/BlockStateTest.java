package tictim.minerstoolbox.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BlockStateTest{
	// ((?:[a-z0-9_.-]+:)?[a-z0-9_.-]+)(?:\s*#\s*([A-Za-z0-9_.-]+\s*=\s*[A-Za-z0-9_.-]+(?:\s*,\s*[A-Za-z0-9_.-]+\s*=\s*[A-Za-z0-9_.-]+)*))?
	private static final Pattern REGEX = Pattern.compile("^((?:[a-z0-9_.-]+:)?[a-z0-9_.-]+)(?:\\s*#\\s*([A-Za-z0-9_.-]+\\s*=\\s*[A-Za-z0-9_.-]+(?:\\s*,\\s*[A-Za-z0-9_.-]+\\s*=\\s*[A-Za-z0-9_.-]+)*))?$");

	@Nullable public static BlockStateTest parseOrNull(String str){
		Matcher match = REGEX.matcher(str);
		if(!match.matches()) return null;
		String block = match.group(1);

		String g2 = match.group(2);
		Map<String, String> states = g2==null ? Map.of() :
				Arrays.stream(g2.split(",")).collect(HashMap::new, (m, s) -> {
					int i = s.indexOf('=');
					String key = s.substring(0, i);
					String val = s.substring(i+1);

					m.put(key, val);
				}, (m, m2) -> {});
		return new BlockStateTest(new ResourceLocation(block), states);
	}

	private final ResourceLocation block;
	private final Map<String, String> states;

	@Nullable private Block blockCache;
	@Nullable private Set<BlockState> matchingBlockStateCache;

	public BlockStateTest(ResourceLocation block, Map<String, String> states){
		this.block = Objects.requireNonNull(block);
		this.states = ImmutableMap.copyOf(states);
	}

	public boolean isValid(){
		return blockCache!=null&&matchingBlockStateCache!=null;
	}
	public boolean matchesOnlyOneState(){
		return isValid()&&matchingBlockStateCache.size()==1;
	}

	public boolean testState(BlockState state){
		return isValid()&&state.getBlock()==blockCache&&matchingBlockStateCache.contains(state);
	}

	@Nullable public Set<BlockState> getMatchingBlockStates(){
		return matchingBlockStateCache;
	}

	public boolean hasMatchingBlockStates(){
		return isValid()&&!matchingBlockStateCache.isEmpty();
	}

	@Nullable public BlockState getOnlyMatchingBlockState(){
		return matchingBlockStateCache!=null&&matchingBlockStateCache.size()==1 ?
				matchingBlockStateCache.iterator().next() : null;
	}

	public void updateCache(){
		this.blockCache = ForgeRegistries.BLOCKS.containsKey(block) ? ForgeRegistries.BLOCKS.getValue(block) : null;
		if(this.blockCache!=null){
			ImmutableSet.Builder<BlockState> b = new ImmutableSet.Builder<>();

			StateDefinition<Block, BlockState> stateDefinition = this.blockCache.getStateDefinition();
			for(BlockState state : stateDefinition.getPossibleStates()){
				boolean matchesAll = true;
				for(Map.Entry<String, String> e : this.states.entrySet()){
					Property<?> property = stateDefinition.getProperty(e.getKey());
					if(property!=null){
						Optional<?> value = property.getValue(e.getValue());
						if(value.isPresent()&&Objects.equals(state.getValue(property), value.get())){
							continue;
						}
					}
					matchesAll = false;
					break;
				}
				if(matchesAll) b.add(state);
			}

			this.matchingBlockStateCache = b.build();
			if(this.matchingBlockStateCache.isEmpty())
				this.matchingBlockStateCache = null;
		}else this.matchingBlockStateCache = null;
	}

	public void destroyCache(){
		this.blockCache = null;
		this.matchingBlockStateCache = null;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;

		BlockStateTest that = (BlockStateTest)o;

		if(!Objects.equals(block, that.block)) return false;
		return states.equals(that.states);
	}
	@Override public int hashCode(){
		return 31*block.hashCode()+states.hashCode();
	}
	@Override public String toString(){
		return states.isEmpty() ? block.toString() :
				block+"#"+states.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining(","));
	}
	public void write(FriendlyByteBuf buf){
		buf.writeResourceLocation(block);
		buf.writeMap(states, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
	}
	public static BlockStateTest read(FriendlyByteBuf buf){
		return new BlockStateTest(buf.readResourceLocation(), buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf));
	}
}
