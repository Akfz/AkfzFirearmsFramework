package v.akfz.aff.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import v.akfz.aff.LoaderConfigs;
import v.akfz.aff.event.bullet.BulletSpawnEvent;
import v.akfz.aff.physics.EntitySnapshot;
import v.akfz.aff.physics.cpp.CSnapshot;
import v.akfz.aff.physics.cpp.NativeBallistics;
import v.akfz.aff.physics.java.BulletPhysicsEngine;
import v.akfz.aff.physics.java.impl.JavaBulletPhysics;
import v.akfz.aff.physics.java.impl.NativeBulletPhysics;
import v.akfz.aff.world.EnvironmentSystem;
import v.akfz.aff.world.Material;
import v.akfz.aslib.AsLib;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Heart of mod
 */
public final class BulletManager {

	@FunctionalInterface
	public interface BulletFactory {
		Bullet create(long id, ServerLevel level, BulletConfig config,
		              BulletState state, UUID shooterUuid, int shooterEntityId);
	}

	private static final BulletManager INSTANCE = new BulletManager();

	public static boolean DEBUG        = false;
	public static boolean RENDER_DEBUG = false;

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
	private final CSnapshot physicsSnapshot = new CSnapshot();

	private MinecraftServer server;
	private long lastUpdateTime = 0;
	private BulletPhysicsEngine physicsEngine;

	private BulletManager() {
		if (new NativeBulletPhysics().isAvailable()) {
			setPhysicsEngine(new NativeBulletPhysics());
		} else {
			setPhysicsEngine(new JavaBulletPhysics());
		}
	}

	public static BulletManager getInstance() { return INSTANCE; }

	public void setPhysicsEngine(BulletPhysicsEngine engine) {
		if (physicsEngine instanceof NativeBulletPhysics) {
			NativeBallistics.discard();
		}
		this.physicsEngine = Objects.requireNonNull(engine, "Physics engine cannot be null");
		System.out.println("[AFF] Physics engine changed to: " + engine.name());
	}

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

	public void spawnBullet(ServerLevel level, Vec3 position, Vec3 velocity,
	                        BulletConfig config, UUID shooterUuid, int shooterEntityId) {
		spawnBullet(level, position, velocity, config, shooterUuid, shooterEntityId, Bullet::new);
	}

	public void spawnBullet(ServerLevel level, Vec3 position, Vec3 velocity,
	                        BulletConfig config, UUID shooterUuid, int shooterEntityId,
	                        BulletFactory factory) {
		BulletSpawnEvent spawnEvent = new BulletSpawnEvent(level, position, velocity, config, shooterUuid);
		AsLib.EVENT_BUS.post(spawnEvent);
		if (spawnEvent.isCancelled()) return;

		long id = bulletIdCounter.incrementAndGet();
		Bullet bullet = factory.create(
				id, level, config,
				new BulletState(position, velocity, 0.0, true),
				shooterUuid, shooterEntityId
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
		final double finalDt = Math.min(dt, 0.05);

		activeBullets.entrySet().removeIf(entry -> {
			Bullet b = entry.getValue();
			if (!b.isAlive()) return true;

			BlockPos pos = BlockPos.containing(b.getState().position());
			if (!b.getLevel().hasChunkAt(pos)) {
				if (DEBUG) {
					System.out.println("[AFF_DEBUG] Removing bullet " + b.getId()
							+ " - out of loaded chunks");
				}
				b.markDead();
				return true;
			}
			return false;
		});

		if (activeBullets.isEmpty()) {
			scheduleNextCycle();
			return;
		}

		List<Bullet> standardBullets = new ArrayList<>();
		List<Bullet> customBullets   = new ArrayList<>();
		for (Bullet b : activeBullets.values()) {
			if (b.requiresCustomPhysics()) customBullets.add(b);
			else                           standardBullets.add(b);
		}

		Map<ResourceKey<Level>, List<Bullet>> bulletsByLevel = new HashMap<>();
		for (Bullet b : standardBullets) {
			bulletsByLevel
					.computeIfAbsent(b.getLevel().dimension(), k -> new ArrayList<>())
					.add(b);
		}

		CompletableFuture.runAsync(() -> {

			try {
				for (Map.Entry<ResourceKey<Level>,List<Bullet>> entry: bulletsByLevel.entrySet()) {
					List<Bullet> levelBullets = entry.getValue();
					if (levelBullets.isEmpty()) continue;

					ServerLevel level = levelBullets.get(0).getLevel();
					EntitySnapshot entitySnap = snapshots.get(entry.getKey());
					if (entitySnap == null) {
						if (DEBUG) {
							System.out.println("[AFF_DEBUG] No entity snapshot for " + entry.getKey() + ", skipping " + levelBullets.size() + " bullets this cycle");
						}
						continue;
					}

					CSnapshot snapshot = physicsSnapshot;
					snapshot.reset();
					snapshot.isDebug   = DEBUG;
					snapshot.deltaTime = finalDt;

					Map<Material,Integer> snapshotMatIds = new IdentityHashMap<>();

					for (Bullet b: levelBullets) {
						var s = b.getState();
						var c = b.getConfig();

						snapshot.addBullet((float) s.position().x,(float) s.position().y,(float) s.position().z,(float) s.velocity().x,(float) s.velocity().y,(float) s.velocity().z);

						snapshot.addConfig(c,b.getShooterEntityId());

						Vec3 bWind = EnvironmentSystem.windAt(level,s.position());
						boolean bEnclosed = EnvironmentSystem.isEnclosed(level,s.position());

						BlockPos bPos = BlockPos.containing(s.position());
						BlockState bState = level.getBlockState(bPos);
						boolean inWater = bState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);

						Vec3 actualWind = inWater ? Vec3.ZERO : bWind;
						float fluidDrag = inWater ? 15.0f : 1.0f;

						snapshot.addWind((float) actualWind.x,(float) actualWind.y,(float) actualWind.z,bEnclosed,fluidDrag);

						collectBlocksForBullet(b,level,finalDt,snapshot,snapshotMatIds);
					}

					for (var entityEntry: entitySnap.getEntries()) {
						AABB box = entityEntry.box();
						snapshot.addEntity((float) box.minX,(float) box.minY,(float) box.minZ,(float) box.maxX,(float) box.maxY,(float) box.maxZ,entityEntry.entity().getId());
					}

					if (DEBUG) {
						System.out.println("[AFF_DEBUG] Level " + entry.getKey() + ": " + levelBullets.size() + " bullets, " + snapshot.blockCount + " blocks, " + snapshot.entityCount + " entities, " + snapshot.materialCount + " materials");
					}

					snapshot.prepareForNative();

					Vec3 wind = EnvironmentSystem.windAt(level,levelBullets.get(0).getState().position());
					physicsEngine.simulate(levelBullets,finalDt,wind,snapshot,entitySnap,level);
				}

				for (Bullet b: customBullets) {
					if (b.isAlive()) b.tickCustomPhysics(finalDt);
				}
			} catch (Throwable t) {
				System.err.println("[AFF] Physics task failed: " + t);
				t.printStackTrace();
				throw t;
			}
		}, physicsExecutor)
				.thenRunAsync(() -> {
					try {
						for (Bullet b : activeBullets.values()) {
							if (RENDER_DEBUG) {
								BulletDebug.renderTrail(b.getLevel(), b.getPreviousPosition(), b.getState().position());
								BulletDebug.renderVelocity(b.getLevel(), b.getState().position(), b.getState().velocity());
							}
							b.processPendingResults();
						}
						activeBullets.entrySet().removeIf(e -> !e.getValue().isAlive());
					} catch (Throwable t) {
						System.err.println("[AFF] Result processing failed: " + t);
						t.printStackTrace();
					}
		}, server)
				.whenComplete((v, t) -> {
					if (t != null) {
						System.err.println("[AFF] Physics cycle aborted, restarting next tick: " + t);
					}
					scheduleNextCycle();
				});
	}

	private void collectBlocksForBullet(Bullet b,
	                                    ServerLevel level,
	                                    double dt,
	                                    CSnapshot snapshot,
	                                    Map<Material, Integer> snapshotMatIds) {
		Vec3 start = b.getState().position();
		Vec3 end   = start.add(b.getState().velocity().scale(dt));

		double distance = start.distanceTo(end);
		int steps = Math.max(1, (int) Math.ceil(distance * 5.0));

		for (int step = 1; step <= steps; step++) {
			double fraction = (double) step / steps;
			Vec3 checkPos = start.add(end.subtract(start).scale(fraction));
			BlockPos bp = BlockPos.containing(checkPos);

			if (!level.hasChunkAt(bp)) continue;

			BlockState state = level.getBlockState(bp);
			if (state.isAir()) continue;

			VoxelShape shape = state.getCollisionShape(level, bp);
			if (shape.isEmpty()) continue;

			Material mat = LoaderConfigs.INSTANCE.getMaterialByState(state);

			Integer matIdBox = snapshotMatIds.get(mat);
			int matId;
			if (matIdBox == null) {
				matId = snapshot.materialCount;
				snapshot.addMaterial(mat);
				snapshotMatIds.put(mat, matId);
			} else {
				matId = matIdBox;
			}

			for (AABB local : shape.toAabbs()) {
				AABB world = local.move(bp.getX(), bp.getY(), bp.getZ());
				snapshot.addBlock(
						(float) world.minX, (float) world.minY, (float) world.minZ,
						(float) world.maxX, (float) world.maxY, (float) world.maxZ,
						matId, bp.getX(), bp.getY(), bp.getZ()
				);
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