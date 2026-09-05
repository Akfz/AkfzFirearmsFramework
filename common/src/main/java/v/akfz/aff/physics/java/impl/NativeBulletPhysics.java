package v.akfz.aff.physics.java.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.Bullet;
import v.akfz.aff.physics.EntitySnapshot;
import v.akfz.aff.physics.cpp.NativeBallistics;
import v.akfz.aff.physics.cpp.NativeBallisticsLoader;
import v.akfz.aff.physics.cpp.NativePhysics;
import v.akfz.aff.physics.cpp.CSnapshot;
import v.akfz.aff.physics.java.BulletPhysicsEngine;

import java.util.List;

public class NativeBulletPhysics implements BulletPhysicsEngine, NativePhysics {

	@Override
	public boolean isAvailable() {
		return NativeBallisticsLoader.isLoaded();
	}

	@Override
	public String name() {
		return "Def_CPP";
	}

	@Override
	public void simulate(List<Bullet> bullets, double deltaTime, Vec3 wind, CSnapshot snapshot, EntitySnapshot worldSnapshot, @Nullable ServerLevel level) {
		if (bullets.isEmpty()) return;

		NativeBallistics.simulateFrame(snapshot);

		snapshot.bulletBuf.rewind();
		snapshot.resultBuf.rewind();

		for (Bullet b : bullets) {
			float nx = snapshot.bulletBuf.getFloat();
			float ny = snapshot.bulletBuf.getFloat();
			float nz = snapshot.bulletBuf.getFloat();
			float nvx = snapshot.bulletBuf.getFloat();
			float nvy = snapshot.bulletBuf.getFloat();
			float nvz = snapshot.bulletBuf.getFloat();

			b.updateStateFromNative(nx, ny, nz, nvx, nvy, nvz);

			int status = snapshot.resultBuf.getInt();
			float hx = snapshot.resultBuf.getFloat();
			float hy = snapshot.resultBuf.getFloat();
			float hz = snapshot.resultBuf.getFloat();
			float extra = snapshot.resultBuf.getFloat();

			int penCount = snapshot.resultBuf.getInt();

			float[] penX = new float[16];
			float[] penY = new float[16];
			float[] penZ = new float[16];
			int[] penMatId = new int[16];
			int[] penBX = new int[16];
			int[] penBY = new int[16];
			int[] penBZ = new int[16];

			for (int p = 0; p < 16; p++) penX[p] = snapshot.resultBuf.getFloat();
			for (int p = 0; p < 16; p++) penY[p] = snapshot.resultBuf.getFloat();
			for (int p = 0; p < 16; p++) penZ[p] = snapshot.resultBuf.getFloat();
			for (int p = 0; p < 16; p++) penMatId[p] = snapshot.resultBuf.getInt();
			for (int p = 0; p < 16; p++) penBX[p] = snapshot.resultBuf.getInt();
			for (int p = 0; p < 16; p++) penBY[p] = snapshot.resultBuf.getInt();
			for (int p = 0; p < 16; p++) penBZ[p] = snapshot.resultBuf.getInt();

			if (status != 0) {
				b.recordNativeResult(status, hx, hy, hz, extra, penCount, penX, penY, penZ, penMatId, penBX, penBY, penBZ);
			}
		}
	}

	@Override
	public void discard() {
		NativeBallistics.discard();
	}
}