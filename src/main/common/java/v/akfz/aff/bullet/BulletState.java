package v.akfz.aff.bullet;

import net.minecraft.world.phys.Vec3;

/**
 * Represents the current dynamic state of a projectile during its flight.
 * This record is immutable, ensuring thread-safety during multithreaded physics calculations.
 *
 * @param position         The current 3D coordinates of the bullet in the world.
 * @param velocity         The current velocity vector (direction and speed in blocks per second).
 * @param distanceTraveled The total distance the bullet has traveled since being spawned.
 * @param isAlive          Whether the bullet is still active and should continue to be simulated.
 */
public record BulletState(
		Vec3 position,
		Vec3 velocity,
		double distanceTraveled,
		boolean isAlive
) {
}