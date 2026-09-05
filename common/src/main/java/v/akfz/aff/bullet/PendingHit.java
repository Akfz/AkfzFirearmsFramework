package v.akfz.aff.bullet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record PendingHit(Entity entity, Vec3 hitPos, double distSqr) {}