package v.akfz.aff.gun.ammo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.gun.registry.AmmoRegistry;
import v.akfz.aff.gun.registry.MagazineRegistry;

import java.util.Collection;

public class MagazineInstance {
	private final MagazineData data;
	private final Magazine magazine;

	public MagazineInstance(MagazineData data) {
		this.data = data;
		this.magazine = new Magazine(data.capacity());
	}

	public boolean loadAmmo(AmmoType ammo) {
		if (!data.allowedAmmoIds().contains(ammo.id())) {
			return false;
		}
		return magazine.addBullet(ammo);
	}

	public AmmoType removeBullet() {
		return magazine.removeBullet();
	}

	public AmmoType peekBullet() {
		return magazine.peekBullet();
	}

	public int getAmmoCount() {
		return magazine.getCurrentCount();
	}

	public int getMaxCapacity() {
		return magazine.getMaxCapacity();
	}

	public boolean isEmpty() {
		return magazine.isEmpty();
	}

	public boolean isFull() {
		return magazine.isFull();
	}

	public MagazineData getData() {
		return data;
	}

	public Collection<AmmoType> getBullets() {
		return this.magazine.getBullets();
	}

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", data.id());
		ListTag bulletsTag = new ListTag();
		for (AmmoType ammo : magazine.getBullets()) {
			bulletsTag.add(StringTag.valueOf(ammo.id()));
		}
		tag.put("bullets", bulletsTag);
		return tag;
	}

	public static MagazineInstance deserializeNBT(CompoundTag tag) {
		String magazineId = tag.getString("id");
		MagazineData data = MagazineRegistry.getMagazineData(magazineId);
		if (data == null) return null;

		MagazineInstance instance = new MagazineInstance(data);
		if (tag.contains("bullets")) {
			ListTag bulletsTag = tag.getList("bullets", Tag.TAG_STRING);
			for (int i = 0; i < bulletsTag.size(); i++) {
				String ammoId = bulletsTag.getString(i);
				AmmoType ammo = AmmoRegistry.getAmmoType(ammoId);
				if (ammo != null) {
					instance.magazine.addBullet(ammo);
				}
			}
		}
		return instance;
	}
}