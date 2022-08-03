package tictim.minerstoolbox.progressivemining;

import net.minecraft.world.level.storage.loot.LootTable;
import tictim.minerstoolbox.config.ProgressiveMiningRule;

import javax.annotation.Nullable;

public sealed class DestroyResult{
	public static Success success(boolean destroyed, Progression progression, ProgressiveMiningRule rule){
		return new Success(destroyed, progression, rule);
	}
	public static Pass pass(){
		return Pass.PASS;
	}

	public final static class Success extends DestroyResult{
		public final boolean destroyed;
		public final Progression progression;
		public final ProgressiveMiningRule rule;

		public Success(boolean destroyed, Progression progression, ProgressiveMiningRule rule){
			this.destroyed = destroyed;
			this.progression = progression;
			this.rule = rule;
		}

		public int getHarvestedSubstage(){
			return progression.substage+1;
		}

		public boolean fullyHarvested(){
			return progression.substage<=0;
		}

		@Nullable public LootTable getLoot(){
			return rule.getLoot(getHarvestedSubstage());
		}

		@Override public String toString(){
			return "success("+destroyed+", "+progression+")";
		}
	}

	public final static class Pass extends DestroyResult{
		private static final Pass PASS = new Pass();
		private Pass(){}

		@Override public String toString(){
			return "pass";
		}
	}
}
