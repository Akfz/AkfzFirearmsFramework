package v.akfz.aff.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.LoaderConfigs;
import v.akfz.aff.event.bullet.BulletBlockHitEvent;
import v.akfz.aff.event.bullet.BulletEntityHitEvent;
import v.akfz.aff.world.Material;
import v.akfz.aslib.AsLib;

import java.util.UUID;

public class Bullet {
	protected BulletVisual visual;

	protected final long id;
	protected final ServerLevel level;
	protected final BulletConfig config;
	protected final UUID shooterUuid;
	protected BulletState state;
	protected Vec3 previousPosition;
	protected boolean alive = true;

	protected volatile int pendingHitStatus = 0;
	protected volatile float pendingHitX = 0, pendingHitY = 0, pendingHitZ = 0, pendingHitExtra = 0;

	protected volatile int pendingPenCount = 0;
	protected volatile float[] pendingPenX = new float[16];
	protected volatile float[] pendingPenY = new float[16];
	protected volatile float[] pendingPenZ = new float[16];
	protected volatile int[] pendingPenMatId = new int[16];
	protected volatile int[] pendingPenBX = new int[16];
	protected volatile int[] pendingPenBY = new int[16];
	protected volatile int[] pendingPenBZ = new int[16];

	protected volatile int lodStepsCap = Integer.MAX_VALUE;

	public Bullet(long id, ServerLevel level, BulletConfig config, BulletState initialState, UUID shooterUuid) {
		this.id = id;
		this.level = level;
		this.config = config;
		this.shooterUuid = shooterUuid;
		this.state = initialState;
		this.previousPosition = initialState.position();
		this.visual = createVisual();
	}

	protected BulletVisual createVisual() {
		return new BulletVisual();
	}

	public BulletVisual getVisual() {
		return visual;
	}

	public boolean requiresCustomPhysics() {
		return false;
	}

	public void tickCustomPhysics(double deltaTime) {
		this.previousPosition = this.state.position();
		Vec3 currentVel = this.state.velocity().add(new Vec3(0.0, -0.08 * config.gravityMultiplier(), 0.0).scale(deltaTime));
		Vec3 currentPos = this.state.position().add(currentVel.scale(deltaTime));
		this.state = new BulletState(currentPos, currentVel, this.state.distanceTraveled() + currentVel.length() * deltaTime, true);
	}

	protected void onBlockHit(BlockHitResult hitResult, BlockState blockState) {
		double speed = state.velocity().length();
		double energy = 0.5 * config.mass() * speed * speed;

		Material material = getMaterial(blockState);
		Vec3 hitLocation = hitResult.getLocation();

		BulletBlockHitEvent event = new BulletBlockHitEvent(this, hitResult, material, energy);
		AsLib.EVENT_BUS.post(event);
		if (event.isCancelled()) return;

		visual.onBlockHit(level, hitLocation, LoaderConfigs.INSTANCE.getMaterialId(material));

		Vec3 normal = new Vec3(hitResult.getDirection().getStepX(), hitResult.getDirection().getStepY(), hitResult.getDirection().getStepZ());
		Vec3 velNorm = state.velocity().normalize();
		double angle = Math.toDegrees(Math.acos(Math.abs(velNorm.dot(normal))));
		double ricochetChance = angle > 45.0 ? ((angle - 45.0) / 45.0) * (material.hardness() / 10.0) : 0.0;

		if (Math.random() < ricochetChance) {
			visual.onRicochet(level, hitLocation);
			Vec3 reflected = state.velocity().subtract(normal.scale(2 * state.velocity().dot(normal)));
			double energyLoss = 0.4 + (Math.random() * 0.3);
			Vec3 newPos = hitLocation.add(reflected.normalize().scale(0.1));
			this.state = new BulletState(newPos, reflected.scale(1.0 - energyLoss), state.distanceTraveled(), true);
			this.previousPosition = newPos;
			return;
		}

		double effectiveEnergy = energy * config.penetrationPower();
		if (effectiveEnergy > material.maxPenetrationJoules()) {
			visual.onPenetration(level, hitLocation);
			if (material.breakAble()) level.destroyBlock(hitResult.getBlockPos(), material.dropOnBreak());
			double newSpeed = speed * (1.0 - material.density() * 0.15);
			if (newSpeed < 1.0) {
				visual.onDestroyed(level, hitLocation);
				markDead();
				return;
			}
			Vec3 newPos = hitLocation.add(state.velocity().normalize().scale(0.1));
			this.state = new BulletState(newPos, state.velocity().normalize().scale(newSpeed), state.distanceTraveled(), true);
			this.previousPosition = newPos;
			return;
		}

		visual.onDestroyed(level, hitLocation);
		markDead();
	}

	protected void onEntityHit(PendingHit hit) {
		double speed = state.velocity().length();
		float damage = (float) (0.5 * config.mass() * speed * speed * config.damageMultiplier());

		BulletEntityHitEvent event = new BulletEntityHitEvent(this, hit.entity(), hit.hitPos(), damage);
		AsLib.EVENT_BUS.post(event);
		if (event.isCancelled()) return;

		Entity target = event.getTarget();
		if (target == null || event.getDamage() <= 0) return;

		visual.onEntityHit(level, hit.hitPos(), hit.entity().getId());

		Entity shooter = shooterUuid != null ? level.getEntity(shooterUuid) : null;
		DamageSource source = (shooter instanceof ServerPlayer p) ? level.damageSources().playerAttack(p) :
				(shooter instanceof LivingEntity m) ? level.damageSources().mobAttack(m) : level.damageSources().generic();

		target.invulnerableTime = 0;
		boolean hurt = target.hurt(source, event.getDamage());

		if (hurt && target instanceof LivingEntity livingTarget && shooter instanceof LivingEntity livingShooter) {
			livingTarget.setLastHurtByMob(livingShooter);
			if (target instanceof Mob mobTarget) mobTarget.setTarget(livingShooter);
			if (target instanceof Animal animal) animal.setLastHurtByMob(livingShooter);
		}
		markDead();
	}

	protected void onOutOfBounds(Vec3 pos) {
		visual.onOutOfBounds(level, pos);
		markDead();
	}

	public void recordNativeResult(int status, float hx, float hy, float hz, float extra,
	                               int penCount, float[] penX, float[] penY, float[] penZ, int[] penMatId,
	                               int[] penBX, int[] penBY, int[] penBZ) {
		this.pendingHitStatus = status;
		this.pendingHitX = hx;
		this.pendingHitY = hy;
		this.pendingHitZ = hz;
		this.pendingHitExtra = extra;

		this.pendingPenCount = penCount;
		System.arraycopy(penX, 0, this.pendingPenX, 0, 16);
		System.arraycopy(penY, 0, this.pendingPenY, 0, 16);
		System.arraycopy(penZ, 0, this.pendingPenZ, 0, 16);
		System.arraycopy(penMatId, 0, this.pendingPenMatId, 0, 16);
		System.arraycopy(penBX, 0, this.pendingPenBX, 0, 16);
		System.arraycopy(penBY, 0, this.pendingPenBY, 0, 16);
		System.arraycopy(penBZ, 0, this.pendingPenBZ, 0, 16);
	}

	public void processPendingResults() {
		if (pendingHitStatus != 0) {
			applyNativeResult(pendingHitStatus, pendingHitX, pendingHitY, pendingHitZ, pendingHitExtra,
					pendingPenCount, pendingPenX, pendingPenY, pendingPenZ, pendingPenMatId,
					pendingPenBX, pendingPenBY, pendingPenBZ);
			pendingHitStatus = 0;
			pendingPenCount = 0;
		}
	}

	public void updateStateFromNative(float nx, float ny, float nz, float nvx, float nvy, float nvz) {
		this.previousPosition = this.state.position();
		double newDistance = this.state.distanceTraveled() + Math.sqrt(nvx*nvx + nvy*nvy + nvz*nvz) * 0.025;
		this.state = new BulletState(new Vec3(nx, ny, nz), new Vec3(nvx, nvy, nvz), newDistance, (nvx*nvx + nvy*nvy + nvz*nvz) > 0.001);
	}

	protected void onRicochet(Vec3 hitPos) {
		visual.onRicochet(level, hitPos);
	}

	protected void onPenetration(Vec3 hitPos) {
		visual.onPenetration(level, hitPos);
	}

	protected void onDestroyed(Vec3 position) {
		visual.onDestroyed(level, position);
	}

	/**
	 * status: -1 = out of bounds, 1 = block, 2 = entity, 3 = ricochet, 4 = destroyed, 5 = penetrated
	 */
	public void applyNativeResult(int status, float hx, float hy, float hz, float extra,
	                              int penCount, float[] penX, float[] penY, float[] penZ, int[] penMatId,
	                              int[] penBX, int[] penBY, int[] penBZ) {
		Vec3 hitPos = new Vec3(hx, hy, hz);

		if (penCount > 0) {
			for (int i = 0; i < penCount; i++) {
				BlockPos pos = new BlockPos(penBX[i], penBY[i], penBZ[i]);
				BlockState blockState = level.getBlockState(pos);
				Material material = getMaterial(blockState);

				if (BulletManager.DEBUG) {
					System.out.println("[AFF_DEBUG_JAVA] Processing penetration " + i +
							" at BlockPos(" + penBX[i] + ", " + penBY[i] + ", " + penBZ[i] + ")");
				}

				if (material.breakAble()) {
					level.destroyBlock(pos, material.dropOnBreak());
					if (BulletManager.DEBUG) {
						System.out.println("[AFF_DEBUG_JAVA] Block destroyed at: " + pos +
								" (MatID: " + penMatId[i] + ")");
					}
				}
				visual.onPenetration(level, new Vec3(penX[i], penY[i], penZ[i]));
			}
		}

		if (status == -1) {
			onOutOfBounds(hitPos);
			return;
		}

		if (status == 2) {
			Entity target = level.getEntity((int) extra);
			if (target != null && target.isAlive()) {
				onEntityHit(new PendingHit(target, hitPos, previousPosition.distanceToSqr(hitPos)));
			} else {
				onOutOfBounds(hitPos);
			}
		} else if (status == 1) {
			BlockPos pos = BlockPos.containing(hitPos);
			BlockState blockState = level.getBlockState(pos);
			Vec3 velDir = this.state.velocity().normalize().scale(-1);
			Direction dir = Direction.getNearest(velDir.x, velDir.y, velDir.z);
			onBlockHit(new BlockHitResult(hitPos, dir, pos, false), blockState);
		} else if (status == 3) {
			onRicochet(hitPos);
			this.alive = true;
		} else if (status == 4) {
			onDestroyed(hitPos);
			markDead();
		} else if (status == 5) {
			this.alive = true;
		}
	}

	public long getId() { return id; }
	public ServerLevel getLevel() { return level; }
	public BulletConfig getConfig() { return config; }
	public UUID getShooterUuid() { return shooterUuid; }
	public boolean isAlive() { return alive && state.isAlive(); }
	public BulletState getState() { return state; }
	public Vec3 getPreviousPosition() { return previousPosition; }
	public void setLodStepsCap(int cap) { this.lodStepsCap = cap; }
	public int getLodStepsCap() { return lodStepsCap; }
	public void markDead() { this.alive = false; }

	protected Material getMaterial(BlockState state) {
		ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return LoaderConfigs.INSTANCE.getMaterialByRL(rl);
	}
}