package v.akfz.aff.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aff.gun.DefaultGun;
import v.akfz.aff.gun.Gun;
import v.akfz.aff.gun.registry.GunRegistry;
import v.akfz.aff.item.GunItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Its more like template
 */
public class TestGunItem extends Item implements GunItem {
	private final String gunId;
	private static final Map<ItemStack, DefaultGun> GUN_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

	public TestGunItem(Properties pProperties,String gunId) {
		super(pProperties);
		this.gunId = gunId;
	}

	@Override
	public String getGunId() {
		return gunId;
	}

	@Override
	public DefaultGun getOrCreateGunInstance(ItemStack stack, @Nullable Player holder) {
		DefaultGun cached = GUN_CACHE.get(stack);
		if (cached != null && cached.getHolder() == holder) {
			return cached;
		}

		GunData data = GunRegistry.getGun(gunId);
		if (data == null) return null;

		DefaultGun gun = new DefaultGun(data, holder);
		gun.loadFromNBT(stack.getOrCreateTag());
		GUN_CACHE.put(stack, gun);
		return gun;
	}

	@Override
	public void saveGunToStack(ItemStack stack, Gun gun) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString("GunId", gunId);
		gun.saveToNBT(tag);
		GUN_CACHE.put(stack, (DefaultGun) gun);
	}

	@Override
	public Gun.ShootResult shoot(ServerPlayer player, ItemStack stack, boolean stop) {
		DefaultGun gun = getOrCreateGunInstance(stack, player);
		if (gun == null) return null;
		if (stop) {
			gun.releaseTrigger();
			return null;
		}

		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		Vec3 eyePos = player.getEyePosition(1.0f);
		Vec3 lookDir = player.getLookAngle();
		Vec3 muzzlePos = eyePos.add(lookDir.scale(0.6)).add(0.0, -0.3, 0.0);
		long currentTime = System.currentTimeMillis();

		Gun.ShootResult result = gun.shoot(serverLevel, player, muzzlePos, lookDir, currentTime);

		saveGunToStack(stack, gun);
		return result;
	}

	@Override
	public boolean changeFireMod(ServerPlayer player, ItemStack stack) {
		DefaultGun gun = getOrCreateGunInstance(stack, player);
		if (gun == null) return false;

		gun.cycleFireMode();
		saveGunToStack(stack, gun);

		player.displayClientMessage(
				Component.literal("§eFire mode: " + gun.getCurrentFireMode().name()), true
		);
		return true;
	}

	@Override
	public boolean reload(ServerPlayer player, ItemStack stack) {
		DefaultGun gun = getOrCreateGunInstance(stack, player);
		if (gun == null) return false;

		boolean success = gun.tryLoadFromOffhand(player);
		saveGunToStack(stack, gun);
		return success;
	}

	@Override
	public boolean unload(ServerPlayer player, ItemStack stack) {
		DefaultGun gun = getOrCreateGunInstance(stack, player);
		if (gun == null) return false;

		ItemStack mag = gun.unloadMagazine();
		saveGunToStack(stack, gun);

		if (!mag.isEmpty()) {
			if (!player.addItem(mag)) {
				player.drop(mag, false);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean chamber(ServerPlayer player, ItemStack stack) {
		DefaultGun gun = getOrCreateGunInstance(stack, player);
		if (gun == null) return false;

		boolean success = gun.chamberRound();
		saveGunToStack(stack, gun);

		if (success) {
			player.serverLevel().playSound(null, player, SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
		return success;
	}

	@Override
	public int check(ItemStack stack) {
		DefaultGun gun = getOrCreateGunInstance(stack, null);
		if (gun == null) return -1;
		return gun.getAmmoCount();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		GunData data = GunRegistry.getGun(gunId);
		if (data == null) {
			tooltip.add(Component.translatable("aff.gun.tooltip.error", gunId));
			return;
		}
	}

	@Override
	public void useByKeyEvent(String GIS_ID, ServerPlayer pPlayer, boolean triggered) {
		if (pPlayer == null || pPlayer.level().isClientSide) return;

		ItemStack stack = pPlayer.getMainHandItem();

		switch (GIS_ID) {
			case "aff:shoot" -> shoot(pPlayer, stack,!triggered);
			case "aff:chamber" -> {
				if (triggered) {
					chamber(pPlayer,stack);
				}
			}
			case "aff:changefiremod" -> {
				if (triggered) {
					changeFireMod(pPlayer,stack);
				}
			}
			case "aff:reload" -> {
				if (triggered) {
					boolean success = reload(pPlayer,stack);
					if (success) {
						pPlayer.displayClientMessage(Component.literal("success " + check(stack)),true);
					} else {
						pPlayer.displayClientMessage(Component.literal("fail"),true);
					}
				}
			}
			case "aff:unload" -> {
				if (triggered) {
					boolean success = unload(pPlayer, stack);
					if (success) {
						pPlayer.displayClientMessage(
								Component.literal("success"),
								true
						);
					} else {
						pPlayer.displayClientMessage(
								Component.literal("fail"),
								true
						);
					}
				}
			}
			case "aff:check" -> {
				if (triggered) {
					int ammo = check(stack);
					DefaultGun gun = getOrCreateGunInstance(stack,pPlayer);
					if (gun != null) {
						String chamberStatus = gun.hasChamberedRound() ? "§a✓" : "§c✗";
						pPlayer.displayClientMessage(Component.literal("§7Ammo: §f" + ammo + " §7| Chamber: " + chamberStatus),true);
					}
				}
			}
			case "aff:aim" -> {
			}
		}
	}
}