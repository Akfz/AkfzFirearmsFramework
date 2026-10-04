package v.akfz.aff.gun.feeding.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.Gun;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.ammo.Magazine;
import v.akfz.aff.gun.feeding.IDirectFeedingSystem;
import v.akfz.aff.gun.registry.AmmoRegistry;

public class DirectFeedingSystem implements IDirectFeedingSystem {
	private final Gun owner;
	private final Magazine internalMagazine;
	private final boolean hasChamber;
	@Nullable private AmmoType chamberAmmo = null;

	public DirectFeedingSystem(Gun owner, int capacity, boolean hasChamber) {
		this.owner = owner;
		this.internalMagazine = new Magazine(capacity);
		this.hasChamber = hasChamber;
	}

	@Override
	public boolean loadBullet(AmmoType ammo) {
		return internalMagazine.addBullet(ammo);
	}

	@Override @Nullable
	public AmmoType chamberNext() {
		if (!hasChamber) return null;
		if (chamberAmmo != null) return null;
		if (internalMagazine.isEmpty()) return null;
		chamberAmmo = internalMagazine.removeBullet();
		return chamberAmmo;
	}

	@Override
	@Nullable
	public AmmoType fire() {
		if (hasChamber) {
			if (chamberAmmo == null) return null;
			AmmoType fired = chamberAmmo;
			chamberAmmo = null;
			return fired;
		} else {
			return internalMagazine.removeBullet();
		}
	}

	@Override
	public boolean canFire() {
		return hasChamber ? chamberAmmo != null : !internalMagazine.isEmpty();
	}

	@Override
	public int getTotalAmmoCount() {
		return internalMagazine.getCurrentCount() + (chamberAmmo != null ? 1 : 0);
	}

	@Override
	public int getMaxCapacity() {
		return internalMagazine.getMaxCapacity() + (hasChamber ? 1 : 0);
	}

	@Override
	public void unload() {
		while (!internalMagazine.isEmpty()) {
			AmmoType ammo = internalMagazine.removeBullet();
			returnAmmo(ammo);
		}
		if (chamberAmmo != null) {
			returnAmmo(chamberAmmo);
			chamberAmmo = null;
		}
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("hasChamber", hasChamber);

		ListTag bulletsTag = new ListTag();
		for (AmmoType ammo : internalMagazine.getBullets()) {
			bulletsTag.add(StringTag.valueOf(ammo.id()));
		}
		tag.put("bullets", bulletsTag);

		if (chamberAmmo != null) {
			tag.putString("chamber", chamberAmmo.id());
		}
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		internalMagazine.clear();
		chamberAmmo = null;

		if (tag.contains("bullets")) {
			ListTag bulletsTag = tag.getList("bullets", Tag.TAG_STRING);
			for (int i = 0; i < bulletsTag.size(); i++) {
				AmmoType ammo = AmmoRegistry.getAmmoType(bulletsTag.getString(i));
				if (ammo != null) internalMagazine.addBullet(ammo);
			}
		}
		if (tag.contains("chamber")) {
			chamberAmmo = AmmoRegistry.getAmmoType(tag.getString("chamber"));
		}
	}

	@Override
	public Gun getOwner() {
		return owner;
	}

	private void returnAmmo(AmmoType ammo) {
		Item item = AmmoRegistry.getItem(ammo);
		if (item == null) return;
		ItemStack stack = new ItemStack(item);

		if (owner.getHolder() instanceof Player pl) {
			if (!pl.addItem(stack)) {
				pl.drop(stack, false);
			}
		} else if (owner.getHolder() != null) {
			var level = owner.getHolder().level();
			if (!level.isClientSide()) {
				ItemEntity entity = new ItemEntity(
						level,
						owner.getHolder().getX(),
						owner.getHolder().getY() + 0.5,
						owner.getHolder().getZ(),
						stack
				);
				entity.setPickUpDelay(20);
				level.addFreshEntity(entity);
			}
		}
	}
}