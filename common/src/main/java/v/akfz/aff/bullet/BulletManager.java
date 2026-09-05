package v.akfz.aff.bullet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.event.bullet.BulletSpawnEvent;
import v.akfz.aff.world.EnvironmentSystem;
import v.akfz.aslib.AsLib;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BulletManager {
	private static final BulletManager INSTANCE = new BulletManager();
	public static boolean DEBUG = false; // если сильно хочется

	private static final long CYCLE_DELAY_MS = 25;

	private final ExecutorService physicsExecutor = Executors.newFixedThreadPool(
			Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 1)),
			r -> { Thread t = new Thread(r, "AFF-BulletPhysics"); t.setDaemon(true); return t; });

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "AFF-BulletScheduler"); t.setDaemon(true); return t; });

	private volatile boolean isRunning = false;

	private final Map<Long, Bullet> activeBullets = new ConcurrentHashMap<>();
	private final AtomicLong bulletIdCounter = new AtomicLong(0);

	private final Map<ResourceKey<Level>, EntitySnapshot> snapshots = new ConcurrentHashMap<>();
	private final Map<ResourceKey<Level>, Long> snapshotGameTime = new ConcurrentHashMap<>();

	private MinecraftServer server;
	private long lastUpdateTime = 0;

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
		EnvironmentSystem.clearCaches();
	}

	public long getNextBulletId() {
		return bulletIdCounter.incrementAndGet();
	}

	public void spawnBullet(ServerLevel level, Vec3 position, Vec3 velocity,
	                        BulletConfig config, UUID shooterUuid) {
		BulletSpawnEvent spawnEvent = new BulletSpawnEvent(level, position, velocity, config, shooterUuid);
		AsLib.EVENT_BUS.post(spawnEvent);
		if (spawnEvent.isCancelled()) return;

		long id = bulletIdCounter.get();
		Bullet bullet = new Bullet(id, spawnEvent.getLevel(), spawnEvent.getConfig(),
				new BulletState(spawnEvent.getPosition(), spawnEvent.getVelocity(), 0.0, true),
				spawnEvent.getShooterUuid());
		activeBullets.put(id, bullet);
	}
	private void physicsCycle() {
		if (!isRunning) return;

		if (server == null || activeBullets.isEmpty()) {
			scheduleNextCycle();
			return;
		}

		long now = System.currentTimeMillis();
		double dt = lastUpdateTime > 0 ? (now - lastUpdateTime) / 1000.0 : 0.025;
		lastUpdateTime = now;
		dt = Math.min(dt, 0.1);
		final double deltaTime = dt;

		server.execute(() -> prepareAndDispatch(deltaTime));
	}

	private void prepareAndDispatch(double dt) {
		if (activeBullets.isEmpty()) {
			scheduleNextCycle();
			return;
		}
		long now = System.currentTimeMillis();

		Map<ServerLevel, List<Vec3>> playersCache = new HashMap<>();

		Iterator<Map.Entry<Long, Bullet>> it = activeBullets.entrySet().iterator();
		while (it.hasNext()) {
			Bullet bullet = it.next().getValue();
			ServerLevel level = bullet.getLevel();
			Vec3 pos = bullet.getState().position();

			if (!level.isLoaded(BlockPos.containing(pos))) {
				it.remove();
				continue;
			}

			List<Vec3> players = playersCache.computeIfAbsent(level, l -> {
				List<Vec3> ps = new ArrayList<>();
				for (ServerPlayer p : l.players()) ps.add(p.position());
				return ps;
			});
			double dist = nearestPlayerDist(players, pos);

			if (dist > Bullet.FAR_DISTANCE) {
				if (bullet.shouldCullFar(snapshotFor(level), now)) {
					it.remove();
					continue;
				}
			}

			bullet.setLodStepsCap((int) Math.max(1, bullet.getConfig().maxSubSteps() - dist / 10));
		}

		if (activeBullets.isEmpty()) {
			scheduleNextCycle();
			return;
		}

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Bullet bullet : activeBullets.values()) {
			ServerLevel level = bullet.getLevel();
			EntitySnapshot snapshot = snapshotFor(level);
			Vec3 wind = EnvironmentSystem.windAt(level, bullet.getState().position());

			futures.add(CompletableFuture.runAsync(() -> {
				try {
					bullet.simulatePhysics(dt, wind, bullet.getLodStepsCap());
					bullet.checkEntityHits(snapshot);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, physicsExecutor));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.thenRunAsync(() -> {
					try {
						applyPhase();
					} finally {
						scheduleNextCycle();
					}
				}, server);
	}

	private static double nearestPlayerDist(List<Vec3> players, Vec3 pos) {
		double best = Double.MAX_VALUE;
		for (Vec3 p : players) {
			double d = p.distanceToSqr(pos);
			if (d < best) best = d;
		}
		return best == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(best);
	}

	private void applyPhase() {
		Iterator<Map.Entry<Long, Bullet>> it = activeBullets.entrySet().iterator();
		while (it.hasNext()) {
			Bullet bullet = it.next().getValue();

			Vec3 prev = bullet.getPreviousPosition();
			Vec3 pos = bullet.getState().position();
			ServerLevel level = bullet.getLevel();

			PendingHit entityHit = bullet.takePendingEntityHit();
			BlockHitResult blockHit = bullet.clipBlocks();

			if (DEBUG) {
				BulletDebug.renderTrail(level, prev, pos);
				BulletDebug.renderHitbox(level, prev, pos, bullet.getHitPadding());
				BulletDebug.renderVelocity(level, pos, bullet.getState().velocity());
			}

			double entityDist = entityHit != null ? entityHit.distSqr() : Double.MAX_VALUE;
			double blockDist = blockHit.getType() != HitResult.Type.MISS
					? prev.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

			boolean destroyed;
			if (entityHit != null && entityDist <= blockDist) {
				if (DEBUG) {
					BulletDebug.renderHitPoint(level, entityHit.hitPos(), true);
					BulletDebug.renderEntityBox(level, entityHit.entity().getBoundingBox());
				}
				destroyed = bullet.applyEntityHit(entityHit);
			} else if (blockDist != Double.MAX_VALUE) {
				if (DEBUG) {
					BulletDebug.renderHitPoint(level, blockHit.getLocation(), false);
				}
				destroyed = bullet.applyBlockHit(blockHit);
			} else {
				destroyed = false;
			}

			if (destroyed || !bullet.isAlive()) it.remove();
		}
	}

	private void scheduleNextCycle() {
		if (isRunning) {
			scheduler.schedule(this::physicsCycle, CYCLE_DELAY_MS, TimeUnit.MILLISECONDS);
		}
	}

	private EntitySnapshot snapshotFor(ServerLevel level) {
		ResourceKey<Level> key = level.dimension();
		long gameTime = level.getGameTime();
		if (snapshotGameTime.getOrDefault(key, -1L) != gameTime) {
			snapshots.put(key, EntitySnapshot.capture(level.getAllEntities()));
			snapshotGameTime.put(key, gameTime);
		}
		return snapshots.get(key);
	}
}