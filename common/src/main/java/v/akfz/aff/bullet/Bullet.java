package v.akfz.aff.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.event.bullet.BulletBlockHitEvent;
import v.akfz.aff.event.bullet.BulletEntityHitEvent;
import v.akfz.aff.world.Material;
import v.akfz.aslib.AsLib;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Bullet {
	public static final double FAR_DISTANCE = 1000.0;
	private static final double PREDICT_RADIUS = 5.0;
	private static final double PREDICT_DT = 0.25;
	private static final int PREDICT_MAX_SAMPLES = 80;
	private static final long PREDICT_RECHECK_MS = 2000;
	private static final float MIN_USEFUL_DAMAGE = 2.0f;
	private static final double MIN_USEFUL_SPEED = 5.0;

	private static final Map<ResourceKey<Level>, Entity> DUMMY_ENTITIES = new ConcurrentHashMap<>();

	private static Entity getDummyEntity(ServerLevel level) {
		return DUMMY_ENTITIES.computeIfAbsent(level.dimension(), k -> new Entity(EntityType.ITEM, level) {
			@Override protected void defineSynchedData() {}
			@Override protected void readAdditionalSaveData(CompoundTag compound) {}
			@Override protected void addAdditionalSaveData(CompoundTag compound) {}
			@Override public boolean isDescending() { return false; }
			@Override public boolean isCrouching() { return false; }
		});
	}

	private final long id;
	private final ServerLevel level;
	private final BulletConfig config;
	private final UUID shooterUuid;
	private BulletState state;
	private Vec3 previousPosition;
	private boolean alive = true;

	private volatile PendingHit pendingEntityHit = null;
	private volatile int lodStepsCap = Integer.MAX_VALUE;
	private long lastPredictiveCheck = 0;

	public Bullet(long id, ServerLevel level, BulletConfig config, BulletState initialState, UUID shooterUuid) {
		this.id = id;
		this.level = level;
		this.config = config;
		this.shooterUuid = shooterUuid;
		this.state = initialState;
		this.previousPosition = initialState.position();
	}

	public void simulatePhysics(double deltaTime, Vec3 wind, int stepsCap) {
		this.previousPosition = this.state.position();
		double speed = this.state.velocity().length();

		Vec3 currentPos = this.state.position();
		Vec3 currentVel = this.state.velocity();

		if (isSimplified()) {
			currentVel = currentVel.add(
					new Vec3(0.0, -0.08 * config.gravityMultiplier(), 0.0).scale(deltaTime));
			currentPos = currentPos.add(currentVel.scale(deltaTime));
		} else {
			int steps = (int) Math.ceil(speed * deltaTime / 0.25);
			steps = Math.max(1, Math.min(steps, Math.min(config.maxSubSteps(), stepsCap)));

			double subDt = deltaTime / steps;
			for (int i = 0; i < steps; i++) {
				Vec3 gravity = new Vec3(0.0, -0.08 * config.gravityMultiplier(), 0.0);
				double velLength = currentVel.length();
				Vec3 drag = velLength > 0 ? currentVel.scale(-config.dragCoefficient() * velLength) : Vec3.ZERO;
				Vec3 acceleration = gravity.add(drag).add(wind.scale(0.1));
				currentVel = currentVel.add(acceleration.scale(subDt));
				currentPos = currentPos.add(currentVel.scale(subDt));
			}
		}

		double newDistance = this.state.distanceTraveled() + currentVel.length() * deltaTime;
		this.state = new BulletState(currentPos, currentVel, newDistance, currentVel.lengthSqr() > 0.001);
	}

	public boolean isSimplified() {
		double speed = state.velocity().length();
		if (speed < MIN_USEFUL_SPEED) return true;
		double damage = 0.5 * config.mass() * speed * speed * config.damageMultiplier();
		return damage < MIN_USEFUL_DAMAGE;
	}

	public boolean shouldCullFar(EntitySnapshot snapshot, long nowMs) {
		if (nowMs - lastPredictiveCheck < PREDICT_RECHECK_MS) return false;
		lastPredictiveCheck = nowMs;

		Vec3 pos = state.position();
		Vec3 vel = state.velocity();
		double minX = pos.x, minY = pos.y, minZ = pos.z;
		double maxX = pos.x, maxY = pos.y, maxZ = pos.z;

		double[] px = new double[PREDICT_MAX_SAMPLES];
		double[] py = new double[PREDICT_MAX_SAMPLES];
		double[] pz = new double[PREDICT_MAX_SAMPLES];
		int n = 0;

		for (int i = 0; i < PREDICT_MAX_SAMPLES; i++) {
			double speed = vel.length();
			if (speed < 1.0) break;

			Vec3 drag = vel.scale(-config.dragCoefficient() * speed);
			vel = vel.add(new Vec3(0.0, -0.08 * config.gravityMultiplier(), 0.0).add(drag).scale(PREDICT_DT));
			pos = pos.add(vel.scale(PREDICT_DT));

			px[n] = pos.x; py[n] = pos.y; pz[n] = pos.z; n++;

			minX = Math.min(minX, pos.x); maxX = Math.max(maxX, pos.x);
			minY = Math.min(minY, pos.y); maxY = Math.max(maxY, pos.y);
			minZ = Math.min(minZ, pos.z); maxZ = Math.max(maxZ, pos.z);

			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
					Mth.floor(pos.x), Mth.floor(pos.z));
			if (pos.y <= ground) break;
		}

		if (n == 0) return true;

		AABB pathBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(PREDICT_RADIUS);
		List<EntitySnapshot.Entry> candidates = snapshot.entriesIn(pathBox);
		if (candidates.isEmpty()) return true;

		for (int i = 0; i < n; i++) {
			double time = (i + 1) * PREDICT_DT;
			for (EntitySnapshot.Entry e : candidates) {
				AABB box = e.box().inflate(PREDICT_RADIUS * 0.5)
						.move(e.entity().getDeltaMovement().scale(time));
				if (box.contains(px[i], py[i], pz[i])) {
					return false;
				}
			}
		}
		return true;
	}

	public void checkEntityHits(EntitySnapshot snapshot) {
		if (!alive || !state.isAlive()) return;
		pendingEntityHit = snapshot.findClosestHit(
				previousPosition, state.position(), getHitPadding(),
				shooterUuid, config.allowFriendlyFire());
	}

	public double getHitPadding() {
		return Math.max(0.02, config.caliber() * 2.0);
	}

	public PendingHit takePendingEntityHit() {
		PendingHit h = pendingEntityHit;
		pendingEntityHit = null;
		return h;
	}

	public BlockHitResult clipBlocks() {
		if (!alive || !state.isAlive()) {
			return BlockHitResult.miss(state.position(), null, BlockPos.containing(state.position()));
		}
		return level.clip(new ClipContext(
				previousPosition, state.position(),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getDummyEntity(level)));
	}

	public boolean applyEntityHit(PendingHit hit) {
		double speed = state.velocity().length();
		float damage = (float) (0.5 * config.mass() * speed * speed * config.damageMultiplier());

		BulletEntityHitEvent event = new BulletEntityHitEvent(this, hit.entity(), hit.hitPos(), damage);
		AsLib.EVENT_BUS.post(event);
		if (event.isCancelled()) return false;

		Entity target = event.getTarget();
		if (target == null || event.getDamage() <= 0) return false;

		Entity shooter = getShooterUuid() != null ? level.getEntity(getShooterUuid()) : null;

		DamageSource source;
		if (shooter instanceof ServerPlayer player) {
			source = level.damageSources().playerAttack(player);
		} else if (shooter instanceof LivingEntity mob) {
			source = level.damageSources().mobAttack(mob);
		} else {
			source = level.damageSources().generic();
		}

		target.invulnerableTime = 0;
		boolean hurt = target.hurt(source, event.getDamage());

		if (hurt && target instanceof LivingEntity livingTarget && shooter instanceof LivingEntity livingShooter) {
			livingTarget.setLastHurtByMob(livingShooter);

			if (target instanceof Mob mobTarget) {
				mobTarget.setTarget(livingShooter);
			}

			if (target instanceof Animal animal) {
				animal.setLastHurtByMob(livingShooter);
			}
		}

		if (BulletManager.DEBUG) {
			System.out.println("[BULLET] hurt " + target.getType()
					+ " by " + (shooter != null ? shooter.getType() : "unknown")
					+ " dmg=" + event.getDamage() + " ok=" + hurt);
		}
		alive = false;
		return true;
	}

	public boolean applyBlockHit(BlockHitResult hitResult) {
		double speed = state.velocity().length();
		double energy = 0.5 * config.mass() * speed * speed;
		Material material = getMaterial(level.getBlockState(hitResult.getBlockPos()));
		Vec3 hitLocation = hitResult.getLocation();

		BulletBlockHitEvent event = new BulletBlockHitEvent(this, hitResult, material, energy);
		AsLib.EVENT_BUS.post(event);
		if (event.isCancelled()) return false;

		Vec3 normal = new Vec3(
				hitResult.getDirection().getStepX(),
				hitResult.getDirection().getStepY(),
				hitResult.getDirection().getStepZ());

		Vec3 velNorm = state.velocity().normalize();
		double angleOfIncidence = Math.toDegrees(Math.acos(Math.abs(velNorm.dot(normal))));

		double ricochetChance = 0.0;
		if (angleOfIncidence > 45.0) {
			ricochetChance = ((angleOfIncidence - 45.0) / 45.0) * (event.getMaterial().hardness() / 10.0);
		}

		if (Math.random() < ricochetChance) {
			Vec3 reflected = state.velocity().subtract(normal.scale(2 * state.velocity().dot(normal)));
			double energyLoss = 0.4 + (Math.random() * 0.3);
			Vec3 newPos = hitLocation.add(reflected.normalize().scale(0.1));
			this.state = new BulletState(newPos, reflected.scale(1.0 - energyLoss), state.distanceTraveled(), true);
			this.previousPosition = newPos;
			if (BulletManager.DEBUG) BulletDebug.renderRicochet(level, hitLocation);
			return false;
		}

		if (energy > event.getMaterial().maxPenetrationJoules()) {
			level.destroyBlock(hitResult.getBlockPos(), true);
			double newSpeed = speed * (1.0 - event.getMaterial().density() * 0.15);
			if (BulletManager.DEBUG) BulletDebug.renderPenetration(level, hitLocation);
			if (newSpeed < 1.0) { alive = false; return true; }
			Vec3 newPos = hitLocation.add(state.velocity().normalize().scale(0.1));
			this.state = new BulletState(newPos, state.velocity().normalize().scale(newSpeed), state.distanceTraveled(), true);
			this.previousPosition = newPos;
			return false;
		}

		if (isSimplified()) {
			level.playSound(null, hitLocation.x, hitLocation.y, hitLocation.z,
					SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.25f, 1.8f);
			level.sendParticles(ParticleTypes.SMOKE,
					hitLocation.x, hitLocation.y, hitLocation.z, 2, 0.1, 0.1, 0.1, 0.02);
		}

		alive = false;
		return true;
	}

	private Material getMaterial(BlockState state) {
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		// TODO: подключить MaterialData из LoaderConfigs
		return new Material("unbreakable", 1, 0, Double.MAX_VALUE);
	}

	public void setLodStepsCap(int cap) { this.lodStepsCap = cap; }
	public int getLodStepsCap() { return lodStepsCap; }
	public long getId() { return id; }
	public ServerLevel getLevel() { return level; }
	public BulletConfig getConfig() { return config; }
	public UUID getShooterUuid() { return shooterUuid; }
	public boolean isAlive() { return alive && state.isAlive(); }
	public BulletState getState() { return state; }
	public Vec3 getPreviousPosition() { return previousPosition; }
	public void markDead() { this.alive = false; }
}