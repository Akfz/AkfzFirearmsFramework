package v.akfz.aff.gun.ammo;

import v.akfz.aff.bullet.BulletConfig;

public record AmmoType(
		String id,
		String displayName,
		BulletConfig bulletConfig,
		double muzzleVelocityMod,
		double damageMod,
		double penetrationMod
) {
}