package v.akfz.aff.event.bullet;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aff.world.Material;

/**
 * Data for {@link BulletHitEvent}
 * Some data is nullable :D
 */
public final class BulletHitContext {

	private final Bullet        bullet;
	private final Entity        target;
	private final BlockHitResult blockHit;
	private final Vec3          hitPos;
	private final Vec3          velocity;
	private final double        speed;
	private final double        kineticEnergy;
	private final double        impactAngleDeg;
	private final Material      material;
	private float               damage;

	private BulletHitContext(Bullet bullet,
	                         Entity target,
	                         BlockHitResult blockHit,
	                         Vec3 hitPos,
	                         Vec3 velocity,
	                         double speed,
	                         double kineticEnergy,
	                         double impactAngleDeg,
	                         Material material,
	                         float damage) {
		this.bullet          = bullet;
		this.target          = target;
		this.blockHit        = blockHit;
		this.hitPos          = hitPos;
		this.velocity        = velocity;
		this.speed           = speed;
		this.kineticEnergy   = kineticEnergy;
		this.impactAngleDeg  = impactAngleDeg;
		this.material        = material;
		this.damage          = damage;
	}

	public static BulletHitContext forBlockHit(Bullet bullet, BlockHitResult blockHit, Material material) {
		Vec3 velocity = bullet.getState().velocity();
		double speed  = velocity.length();
		double energy = 0.5 * bullet.getConfig().mass() * speed * speed;

		Direction face = blockHit.getDirection();
		Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());

		double cos = 0.0;
		if (speed > 1e-6) {
			cos = Math.abs(velocity.scale(1.0 / speed).dot(normal));
			cos = Math.min(1.0, Math.max(0.0, cos));
		}
		double angleDeg = Math.toDegrees(Math.acos(cos));

		float damage = (float) energy;

		return new BulletHitContext(
				bullet, null, blockHit,
				blockHit.getLocation(), velocity, speed,
				energy, angleDeg, material, damage
		);
	}

	public static BulletHitContext forEntityHit(Bullet bullet, Entity target, Vec3 hitPos) {
		Vec3 velocity = bullet.getState().velocity();
		double speed  = velocity.length();
		double energy = 0.5 * bullet.getConfig().mass() * speed * speed;
		float  damage = (float) (energy * bullet.getConfig().damageMultiplier());

		return new BulletHitContext(
				bullet, target, null,
				hitPos, velocity, speed,
				energy, 0.0, null, damage
		);
	}

	public Bullet         bullet()         { return bullet; }
	public Entity         target()         { return target; }
	public BlockHitResult blockHit()       { return blockHit; }
	public Vec3           hitPos()         { return hitPos; }
	public Vec3           velocity()       { return velocity; }
	public double         speed()          { return speed; }
	public double         kineticEnergy()  { return kineticEnergy; }
	public double         impactAngleDeg() { return impactAngleDeg; }
	public Material       material()       { return material; }
	public float          damage()         { return damage; }

	public void setDamage(float damage) { this.damage = damage; }

	public boolean isBlockHit()  { return target == null; }
	public boolean isEntityHit() { return target != null; }
}