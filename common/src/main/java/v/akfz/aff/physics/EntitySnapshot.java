package v.akfz.aff.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.PendingHit;

import java.util.*;

public final class EntitySnapshot {
	public record Entry(Entity entity, UUID uuid, AABB box) {}

	private final List<Entry> entries;
	private static final int CELL_SIZE = 4;
	private final Map<Long, List<Entry>> grid;

	private static final ThreadLocal<IdentityHashMap<Entry, Boolean>> SEEN_POOL =
			ThreadLocal.withInitial(() -> new IdentityHashMap<>(32));

	private EntitySnapshot(List<Entry> entries, Map<Long, List<Entry>> grid) {
		this.entries = entries;
		this.grid = grid;
	}

	public List<Entry> getEntries() { return entries; }

	public static EntitySnapshot capture(Iterable<Entity> entities) {
		List<Entry> list = new ArrayList<>();
		Map<Long, List<Entry>> grid = new HashMap<>();

		for (Entity e : entities) {
			if (!e.isAlive() || e.isSpectator()) continue;
			if (e instanceof ItemEntity || e instanceof ExperienceOrb) continue;
			if (e instanceof AbstractArrow || e instanceof Projectile) continue;

			Entry entry = new Entry(e, e.getUUID(), e.getBoundingBox());
			list.add(entry);

			AABB box = entry.box();
			int minX = floorDiv((int) box.minX, CELL_SIZE);
			int maxX = floorDiv((int) box.maxX, CELL_SIZE);
			int minY = floorDiv((int) box.minY, CELL_SIZE);
			int maxY = floorDiv((int) box.maxY, CELL_SIZE);
			int minZ = floorDiv((int) box.minZ, CELL_SIZE);
			int maxZ = floorDiv((int) box.maxZ, CELL_SIZE);

			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						grid.computeIfAbsent(cellKey(x, y, z), k -> new ArrayList<>()).add(entry);
					}
				}
			}
		}
		return new EntitySnapshot(list, grid);
	}

	/**
	 * Уменьшенный padding: 0.05 блока (5 см) + физический радиус пули.
	 * Этого достаточно для надёжного попадания, но без ложных срабатываний.
	 */
	@Nullable
	public PendingHit findClosestHit(
			double fx, double fy, double fz,
			double tx, double ty, double tz,
			double padding,
			@Nullable UUID shooter, boolean allowFriendlyFire) {

		double dx = tx - fx;
		double dy = ty - fy;
		double dz = tz - fz;

		double segLenSqr = dx * dx + dy * dy + dz * dz;
		if (segLenSqr < 1e-8) return null;

		int cMinX = floorDiv((int) (Math.min(fx, tx) - padding), CELL_SIZE);
		int cMaxX = floorDiv((int) (Math.max(fx, tx) + padding), CELL_SIZE);
		int cMinY = floorDiv((int) (Math.min(fy, ty) - padding), CELL_SIZE);
		int cMaxY = floorDiv((int) (Math.max(fy, ty) + padding), CELL_SIZE);
		int cMinZ = floorDiv((int) (Math.min(fz, tz) - padding), CELL_SIZE);
		int cMaxZ = floorDiv((int) (Math.max(fz, tz) + padding), CELL_SIZE);

		PendingHit best = null;
		double bestT = Double.MAX_VALUE;

		IdentityHashMap<Entry, Boolean> seen = SEEN_POOL.get();
		seen.clear();

		for (int x = cMinX; x <= cMaxX; x++) {
			for (int y = cMinY; y <= cMaxY; y++) {
				for (int z = cMinZ; z <= cMaxZ; z++) {
					List<Entry> cell = grid.get(cellKey(x, y, z));
					if (cell == null) continue;

					for (Entry entry : cell) {
						if (seen.put(entry, Boolean.TRUE) != null) continue;

						if (!allowFriendlyFire && shooter != null && shooter.equals(entry.uuid()))
							continue;

						AABB box = entry.box();
						double bMinX = box.minX - padding, bMaxX = box.maxX + padding;
						double bMinY = box.minY - padding, bMaxY = box.maxY + padding;
						double bMinZ = box.minZ - padding, bMaxZ = box.maxZ + padding;

						double t = rayAABB(fx, fy, fz, dx, dy, dz,
								bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ);

						if (t >= 0.0 && t <= 1.0 && t < bestT) {
							bestT = t;
							best = new PendingHit(
									entry.entity(),
									new Vec3(fx + dx * t, fy + dy * t, fz + dz * t),
									t * t * segLenSqr
							);
						}
					}
				}
			}
		}
		return best;
	}

	private static double rayAABB(
			double ox, double oy, double oz,
			double dx, double dy, double dz,
			double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {

		double tmin = Double.NEGATIVE_INFINITY;
		double tmax = Double.POSITIVE_INFINITY;

		if (Math.abs(dx) > 1e-10) {
			double t1 = (minX - ox) / dx;
			double t2 = (maxX - ox) / dx;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (ox < minX || ox > maxX) return -1;
		}

		if (Math.abs(dy) > 1e-10) {
			double t1 = (minY - oy) / dy;
			double t2 = (maxY - oy) / dy;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (oy < minY || oy > maxY) return -1;
		}

		if (Math.abs(dz) > 1e-10) {
			double t1 = (minZ - oz) / dz;
			double t2 = (maxZ - oz) / dz;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (oz < minZ || oz > maxZ) return -1;
		}

		if (tmax < 0) return -1;
		return Math.max(tmin, 0.0);
	}

	public List<Entry> entriesIn(AABB box) {
		List<Entry> result = new ArrayList<>();
		for (Entry e : entries) {
			if (e.box().intersects(box)) result.add(e);
		}
		return result;
	}

	private static long cellKey(int x, int y, int z) {
		return ((long) x << 40) | ((long) y << 20) | (z & 0xFFFFFL);
	}

	private static int floorDiv(int a, int b) {
		return Math.floorDiv(a, b);
	}
}