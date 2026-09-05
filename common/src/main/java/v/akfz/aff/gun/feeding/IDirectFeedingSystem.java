package v.akfz.aff.gun.feeding;

import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.ammo.AmmoType;

public interface IDirectFeedingSystem extends IFeedingSystem {
	boolean loadBullet(AmmoType ammo);
	int getMaxCapacity();
}