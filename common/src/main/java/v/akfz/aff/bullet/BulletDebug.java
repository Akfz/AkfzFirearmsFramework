package v.akfz.aff.bullet;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BulletDebug {

	private BulletDebug() {}

	public static void renderTrail(ServerLevel level, Vec3 prev, Vec3 pos) {
		double distance = prev.distanceTo(pos);
		if (distance < 0.05) return;

		int count = Math.max(2, (int) (distance * 3));
		count = Math.min(count, 25);

		Vec3 step = pos.subtract(prev).scale(1.0 / count);
		for (int i = 0; i < count; i++) {
			Vec3 p = prev.add(step.scale(i));
			send(level, ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		send(level, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
	}

	public static void renderHitbox(ServerLevel level, Vec3 prev, Vec3 pos, double padding) {
		AABB box = new AABB(prev, pos).inflate(padding);
		renderBox(level, box, ParticleTypes.HAPPY_VILLAGER, 6);
	}

	public static void renderEntityBox(ServerLevel level, AABB box) {
		renderBox(level, box, ParticleTypes.SMOKE, 6);
	}

	public static void renderVelocity(ServerLevel level, Vec3 pos, Vec3 velocity) {
		double speed = velocity.length();
		if (speed < 0.1) return;
		Vec3 end = pos.add(velocity.normalize().scale(Math.min(2.0, speed / 150.0)));
		drawLine(level, pos, end, ParticleTypes.FLAME, 4);
	}

	public static void renderWind(ServerLevel level, Vec3 pos, Vec3 wind) {
		if (wind.lengthSqr() < 1e-6) return;

		double speed = wind.length();
		double visualLength = Math.min(5.0, speed * 0.2);

		Vec3 windDir = wind.normalize();
		Vec3 endPos = pos.add(windDir.scale(visualLength));

		drawLine(level, pos, endPos, ParticleTypes.CLOUD, 12);

		send(level, ParticleTypes.FLAME, endPos.x, endPos.y, endPos.z, 4, 0.05, 0.05, 0.05, 0.0);
		send(level, ParticleTypes.END_ROD, endPos.x, endPos.y, endPos.z, 1, 0.0, 0.0, 0.0, 0.0);
	}

	public static void renderHitPoint(ServerLevel level, Vec3 hitPos, boolean isEntity) {
		if (isEntity) {
			send(level, ParticleTypes.DAMAGE_INDICATOR, hitPos.x, hitPos.y, hitPos.z,
					20, 0.15, 0.15, 0.15, 0.3);
		} else {
			send(level, ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
					10, 0.1, 0.1, 0.1, 0.2);
		}
	}

	public static void renderRicochet(ServerLevel level, Vec3 pos) {
		send(level, ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
				15, 0.2, 0.2, 0.2, 0.4);
	}

	public static void renderPenetration(ServerLevel level, Vec3 pos) {
		send(level, ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z,
				5, 0.1, 0.1, 0.1, 0.05);
		send(level, ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z,
				2, 0.1, 0.1, 0.1, 0.0);
	}

	private static void renderBox(ServerLevel level, AABB box, ParticleOptions particle, int densityPerBlock) {
		Vec3[] c = {
				new Vec3(box.minX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.minZ),
				new Vec3(box.minX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.minZ),
				new Vec3(box.minX, box.minY, box.maxZ),
				new Vec3(box.maxX, box.minY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.maxZ),
				new Vec3(box.maxX, box.maxY, box.maxZ),
		};

		int[][] edges = {
				{0, 1}, {2, 3}, {4, 5}, {6, 7}, // X
				{0, 2}, {1, 3}, {4, 6}, {5, 7}, // Y
				{0, 4}, {1, 5}, {2, 6}, {3, 7}, // Z
		};

		for (int[] e : edges) {
			drawLine(level, c[e[0]], c[e[1]], particle, densityPerBlock);
		}
	}

	private static void drawLine(ServerLevel level, Vec3 a, Vec3 b,
	                             ParticleOptions particle, int densityPerBlock) {
		double dist = a.distanceTo(b);
		int count = Math.max(2, (int) (dist * densityPerBlock));
		Vec3 step = b.subtract(a).scale(1.0 / count);
		for (int i = 0; i <= count; i++) {
			Vec3 p = a.add(step.scale(i));
			send(level, particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	private static void send(ServerLevel level, ParticleOptions particle,
	                         double x, double y, double z,
	                         int count, double xOff, double yOff, double zOff, double speed) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
				particle, true,
				x, y, z,
				(float) xOff, (float) yOff, (float) zOff,
				(float) speed, count);
		for (ServerPlayer player : level.players()) {
			player.connection.send(packet);
		}
	}
}