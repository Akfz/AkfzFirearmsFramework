package v.akfz.aff.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.gun.ammo.MagazineInstance;
import v.akfz.aff.gun.registry.AmmoRegistry;

import java.util.List;

public class MagazineItemInstance extends Item implements MagazineItem{
	private final MagazineData magazineData;

	public MagazineItemInstance(Properties pProperties, MagazineData magazineData) {
		super(pProperties);
		this.magazineData = magazineData;
	}

	@Override
	public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
		MagazineInstance magazine = getOrCreateMagazine(stack);

		if (!magazine.isEmpty()) {
			var bullets = magazine.getBullets();
			if (!bullets.isEmpty()) {
				AmmoType firstAmmo = bullets.iterator().next();
				tooltip.add(Component.translatable("aff.magazine.tooltip.type", firstAmmo.displayName()));
			}
		}
	}

	@Override
	public MagazineData getMagazineData() {
		return magazineData;
	}

	@Override
	public MagazineInstance getOrCreateMagazine(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("MagazineData")) {
			MagazineInstance loaded = MagazineInstance.deserializeNBT(tag.getCompound("MagazineData"));
			if (loaded != null) return loaded;
		}
		return new MagazineInstance(magazineData);
	}

	@Override
	public void saveMagazine(ItemStack stack, MagazineInstance magazine) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.put("MagazineData", magazine.serializeNBT());
	}

	public String ammoCount(ItemStack stack) {
		MagazineInstance magazine = getOrCreateMagazine(stack);
		if (magazine != null) return String.valueOf(magazine.getAmmoCount());
		return "cantload";
	}

	@Override
	public void useByKeyEvent(String GIS_ID, ServerPlayer pPlayer, boolean triggered) {
		if (pPlayer == null || pPlayer.level().isClientSide) return;
		ItemStack stack = pPlayer.getMainHandItem();
		MagazineInstance magazine = getOrCreateMagazine(stack);
		if (magazine == null) return;

		switch (GIS_ID) {
			case "aff:reload" -> {
				if (triggered) {
					ItemStack offhand = pPlayer.getOffhandItem();
					if (!offhand.isEmpty() && offhand.getItem() instanceof AmmoItem ammoItem) {
						AmmoType ammo = ammoItem.getAmmoType();

						if (!magazineData.allowedAmmoIds().contains(ammo.id())) {
							pPlayer.displayClientMessage(Component.translatable("aff.magazine.incompatible"),true);
							return;
						}

						if (magazine.isFull()) {
							pPlayer.displayClientMessage(Component.translatable("aff.magazine.full"),true);
							return;
						}

						if (magazine.loadAmmo(ammo)) {
							offhand.shrink(1);
							saveMagazine(stack,magazine);
						}
					}
				}
			}
			case "aff:unload" -> {
				if (triggered) {
					if (!magazine.isEmpty()) {
						AmmoType ammo = magazine.peekBullet();
						Item ammoI = AmmoRegistry.getItem(ammo);
						if (ammoI != null) {
							ItemStack item = new ItemStack(ammoI);
							if (!pPlayer.addItem(item)) {
								pPlayer.drop(item,false);
							}
							magazine.removeBullet();
							saveMagazine(stack,magazine);
						}
					} else {
						pPlayer.displayClientMessage(Component.translatable("aff.magazine.empty"),true);
					}
				}
			}
			case "aff:check" -> {
				if (triggered) {
					pPlayer.displayClientMessage(Component.literal(ammoCount(stack)),true);
				}
			}
		}
	}
}
