package v.akfz.aff.event.bullet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.bullet.BulletConfig;
import v.akfz.aslib.event.api.Cancellable;
import v.akfz.aslib.event.api.Event;
import java.util.UUID;

public class BulletSpawnEvent extends Event implements Cancellable {
	private ServerLevel level;
	private Vec3 position;
	private Vec3 velocity;
	private BulletConfig config;
	private UUID shooterUuid;
	private boolean cancelled = false;

	public BulletSpawnEvent(ServerLevel level, Vec3 position, Vec3 velocity, BulletConfig config, UUID shooterUuid) {
		this.level = level;
		this.position = position;
		this.velocity = velocity;
		this.config = config;
		this.shooterUuid = shooterUuid;
	}

	@Override
	public boolean isCancelled() { return cancelled; }
	@Override
	public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

	public ServerLevel getLevel() { return level; }
	public void setLevel(ServerLevel level) { this.level = level; }
	public Vec3 getPosition() { return position; }
	public void setPosition(Vec3 position) { this.position = position; }
	public Vec3 getVelocity() { return velocity; }
	public void setVelocity(Vec3 velocity) { this.velocity = velocity; }
	public BulletConfig getConfig() { return config; }
	public void setConfig(BulletConfig config) { this.config = config; }
	public UUID getShooterUuid() { return shooterUuid; }
	public void setShooterUuid(UUID shooterUuid) { this.shooterUuid = shooterUuid; }
}