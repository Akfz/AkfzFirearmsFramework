package v.akfz.aff.physics.java.impl;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aff.bullet.BulletManager;
import v.akfz.aff.bullet.PendingHit;
import v.akfz.aff.physics.EntitySnapshot;
import v.akfz.aff.physics.cpp.CSnapshot;
import v.akfz.aff.physics.java.BulletPhysicsEngine;
import v.akfz.aff.physics.java.DDARaycast;

import java.util.List;

/**
 * The Java physics will no longer be updated.
 */
public class JavaBulletPhysics implements BulletPhysicsEngine {

	private static final double[] HIT_POS_BUFFER = new double[3];
	private static final int[] BLOCK_POS_BUFFER = new int[3];
	private static final Direction[] FACE_BUFFER = new Direction[1];

	@Override
	public boolean isAvailable() { return true; }

	@Override
	public String name() { return "Def_Java"; }

	@Override
	public void simulate(List<Bullet> bullets, double deltaTime, Vec3 wind,
	                     CSnapshot cSnapshot, EntitySnapshot entitySnapshot, @Nullable ServerLevel level) {

		for (Bullet b : bullets) {
			if (!b.isAlive()) continue;
			simulateBullet(b, deltaTime, wind.x, wind.y, wind.z, level, entitySnapshot);
		}
	}

	private static double effectivePadding(double caliberMM) {
		double radiusBlocks = (caliberMM / 1000.0) / 2.0;
		return 0.01 + radiusBlocks;
	}

	private void simulateBullet(Bullet b, double dt, double windX_kmh, double windY_kmh, double windZ_kmh,
	                            ServerLevel level, EntitySnapshot entitySnapshot) {
		var config = b.getConfig();
		Vec3 startPos = b.getState().position();
		Vec3 currentVel = b.getState().velocity();

		double posX = startPos.x;
		double posY = startPos.y;
		double posZ = startPos.z;
		double velX = currentVel.x;
		double velY = currentVel.y;
		double velZ = currentVel.z;

		double gravity = -0.08 * config.gravityMultiplier();
		double dragCoeff = config.dragCoefficient();

		double windX_bps = (windX_kmh / 3.6);
		double windY_bps = (windY_kmh / 3.6);
		double windZ_bps = (windZ_kmh / 3.6);

		double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
		boolean isSimplified = (speed < 5.0) || (0.5 * config.mass() * speed * speed * config.damageMultiplier() < 2.0);

		int steps = isSimplified ? 1 : Math.max(1, Math.min((int) Math.ceil((speed * dt) / 0.5), config.maxSubSteps()));
		double subDt = dt / steps;

		double padding = effectivePadding(config.caliber());

		int hitStatus = 0;
		double hitX = 0, hitY = 0, hitZ = 0;
		int hitExtra = 0;
		boolean alive = true;

		if (BulletManager.DEBUG) {
			System.out.println("[AFF_DEBUG] === Bullet " + b.getId() + " ===");
			System.out.println("[AFF_DEBUG] Start: " + String.format("%.2f, %.2f, %.2f", posX, posY, posZ));
			System.out.println("[AFF_DEBUG] Speed: " + String.format("%.2f", speed) + " | Steps: " + steps);
			System.out.println("[AFF_DEBUG] Wind (km/h): [" + String.format("%.1f", windX_kmh) + ", " + String.format("%.1f", windY_kmh) + ", " + String.format("%.1f", windZ_kmh) + "]");
			System.out.println("[AFF_DEBUG] Wind Applied (blocks/s): [" + String.format("%.2f", windX_bps) + ", " + String.format("%.2f", windY_bps) + ", " + String.format("%.2f", windZ_bps) + "]");
		}

		for (int step = 0; step < steps && alive; step++) {
			double relVelX = velX - windX_bps;
			double relVelY = velY - windY_bps;
			double relVelZ = velZ - windZ_bps;

			double relVelLen = Math.sqrt(relVelX * relVelX + relVelY * relVelY + relVelZ * relVelZ);
			double dragFactor = relVelLen > 0 ? -dragCoeff * relVelLen : 0;

			velX += (dragFactor * relVelX) * subDt;
			velY += (gravity + dragFactor * relVelY) * subDt;
			velZ += (dragFactor * relVelZ) * subDt;

			double nextX = posX + velX * subDt;
			double nextY = posY + velY * subDt;
			double nextZ = posZ + velZ * subDt;

			if (level != null) {
				boolean hitBlock = DDARaycast.raycast(level, posX, posY, posZ, nextX, nextY, nextZ,
						HIT_POS_BUFFER, BLOCK_POS_BUFFER, FACE_BUFFER);
				if (hitBlock) {
					hitStatus = 1;
					hitX = HIT_POS_BUFFER[0];
					hitY = HIT_POS_BUFFER[1];
					hitZ = HIT_POS_BUFFER[2];
					hitExtra = 0;
					alive = false;

					if (BulletManager.DEBUG) {
						System.out.println("[AFF_DEBUG] ✗ Hit BLOCK at " + String.format("%.2f, %.2f, %.2f", hitX, hitY, hitZ));
					}
					break;
				}
			}

			if (entitySnapshot != null) {
				PendingHit hit = entitySnapshot.findClosestHit(
						posX, posY, posZ, nextX, nextY, nextZ,
						padding,
						b.getShooterUuid(), config.allowFriendlyFire());
				if (hit != null) {
					hitStatus = 2;
					hitX = hit.hitPos().x;
					hitY = hit.hitPos().y;
					hitZ = hit.hitPos().z;
					hitExtra = hit.entity().getId();
					alive = false;

					if (BulletManager.DEBUG) {
						double dx = nextX - posX;
						double dy = nextY - posY;
						double dz = nextZ - posZ;
						double segLenSqr = dx * dx + dy * dy + dz * dz;
						double t = segLenSqr > 0 ? hit.distSqr() / segLenSqr : 0;

						System.out.println("[AFF_DEBUG] ✓ Hit ENTITY " + hitExtra + " at t=" + String.format("%.3f", t));
						System.out.println("[AFF_DEBUG]   Hit pos: " + String.format("%.2f, %.2f, %.2f", hitX, hitY, hitZ));
					}
					break;
				}
			}

			posX = nextX;
			posY = nextY;
			posZ = nextZ;
		}

		if (posY < -64.0) {
			hitStatus = -1;
			alive = false;
			if (BulletManager.DEBUG) {
				System.out.println("[AFF_DEBUG] ✗ Out of bounds (Y < -64)");
			}
		}

		b.updateStateFromNative((float) posX, (float) posY, (float) posZ,
				(float) velX, (float) velY, (float) velZ);

		if (!alive) {
			b.recordNativeResult(hitStatus, (float) hitX, (float) hitY, (float) hitZ, hitExtra,
					0, new float[16], new float[16], new float[16], new int[16], new int[16]
					, new int[16], new int[16]);
			if (BulletManager.DEBUG) {
				System.out.println("[AFF_DEBUG] Final pos: " + String.format("%.2f, %.2f, %.2f", posX, posY, posZ));
				System.out.println("[AFF_DEBUG] ==========================");
			}
		}
	}
}