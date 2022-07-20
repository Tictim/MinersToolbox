package tictim.minerstoolbox.config;

public interface ExplosionStat{
	float maxResistance();
	float force();
	int explosionRadius();
	boolean destroyDrop();

	record Record(float maxResistance, float force, int explosionRadius, boolean destroyDrop) implements ExplosionStat{}
}
