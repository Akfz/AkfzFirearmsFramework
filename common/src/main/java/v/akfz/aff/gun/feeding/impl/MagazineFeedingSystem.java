package v.akfz.aff.gun.feeding.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.data.magazine.MagazineSetting;
import v.akfz.aff.gun.Gun;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.ammo.MagazineInstance;
import v.akfz.aff.gun.feeding.IFeedingSystem;
import v.akfz.aff.gun.feeding.IMagazineFeedingSystem;
import v.akfz.aff.gun.registry.AmmoRegistry;
import v.akfz.aff.item.MagazineItem;

public class MagazineFeedingSystem implements IMagazineFeedingSystem {
	@Nullable private ItemStack currentMagazineItem = null;
	@Nullable private AmmoType chamberAmmo = null;
	private final Gun owner;

	@Nullable private MagazineInstance cachedMagazineInstance = null;

	public MagazineFeedingSystem(Gun owner) {
		this.owner = owner;
	}

	public boolean attachMagazine(ItemStack magazineStack) {
		if (currentMagazineItem != null) return false;
		if (!(magazineStack.getItem() instanceof MagazineItem)) return false;

		this.currentMagazineItem = magazineStack;
		this.cachedMagazineInstance = null;
		return true;
	}

	@Nullable
	public ItemStack detachMagazine() {
		saveMagazineToStack();
		ItemStack removed = this.currentMagazineItem;
		this.currentMagazineItem = null;
		this.cachedMagazineInstance = null;
		return removed;
	}

	@Nullable
	public MagazineInstance getMagazineInstance() {
		if (currentMagazineItem == null) return null;

		if (!(currentMagazineItem.getItem() instanceof MagazineItem magItem)) {
			currentMagazineItem = null;
			cachedMagazineInstance = null;
			return null;
		}

		if (cachedMagazineInstance == null) {
			cachedMagazineInstance = magItem.getOrCreateMagazine(currentMagazineItem);
		}
		return cachedMagazineInstance;
	}

	@Override
	public boolean loadBullet(AmmoType ammo) {
		if (owner.getData().magazineSetting() == MagazineSetting.MAGAZINE) {
			return false;
		}

		MagazineInstance mag = getMagazineInstance();
		if (mag != null) {
			if (mag.loadAmmo(ammo)) {
				saveMagazineToStack();
				return true;
			}
			return false;
		}

		if (owner.getData().hasChamber() && chamberAmmo == null) {
			if (!owner.getData().compatibleAmmoIds().contains(ammo.id())) return false;
			chamberAmmo = ammo;
			return true;
		}
		return false;
	}

	@Override
	public AmmoType chamberNext() {
		MagazineInstance mag = getMagazineInstance();
		if (mag == null || mag.isEmpty()) return null;
		if (chamberAmmo != null) return null;

		chamberAmmo = mag.removeBullet();
		saveMagazineToStack();
		return chamberAmmo;
	}

	@Override
	public AmmoType fire() {
		if (chamberAmmo == null) return null;
		AmmoType fired = chamberAmmo;
		chamberAmmo = null;
		if (!owner.needsManualChambering()) {
			chamberNext();
		}
		return fired;
	}

	@Override
	public boolean canFire() {
		return chamberAmmo != null;
	}

	@Override
	public int getTotalAmmoCount() {
		int magCount = getMagazineInstance() != null ? getMagazineInstance().getAmmoCount() : 0;
		return magCount + (chamberAmmo != null ? 1 : 0);
	}

	@Override
	public void unload() {
		ItemStack mag = detachMagazine();
		if (mag != null) {
			if (owner.getHolder() instanceof Player pl) {
				if (!pl.addItem(mag)) {
					pl.drop(mag, false);
				}
			} else if (owner.getHolder() != null) {
				dropItemInWorld(mag);
			}
		}

		if (chamberAmmo != null) {
			Item ammoItem = AmmoRegistry.getItem(chamberAmmo);
			if (ammoItem != null) {
				if (owner.getHolder() instanceof Player pl) {
					if (!pl.addItem(new ItemStack(ammoItem))) {
						pl.drop(new ItemStack(ammoItem), false);
					}
				} else if (owner.getHolder() != null) {
					dropItemInWorld(new ItemStack(ammoItem));
				}
			}
			chamberAmmo = null;
		}
	}

	private void dropItemInWorld(ItemStack stack) {
		if (owner.getHolder() == null) return;
		var level = owner.getHolder().level();
		if (level.isClientSide()) return;

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

	@Nullable
	public AmmoType ejectChamber() {
		AmmoType ejected = chamberAmmo;
		chamberAmmo = null;
		return ejected;
	}

	@Override
	public CompoundTag serializeNBT() {
		saveMagazineToStack();

		CompoundTag tag = new CompoundTag();

		if (chamberAmmo != null) {
			tag.putString("chamber", chamberAmmo.id());
		}

		if (currentMagazineItem != null) {
			tag.put("magazineItem", currentMagazineItem.save(new CompoundTag()));
		}

		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		this.chamberAmmo = null;
		this.currentMagazineItem = null;
		this.cachedMagazineInstance = null;

		if (tag.contains("chamber")) {
			String ammoId = tag.getString("chamber");
			this.chamberAmmo = AmmoRegistry.getAmmoType(ammoId);
		}

		if (tag.contains("magazineItem")) {
			ItemStack loadedStack = ItemStack.of(tag.getCompound("magazineItem"));

			if (!loadedStack.isEmpty() && loadedStack.getItem() instanceof MagazineItem) {
				this.currentMagazineItem = loadedStack;
				loadMagazineFromStack();
			}
		}
	}

	@Override
	public Gun getOwner() {
		return this.owner;
	}

	private void saveMagazineToStack() {
		if (currentMagazineItem != null && cachedMagazineInstance != null) {
			if (currentMagazineItem.getItem() instanceof MagazineItem magItem) {
				magItem.saveMagazine(currentMagazineItem, cachedMagazineInstance);
			} else {
				currentMagazineItem = null;
				cachedMagazineInstance = null;
			}
		}
	}

	private void loadMagazineFromStack() {
		if (currentMagazineItem != null && currentMagazineItem.getItem() instanceof MagazineItem magItem) {
			this.cachedMagazineInstance = magItem.getOrCreateMagazine(currentMagazineItem);
		}
	}
}