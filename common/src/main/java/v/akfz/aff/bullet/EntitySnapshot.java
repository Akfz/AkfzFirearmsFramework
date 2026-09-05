package v.akfz.aff.bullet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EntitySnapshot {
	public record Entry(Entity entity, UUID uuid, AABB box) {}

	private final List<Entry> entries;

	private EntitySnapshot(List<Entry> entries) {
		this.entries = entries;
	}

	public static EntitySnapshot capture(Iterable<Entity> entities) {
		List<Entry> list = new ArrayList<>();
		for (Entity e : entities) {
			if (!e.isAlive() || e.isSpectator()) continue;

			if (e instanceof ItemEntity) continue;
			if (e instanceof ExperienceOrb) continue;
			if (e instanceof AbstractArrow) continue;
			if (e instanceof Projectile) continue;

			list.add(new Entry(e, e.getUUID(), e.getBoundingBox()));
		}

		if (BulletManager.DEBUG && !list.isEmpty()) {
			System.out.println("[SNAPSHOT] captured " + list.size() + " entities");
			for (Entry entry : list) {
				System.out.println("  - " + entry.entity().getType() + " at " + entry.box().getCenter());
			}
		}

		return new EntitySnapshot(list);
	}

	@Nullable
	public PendingHit findClosestHit(Vec3 from, Vec3 to, double padding,
	                                 @Nullable UUID shooter, boolean allowFriendlyFire) {
		PendingHit bestClip = null;
		PendingHit bestContains = null;

		for (Entry entry : entries) {
			if (!allowFriendlyFire && shooter != null && shooter.equals(entry.uuid())) continue;

			AABB inflated = entry.box().inflate(padding);

			Optional<Vec3> clip = inflated.clip(from, to);
			if (clip.isPresent()) {
				double d = from.distanceToSqr(clip.get());
				if (bestClip == null || d < bestClip.distSqr()) {
					Vec3 exact = entry.box().clip(from, to).orElse(clip.get());
					bestClip = new PendingHit(entry.entity(), exact, d);
				}
			} else if (inflated.contains(from) || inflated.contains(to)) {
				Vec3 hitPos = inflated.contains(to) ? to : from;
				double d = from.distanceToSqr(hitPos);
				if (bestContains == null || d < bestContains.distSqr()) {
					bestContains = new PendingHit(entry.entity(), hitPos, d);
				}
			}
		}

		return bestClip != null ? bestClip : bestContains;
	}

	public List<Entry> entriesIn(AABB box) {
		List<Entry> result = new ArrayList<>();
		for (Entry e : entries) {
			if (e.box().intersects(box)) result.add(e);
		}
		return result;
	}
}