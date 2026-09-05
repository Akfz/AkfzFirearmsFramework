package v.akfz.aff.bullet;

/**
 * Represents the ballistic and physical configuration of a projectile.
 * This record defines both real-world physical properties (mass, velocity, drag)
 * and gameplay-specific modifiers (damage, friendly fire, penetration).
 *
 * @param muzzleVelocity      Initial velocity of the bullet in blocks per second (e.g., 300.0 = 300 m/s).
 * @param mass                Mass of the bullet in kilograms (e.g., 0.004 = 4 grams).
 * @param dragCoefficient     Air resistance coefficient affecting deceleration over distance.
 * @param gravityMultiplier   Multiplier for gravitational pull (1.0 represents standard Minecraft gravity).
 * @param maxSubSteps         Maximum number of sub-steps for raycasting to prevent tunneling at high speeds.
 * @param penetrationPower    Base penetration capability of the projectile against materials.
 * @param allowFriendlyFire   Whether the bullet is allowed to damage the shooter or allied entities.
 * @param damageMultiplier    Multiplier applied to the bullet's kinetic energy to calculate final Minecraft damage.
 * @param caliber             Physical diameter of the bullet in blocks (e.g., 0.00762 for 7.62mm),
 *                            used to slightly expand target hitboxes for realistic collision detection.
 */
public record BulletConfig(
		double muzzleVelocity,
		double mass,
		double dragCoefficient,
		double gravityMultiplier,
		int maxSubSteps,
		double penetrationPower,
		boolean allowFriendlyFire,
		float damageMultiplier,
		double caliber
) {
}