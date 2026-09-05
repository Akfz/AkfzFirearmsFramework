package v.akfz.aff.item;

import net.minecraft.world.item.ItemStack;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.gun.ammo.MagazineInstance;

public interface MagazineItem extends GItem {
	MagazineData getMagazineData();
	MagazineInstance getOrCreateMagazine(ItemStack stack);
	void saveMagazine(ItemStack stack, MagazineInstance magazine);
}