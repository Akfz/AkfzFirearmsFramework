package v.akfz.aff.event.bullet;

import net.minecraft.world.phys.BlockHitResult;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aff.world.Material;
import v.akfz.aslib.event.api.Cancellable;
import v.akfz.aslib.event.api.Event;

public class BulletBlockHitEvent extends Event implements Cancellable {
	private final Bullet bullet;
	private final BlockHitResult hitResult;
	private Material material;
	private double energy;
	private boolean cancelled = false;

	public BulletBlockHitEvent(Bullet bullet, BlockHitResult hitResult, Material material, double energy) {
		this.bullet = bullet;
		this.hitResult = hitResult;
		this.material = material;
		this.energy = energy;
	}

	@Override
	public boolean isCancelled() { return cancelled; }
	@Override
	public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

	public Bullet getBullet() { return bullet; }
	public BlockHitResult getHitResult() { return hitResult; }
	public Material getMaterial() { return material; }
	public void setMaterial(Material material) { this.material = material; }
	public double getEnergy() { return energy; }
	public void setEnergy(double energy) { this.energy = energy; }
}