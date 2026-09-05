package v.akfz.aff.event.bullet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aslib.event.api.Cancellable;
import v.akfz.aslib.event.api.Event;

public class BulletEntityHitEvent extends Event implements Cancellable {
	private final Bullet bullet;
	private Entity target;
	private Vec3 hitLocation;
	private float damage;
	private boolean cancelled = false;

	public BulletEntityHitEvent(Bullet bullet, Entity target, Vec3 hitLocation, float damage) {
		this.bullet = bullet;
		this.target = target;
		this.hitLocation = hitLocation;
		this.damage = damage;
	}

	@Override
	public boolean isCancelled() { return cancelled; }
	@Override
	public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

	public Bullet getBullet() { return bullet; }
	public Entity getTarget() { return target; }
	public void setTarget(Entity target) { this.target = target; }
	public Vec3 getHitLocation() { return hitLocation; }
	public void setHitLocation(Vec3 hitLocation) { this.hitLocation = hitLocation; }
	public float getDamage() { return damage; }
	public void setDamage(float damage) { this.damage = damage; }
}