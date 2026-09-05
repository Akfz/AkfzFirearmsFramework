package v.akfz.aff.gun.feeding;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.ammo.MagazineInstance;

public interface IMagazineFeedingSystem extends IFeedingSystem {
	boolean attachMagazine(ItemStack magazineStack);
	@Nullable ItemStack detachMagazine();
	boolean loadBullet(AmmoType ammo);

	@Nullable
	MagazineInstance getMagazineInstance();
}