package v.akfz.aff.gun.feeding;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.Gun;
import v.akfz.aff.gun.ammo.AmmoType;

public interface IFeedingSystem {
	AmmoType fire();

	boolean canFire();
	int getTotalAmmoCount();
	void unload();

	CompoundTag serializeNBT();
	void deserializeNBT(CompoundTag tag);

	Gun getOwner();
	@Nullable AmmoType chamberNext();
}