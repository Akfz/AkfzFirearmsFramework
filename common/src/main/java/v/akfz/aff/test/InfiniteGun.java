package v.akfz.aff.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.gun.DefaultGun;
import v.akfz.aff.gun.Gun;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.feeding.IFeedingSystem;
import v.akfz.aff.gun.registry.AmmoRegistry;
import v.akfz.aff.gun.registry.MagazineRegistry;

public class InfiniteGun extends DefaultGun {

	public InfiniteGun(GunData data, @Nullable Entity holder) {
		super(data, holder);
	}

	@Override
	protected IFeedingSystem createFeedingSystem() {
		return new InfiniteFeedingSystem(this, getData());
	}

	@Override
	public int getAmmoCount() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean hasChamberedRound() {
		return true;
	}

	@Override
	public CompoundTag saveToNBT(CompoundTag tag) {
		super.saveToNBT(tag);
		tag.putBoolean("InfiniteAmmo", true);
		return tag;
	}

	private static class InfiniteFeedingSystem implements IFeedingSystem {
		private final Gun owner;
		private final AmmoType currentAmmo;

		public InfiniteFeedingSystem(Gun owner, GunData data) {
			this.owner = owner;
			this.currentAmmo = resolveInfiniteAmmo(data);
		}

		private AmmoType resolveInfiniteAmmo(GunData data) {
			if (data.compatibleAmmoIds() != null && !data.compatibleAmmoIds().isEmpty()) {
				return AmmoRegistry.getAmmoType(data.compatibleAmmoIds().get(0));
			}

			if (data.compatibleMagazineIds() != null && !data.compatibleMagazineIds().isEmpty()) {
				String firstMagId = data.compatibleMagazineIds().get(0);
				MagazineData magData = MagazineRegistry.getMagazineData(firstMagId);

				if (magData != null && !magData.allowedAmmoIds().isEmpty()) {
					return AmmoRegistry.getAmmoType(magData.allowedAmmoIds().get(0));
				}
			}

			return null;
		}

		@Override
		public AmmoType fire() {
			return currentAmmo;
		}

		@Override
		public boolean canFire() {
			return currentAmmo != null;
		}

		@Override
		public int getTotalAmmoCount() {
			return Integer.MAX_VALUE;
		}

		@Override
		public void unload() {
		}

		@Override
		public CompoundTag serializeNBT() {
			return new CompoundTag();
		}

		@Override
		public void deserializeNBT(CompoundTag tag) {
		}

		@Override
		public Gun getOwner() {
			return owner;
		}

		@Override
		public @Nullable AmmoType chamberNext() {
			return currentAmmo;
		}
	}
}