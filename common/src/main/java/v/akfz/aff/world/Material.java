package v.akfz.aff.world;

public record Material(
		double density,
		double hardness,
		double maxPenetrationJoules,
		boolean breakAble,
		boolean dropOnBreak
) {
}