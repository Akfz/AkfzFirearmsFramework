package v.akfz.aff.gun;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.feeding.IFeedingSystem;

public interface Gun {
	GunData getData();
	IFeedingSystem getFeedingSystem();

	FireMode getCurrentFireMode();
	void setFireMode(FireMode mode);
	void cycleFireMode();

	void releaseTrigger();
	boolean canShoot(long currentTime);
	ShootResult shoot(ServerLevel level, Player player, Vec3 muzzlePos, Vec3 lookDir, long currentTime);

	boolean needsChambering();

	boolean loadAmmo(AmmoType ammo);
	boolean loadMagazine(ItemStack magazineStack);
	ItemStack unloadMagazine();
	boolean tryLoadFromOffhand(Player player);

	int getAmmoCount();
	boolean hasChamberedRound();
	boolean needsManualChambering();
	boolean chamberRound();

	CompoundTag saveToNBT(CompoundTag tag);
	void loadFromNBT(CompoundTag tag);

	Entity getHolder();
	void setHolder(Entity newOwner);

	record ShootResult(boolean success, long bulletId, FireMode actualMode, AmmoType usedAmmo) {
		public static final ShootResult FAILURE = new ShootResult(false, -1, null, null);
	}
}