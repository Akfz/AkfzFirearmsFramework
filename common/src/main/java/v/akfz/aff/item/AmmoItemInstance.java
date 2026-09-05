package v.akfz.aff.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.ammo.AmmoType;

import java.util.List;

public class AmmoItemInstance extends Item implements AmmoItem {
	private final AmmoType ammoType;

	public AmmoItemInstance(Item.Properties pProperties,AmmoType type) {
		super(pProperties);
		this.ammoType = type;
	}

	@Override
	public AmmoType getAmmoType() {
		return ammoType;
	}

	@Override
	public Component getDescription() {
		return Component.translatable(ammoType.displayName());
	}

	@Override
	public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
		tooltip.add(Component.translatable("aff.ammo.tooltip.caliber", formatCaliber()));
		tooltip.add(Component.translatable("aff.ammo.tooltip.velocity", (int) ammoType.bulletConfig().muzzleVelocity()));
		tooltip.add(Component.translatable("aff.ammo.tooltip.mass", formatMass()));

		if (ammoType.damageMod() != 1.0) {
			tooltip.add(Component.translatable("aff.ammo.tooltip.damage", ammoType.damageMod()));
		}
		if (ammoType.penetrationMod() != 1.0) {
			tooltip.add(Component.translatable("aff.ammo.tooltip.penetration", ammoType.penetrationMod()));
		}
	}

	private String formatCaliber() {
		return String.format("%.2f", ammoType.bulletConfig().caliber() * 1000);
	}

	private String formatMass() {
		return String.format("%.1f", ammoType.bulletConfig().mass() * 1000);
	}

	@Override
	public void useByKeyEvent(String GIS_ID,ServerPlayer pPlayer, boolean triggered) {}
}
