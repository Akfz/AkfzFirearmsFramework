package v.akfz.aff.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import v.akfz.aff.LoaderConfigs;
import v.akfz.aff.event.bullet.BulletSpawnEvent;
import v.akfz.aff.physics.cpp.CSnapshot;
import v.akfz.aff.physics.cpp.NativeBallistics;
import v.akfz.aff.physics.EntitySnapshot;
import v.akfz.aff.physics.java.BulletPhysicsEngine;
import v.akfz.aff.physics.java.impl.JavaBulletPhysics;
import v.akfz.aff.physics.java.impl.NativeBulletPhysics;
import v.akfz.aff.world.EnvironmentSystem;
import v.akfz.aff.world.Material;
import v.akfz.aslib.AsLib;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BulletManager {
	private static final BulletManager INSTANCE = new BulletManager();
	public static boolean DEBUG = true;

	private static final long CYCLE_DELAY_MS = 25;

	private final ExecutorService physicsExecutor = Executors.newFixedThreadPool(
			Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 1)),
			r -> { Thread t = new Thread(r, "AFF-BulletPhysics"); t.setDaemon(true); return t; }
	);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "AFF-BulletScheduler");
		t.setDaemon(true);
		return t;
	});

	private volatile boolean isRunning = false;
	private final Map<Long, Bullet> activeBullets = new ConcurrentHashMap<>();
	private final AtomicLong bulletIdCounter = new AtomicLong(0);
	private final Map<ResourceKey<Level>, EntitySnapshot> snapshots = new ConcurrentHashMap<>();
	private final Map<ResourceKey<Level>, Long> snapshotGameTime = new ConcurrentHashMap<>();

	private MinecraftServer server;
	private long lastUpdateTime = 0;
	private BulletPhysicsEngine physicsEngine;

	private final CSnapshot cSnapshot = new CSnapshot();

	private BulletManager() {
		if (new NativeBulletPhysics().isAvailable()) {
			setPhysicsEngine(new NativeBulletPhysics());
		} else {
			setPhysicsEngine(new JavaBulletPhysics());
		}
	}

	public void setPhysicsEngine(BulletPhysicsEngine engine) {
		if (physicsEngine instanceof NativeBulletPhysics) {
			NativeBallistics.discard();
		}
		this.physicsEngine = Objects.requireNonNull(engine, "Physics engine cannot be null");
		System.out.println("[AFF] Physics engine changed to: " + engine.name());
	}

	public static BulletManager getInstance() { return INSTANCE; }

	public void start(MinecraftServer server) {
		if (isRunning) return;
		this.server = server;
		isRunning = true;
		scheduler.schedule(this::physicsCycle, CYCLE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		isRunning = false;
		scheduler.shutdown();
		physicsExecutor.shutdown();
		activeBullets.clear();
		snapshots.clear();
		snapshotGameTime.clear();

		if (physicsEngine instanceof NativeBulletPhysics) {
			NativeBallistics.discard();
		}
	}

	public void spawnBullet(ServerLevel level, Vec3 position, Vec3 velocity, BulletConfig config, UUID shooterUuid) {
		BulletSpawnEvent spawnEvent = new BulletSpawnEvent(level, position, velocity, config, shooterUuid);
		AsLib.EVENT_BUS.post(spawnEvent);
		if (spawnEvent.isCancelled()) return;

		Bullet bullet = new Bullet(
				bulletIdCounter.incrementAndGet(), level, config,
				new BulletState(position, velocity, 0.0, true), shooterUuid
		);
		activeBullets.put(bullet.getId(), bullet);
	}

	private void physicsCycle() {
		if (!isRunning || server == null || activeBullets.isEmpty()) {
			scheduleNextCycle();
			return;
		}
		long now = System.currentTimeMillis();
		double dt = lastUpdateTime > 0 ? (now - lastUpdateTime) / 1000.0 : 0.025;
		lastUpdateTime = now;
		dt = Math.min(dt, 0.05);
		final double finalDt = dt;

		List<Bullet> standardBullets = new ArrayList<>();
		List<Bullet> customBullets = new ArrayList<>();

		activeBullets.entrySet().removeIf(entry -> {
			Bullet b = entry.getValue();
			if (!b.isAlive()) return true;

			BlockPos pos = BlockPos.containing(b.getState().position());
			if (!b.getLevel().hasChunkAt(pos)) {
				if (DEBUG) {
					System.out.println("[AFF_DEBUG] Removing bullet " + b.getId() + " - out of loaded chunks");
				}
				b.markDead();
				return true;
			}
			return false;
		});

		for (Bullet b : activeBullets.values()) {
			if (b.requiresCustomPhysics()) customBullets.add(b);
			else standardBullets.add(b);
		}

		Map<ResourceKey<Level>, List<Bullet>> bulletsByLevel = new HashMap<>();
		for (Bullet b : standardBullets) {
			bulletsByLevel.computeIfAbsent(b.getLevel().dimension(), k -> new ArrayList<>()).add(b);
		}

		CompletableFuture.runAsync(() -> {
			cSnapshot.reset();
			cSnapshot.isDebug = DEBUG;
			Map<BlockState, Integer> blockStateMaterialCache = new HashMap<>();

			for (Map.Entry<ResourceKey<Level>, List<Bullet>> entry : bulletsByLevel.entrySet()) {
				ResourceKey<Level> dim = entry.getKey();
				List<Bullet> levelBullets = entry.getValue();

				ServerLevel level = null;
				for (Bullet b : levelBullets) {
					if (b.getLevel().dimension() == dim) {
						level = b.getLevel();
						break;
					}
				}
				if (level == null) continue;

				EntitySnapshot entitySnap = snapshots.get(dim);
				if (entitySnap == null) continue;

				for (Bullet b : levelBullets) {
					var s = b.getState();
					var c = b.getConfig();

					cSnapshot.addBullet(
							(float) s.position().x, (float) s.position().y, (float) s.position().z,
							(float) s.velocity().x, (float) s.velocity().y, (float) s.velocity().z
					);

					int shooterId = -1;
					if (b.getShooterUuid() != null) {
						Entity shooter = level.getEntity(b.getShooterUuid());
						if (shooter != null) {
							shooterId = shooter.getId();
						}
					}

					cSnapshot.addConfig(c, shooterId);

					Vec3 bWind = EnvironmentSystem.windAt(level, s.position());
					boolean bEnclosed = EnvironmentSystem.isEnclosed(level, s.position());

					BlockPos bPos = BlockPos.containing(s.position());
					BlockState bState = level.getBlockState(bPos);
					boolean inWater = bState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);

					Vec3 actualWind = bWind;
					float fluidDrag = 1.0f;

					if (inWater) {
						actualWind = Vec3.ZERO;
						fluidDrag = 15.0f;
					}

					cSnapshot.addWind((float) actualWind.x, (float) actualWind.y, (float) actualWind.z, bEnclosed, fluidDrag);
					collectBlocksForBullet(b, level, finalDt, cSnapshot, blockStateMaterialCache);
				}

				if (BulletManager.DEBUG) {
					System.out.println("[AFF_DEBUG] Level " + dim + ": " +
							levelBullets.size() + " bullets, " +
							cSnapshot.blockCount + " blocks, " +
							cSnapshot.entityCount + " entities");
				}

				for (var entityEntry : entitySnap.getEntries()) {
					AABB box = entityEntry.box();
					cSnapshot.addEntity(
							(float) box.minX, (float) box.minY, (float) box.minZ,
							(float) box.maxX, (float) box.maxY, (float) box.maxZ,
							entityEntry.entity().getId()
					);
				}

				cSnapshot.deltaTime = finalDt;
				cSnapshot.prepareForNative();

				Vec3 wind = EnvironmentSystem.windAt(level, levelBullets.get(0).getState().position());
				physicsEngine.simulate(levelBullets, finalDt, wind, cSnapshot, entitySnap, level);
			}

			for (Bullet b : customBullets) {
				if (b.isAlive()) {
					b.tickCustomPhysics(finalDt);
				}
			}
		}, physicsExecutor).thenRunAsync(() -> {
			for (Bullet b : activeBullets.values()) {
				if (DEBUG) {
					BulletDebug.renderTrail(b.getLevel(), b.getPreviousPosition(), b.getState().position());
					BulletDebug.renderVelocity(b.getLevel(), b.getState().position(), b.getState().velocity());
					Vec3 wind = EnvironmentSystem.windAt(b.getLevel(), b.getState().position());
					BulletDebug.renderWind(b.getLevel(), b.getState().position(), wind);
				}
				b.processPendingResults();
			}
			activeBullets.entrySet().removeIf(e -> !e.getValue().isAlive());
			scheduleNextCycle();
		}, server);
	}

	private void collectBlocksForBullet(Bullet b, ServerLevel level, double dt, CSnapshot snapshot, Map<BlockState, Integer> materialCache) {
		Vec3 start = b.getState().position();
		Vec3 end = start.add(b.getState().velocity().scale(dt));

		double distance = start.distanceTo(end);

		int steps = Math.max(1, (int) Math.ceil(distance * 5.0));

		for (int step = 1; step <= steps; step++) {
			double fraction = (double) step / steps;
			Vec3 checkPos = start.add(end.subtract(start).scale(fraction));
			BlockPos bp = BlockPos.containing(checkPos);

			if (level.hasChunkAt(bp)) {
				BlockState state = level.getBlockState(bp);
				if (!state.isAir()) {
					VoxelShape shape = state.getCollisionShape(level, bp);
					if (!shape.isEmpty()) {
						int matId = materialCache.computeIfAbsent(state, bs -> {
							Material mat = b.getMaterial(bs);
							int id = snapshot.materialCount;
							snapshot.addMaterial(mat);
							return id;
						});

						for (AABB local : shape.toAabbs()) {
							AABB world = local.move(bp.getX(), bp.getY(), bp.getZ());
							snapshot.addBlock((float) world.minX, (float) world.minY, (float) world.minZ,
									(float) world.maxX, (float) world.maxY, (float) world.maxZ,
									matId, bp.getX(), bp.getY(), bp.getZ());
						}
					}
				}
			}
		}
	}

	private void scheduleNextCycle() {
		if (isRunning) {
			scheduler.schedule(this::physicsCycle, CYCLE_DELAY_MS, TimeUnit.MILLISECONDS);
		}
	}

	public long getNextBulletId() {
		return bulletIdCounter.incrementAndGet();
	}

	public void updateSnapshot(ServerLevel level) {
		ResourceKey<Level> key = level.dimension();
		long gameTime = level.getGameTime();
		if (snapshotGameTime.getOrDefault(key, -1L) != gameTime) {
			EntitySnapshot newSnapshot = EntitySnapshot.capture(level.getAllEntities());
			snapshots.put(key, newSnapshot);
			snapshotGameTime.put(key, gameTime);
		}
	}
}