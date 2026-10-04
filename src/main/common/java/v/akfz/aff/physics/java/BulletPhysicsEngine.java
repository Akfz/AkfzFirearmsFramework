package v.akfz.aff.physics.java;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aff.physics.cpp.CSnapshot;
import v.akfz.aff.physics.EntitySnapshot;

import java.util.List;

public interface BulletPhysicsEngine {
	boolean isAvailable();
	String name();

	void simulate(List<Bullet> bullets,double deltaTime,Vec3 wind,
	              CSnapshot CSnapshot,EntitySnapshot ESnapshot,@Nullable ServerLevel level);
}