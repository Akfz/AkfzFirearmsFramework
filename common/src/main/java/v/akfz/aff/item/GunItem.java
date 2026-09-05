package v.akfz.aff.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.DefaultGun;
import v.akfz.aff.gun.Gun;

public interface GunItem extends GItem {
	@Nullable Gun.ShootResult shoot(ServerPlayer player,ItemStack stack, boolean stop);
	boolean changeFireMod(ServerPlayer player,ItemStack stack);
	boolean reload(ServerPlayer player, ItemStack stack);
	boolean unload(ServerPlayer player, ItemStack stack);
	boolean chamber(ServerPlayer player, ItemStack stack);
	int check(ItemStack stack);

	String getGunId();
	DefaultGun getOrCreateGunInstance(ItemStack stack,@Nullable Player holder);
	void saveGunToStack(ItemStack stack, Gun gun);
}
