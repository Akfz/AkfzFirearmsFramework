package v.akfz.aff.gun;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.BulletManager;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aff.data.magazine.MagazineSetting;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.feeding.IDirectFeedingSystem;
import v.akfz.aff.gun.feeding.IFeedingSystem;
import v.akfz.aff.gun.feeding.IMagazineFeedingSystem;
import v.akfz.aff.gun.feeding.impl.DirectFeedingSystem;
import v.akfz.aff.gun.feeding.impl.MagazineFeedingSystem;
import v.akfz.aff.item.AmmoItem;
import v.akfz.aff.item.MagazineItem;
import v.akfz.aff.network.SyncRecoil;
import v.akfz.aslib.network.AsLibNetworking;

import java.util.Arrays;

public class DefaultGun implements Gun {
	private final GunData data;
	private final IFeedingSystem feedingSystem;
	private long lastShotTime = 0;
	private long burstEndTime = 0;
	private boolean triggerReleased = true;
	@Nullable private Entity holder;

	private FireMode currentFireMode;
	private boolean needsChambering = false;

	public DefaultGun(GunData data, @Nullable Entity holder) {
		this.data = data;
		this.holder = holder;
		this.feedingSystem = createFeedingSystem();
		this.currentFireMode = data.fireMods()[0];
	}

	protected IFeedingSystem createFeedingSystem() {
		if (data.magazineSetting() == MagazineSetting.AMMO) {
			return new DirectFeedingSystem(this, data.magazineCapacity(), data.hasChamber());
		}
		return new MagazineFeedingSystem(this);
	}

	@Override
	public GunData getData() { return data; }

	@Override
	public IFeedingSystem getFeedingSystem() { return feedingSystem; }

	@Override
	public FireMode getCurrentFireMode() { return currentFireMode; }

	@Override
	public void setFireMode(FireMode mode) {
		if (Arrays.asList(data.fireMods()).contains(mode)) {
			this.currentFireMode = mode;
		}
	}

	@Override
	public void cycleFireMode() {
		FireMode[] modes = data.fireMods();
		int currentIndex = Arrays.asList(modes).indexOf(currentFireMode);
		currentFireMode = modes[(currentIndex + 1) % modes.length];
	}

	@Override
	public boolean needsChambering() { return needsChambering; }

	@Override
	public boolean needsManualChambering() {return currentFireMode == FireMode.BOLT;}

	@Override
	public boolean canShoot(long currentTime) {
		if (!feedingSystem.canFire()) return false;

		if ((currentFireMode == FireMode.BOLT) && needsChambering) {
			return false;
		}

		if ((currentFireMode == FireMode.SINGLE || currentFireMode == FireMode.BURST) && !triggerReleased) {
			return false;
		}

		if (currentTime < burstEndTime) return false;

		long msBetweenShots = 60000L / data.fireRate();
		return (currentTime - lastShotTime) >= msBetweenShots;
	}

	@Override
	public void releaseTrigger() {
		this.triggerReleased = true;
	}

	@Override
	public ShootResult shoot(ServerLevel level, Player player, Vec3 muzzlePos, Vec3 lookDir, long currentTime) {
		if (!canShoot(currentTime)) return ShootResult.FAILURE;

		return switch (currentFireMode) {
			case AUTO, BOLT, SINGLE -> fireSingle(level, player, muzzlePos, lookDir, currentTime);
			case BURST -> fireBurst(level, player, muzzlePos, lookDir, currentTime);
		};
	}

	private ShootResult fireSingle(ServerLevel level, Player player, Vec3 muzzlePos, Vec3 lookDir, long currentTime) {
		AmmoType ammo = feedingSystem.fire();
		if (ammo == null) return ShootResult.FAILURE;

		long bulletId = spawnBullet(level, muzzlePos, lookDir, ammo, player);
		this.lastShotTime = currentTime;

		if (currentFireMode == FireMode.SINGLE) {
			this.triggerReleased = false;
		}

		if (currentFireMode == FireMode.BOLT) {
			this.needsChambering = true;
		}

		sendRecoil(player, (float) (data.recoilBase() * getSpreadMultiplier(player)));
		return new ShootResult(true, bulletId, currentFireMode, ammo);
	}

	private ShootResult fireBurst(ServerLevel level, Player player, Vec3 muzzlePos, Vec3 lookDir, long currentTime) {
		int burstSize = Math.max(1, data.burstSize());
		int delayTicks = Math.max(1, data.burstTime());

		AmmoType firstAmmo = feedingSystem.fire();
		if (firstAmmo == null) return ShootResult.FAILURE;

		long firstBulletId = spawnBullet(level, muzzlePos, lookDir, firstAmmo, player);
		sendRecoil(player, (float) (data.recoilBase() * getSpreadMultiplier(player)));

		this.triggerReleased = false;

		MinecraftServer server = level.getServer();
		int currentTick = server.getTickCount();

		final Vec3 shotPos = muzzlePos;
		final Vec3 shotDir = lookDir;

		for (int i = 1; i < burstSize; i++) {
			server.tell(new TickTask(currentTick + delayTicks * i, () -> {
				if (player.isRemoved() || !player.isAlive()) return;
				if (!feedingSystem.canFire()) return;

				AmmoType ammo = feedingSystem.fire();
				if (ammo != null) {
					spawnBullet(level, shotPos, shotDir, ammo, player);
					sendRecoil(player, (float) (data.recoilBase() * getSpreadMultiplier(player)));
				}
			}));
		}

		long totalBurstDurationMs = (long) (burstSize - 1) * delayTicks * 50L;
		this.burstEndTime = currentTime + totalBurstDurationMs;
		this.lastShotTime = currentTime;

		return new ShootResult(true, firstBulletId, currentFireMode, firstAmmo);
	}

	private long spawnBullet(ServerLevel level, Vec3 muzzlePos, Vec3 direction, AmmoType ammo, Player player) {
		double finalVelocity = ammo.bulletConfig().muzzleVelocity() * ammo.muzzleVelocityMod();
		Vec3 velocityVector = direction.scale(finalVelocity);

		long bulletId = BulletManager.getInstance().getNextBulletId();

		BulletManager.getInstance().spawnBullet(
				level, muzzlePos, velocityVector, ammo.bulletConfig(), player.getUUID()
		);

		return bulletId;
	}

	private void sendRecoil(Player player, float recoil) {
		if (player instanceof ServerPlayer sp) {
			AsLibNetworking.SENDER.sendToPlayer(sp, new SyncRecoil(recoil));
		}
	}

	@Override
	public boolean loadAmmo(AmmoType ammo) {
		if (feedingSystem instanceof IDirectFeedingSystem direct) {
			if (!data.compatibleAmmoIds().contains(ammo.id())) return false;
			return direct.loadBullet(ammo);
		}
		if (feedingSystem instanceof IMagazineFeedingSystem mag) {
			return mag.loadBullet(ammo);
		}
		return false;
	}

	@Override
	public boolean loadMagazine(ItemStack magazineStack) {
		if (data.magazineSetting() == MagazineSetting.AMMO) return false;
		if (!(feedingSystem instanceof IMagazineFeedingSystem magFs)) return false;
		if (!(magazineStack.getItem() instanceof MagazineItem magItem)) return false;

		if (!data.compatibleMagazineIds().contains(magItem.getMagazineData().id())) {
			return false;
		}
		return magFs.attachMagazine(magazineStack);
	}

	@Override
	public ItemStack unloadMagazine() {
		if (data.magazineSetting() == MagazineSetting.AMMO) return ItemStack.EMPTY;
		if (!(feedingSystem instanceof IMagazineFeedingSystem magFs)) return ItemStack.EMPTY;

		ItemStack mag = magFs.detachMagazine();
		return mag != null ? mag : ItemStack.EMPTY;
	}

	@Override
	public boolean tryLoadFromOffhand(Player player) {
		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) return false;

		if (offhand.getItem() instanceof MagazineItem magItem) {
			if (data.compatibleMagazineIds().stream().noneMatch((i) -> i.equals(magItem.getMagazineData().id()))) {
				return false;
			}

			ItemStack oldMag = unloadMagazine();
			if (!oldMag.isEmpty()) {
				if (!player.addItem(oldMag)) {
					player.drop(oldMag, false);
				}
			}

			ItemStack newMag = offhand.copy();
			newMag.setCount(1);
			if (loadMagazine(newMag)) {
				offhand.shrink(1);
				return true;
			}
			return false;
		}

		if (offhand.getItem() instanceof AmmoItem ammoItem) {
			if (loadAmmo(ammoItem.getAmmoType())) {
				offhand.shrink(1);
				return true;
			}
		}
		return false;
	}

	private float getSpreadMultiplier(Player player) {
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) return 2.0f;
		return 1.0f;
	}

	@Override
	public int getAmmoCount() { return feedingSystem.getTotalAmmoCount(); }

	@Override
	public boolean hasChamberedRound() { return feedingSystem.canFire(); }

	@Override
	public boolean chamberRound() {
		boolean success = feedingSystem.chamberNext() != null;
		if (success) this.needsChambering = false;
		return success;
	}

	@Override
	public CompoundTag saveToNBT(CompoundTag tag) {
		tag.put("Feeding", feedingSystem.serializeNBT());
		tag.putLong("LastShotTime", this.lastShotTime);
		tag.putString("FireMode", currentFireMode.name());
		tag.putBoolean("NeedsChambering", needsChambering);
		return tag;
	}

	@Override
	public void loadFromNBT(CompoundTag tag) {
		if (tag.contains("Feeding")) {
			feedingSystem.deserializeNBT(tag.getCompound("Feeding"));
		}
		this.lastShotTime = tag.getLong("LastShotTime");

		if (tag.contains("FireMode")) {
			try {
				this.currentFireMode = FireMode.valueOf(tag.getString("FireMode"));
			} catch (IllegalArgumentException e) {
				this.currentFireMode = data.fireMods()[0];
			}
		}

		this.needsChambering = tag.getBoolean("NeedsChambering");
	}

	@Override
	public Entity getHolder() { return this.holder; }

	@Override
	public void setHolder(Entity newOwner) { this.holder = newOwner; }
}