package tictim.minerstoolbox.config;

public class ExplosionStats{
	public static final ExplosionStat CRUDE = new ExplosionStat.Record(10, 6*3, 3, false);
	public static final ExplosionStat IMPROVED = new ExplosionStat.Record(100, 50*5, 5, false);
	public static final ExplosionStat ENHANCED = new ExplosionStat.Record(1000, 400*7, 7, false);
	public static final ExplosionStat SUPERB = new ExplosionStat.Record(10000, 300*9, 9, false);
	public static final ExplosionStat SUPERCALIFRAGILISTICEXPIALIDOCIOUS = new ExplosionStat.Record(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 30, true);
}
