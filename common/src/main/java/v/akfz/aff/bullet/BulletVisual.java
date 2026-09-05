package v.akfz.aff.bullet;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Calling from server, so you can just send packets
 */
public class BulletVisual {

	public void onSpawn(ServerLevel level, Vec3 position, Vec3 velocity) {
	}

	public void onBlockHit(ServerLevel level, Vec3 hitPos, int materialId) {
		send(level, ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
				10, 0.1, 0.1, 0.1, 0.2);
	}

	public void onEntityHit(ServerLevel level, Vec3 hitPos, int entityId) {
		send(level, ParticleTypes.DAMAGE_INDICATOR, hitPos.x, hitPos.y, hitPos.z,
				20, 0.15, 0.15, 0.15, 0.3);
	}

	public void onRicochet(ServerLevel level, Vec3 hitPos) {
		send(level, ParticleTypes.ENCHANTED_HIT, hitPos.x, hitPos.y, hitPos.z,
				15, 0.2, 0.2, 0.2, 0.4);
	}

	public void onPenetration(ServerLevel level, Vec3 hitPos) {
		send(level, ParticleTypes.LARGE_SMOKE, hitPos.x, hitPos.y, hitPos.z,
				5, 0.1, 0.1, 0.1, 0.05);
	}

	public void onDestroyed(ServerLevel level, Vec3 position) {
		send(level, ParticleTypes.EXPLOSION, position.x, position.y, position.z,
				2, 0.1, 0.1, 0.1, 0.0);
	}

	public void onOutOfBounds(ServerLevel level, Vec3 position) {
	}

	protected void send(ServerLevel level, ParticleOptions particle,
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