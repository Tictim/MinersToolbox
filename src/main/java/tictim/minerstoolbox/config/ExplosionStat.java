package tictim.minerstoolbox.config;

public interface ExplosionStat{
	ExplosionStat EMPTY = new Record(0, 0, 0);

	float maxResistance();
	float force();
	int explosionRadius();

	record Record(float maxResistance, float force, int explosionRadius) implements ExplosionStat{}
}
