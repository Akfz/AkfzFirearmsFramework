package v.akfz.aff.gun.ammo;

import v.akfz.aff.bullet.BulletConfig;

/**
 * What i need to write here, its basically data for creating multiple bullets from a single config.
 * @param id
 * @param displayName
 * @param bulletConfig
 * @param muzzleVelocityMod
 * @param damageMod
 * @param penetrationMod
 */
public record AmmoType(
		String id,
		String displayName,
		BulletConfig bulletConfig,
		double muzzleVelocityMod,
		double damageMod,
		double penetrationMod
) {
}