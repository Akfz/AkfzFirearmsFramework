package v.akfz.aff.gun.registry;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.gun.ammo.AmmoType;
import v.akfz.aff.item.AmmoItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AmmoRegistry {
	private static final Map<String, Item> AMMO_ITEMS = new HashMap<>();

	public static void register(Item item) {
		if (!(item instanceof AmmoItem ammoItem)) {
			throw new IllegalArgumentException("Item must be an AmmoItem: " + item);
		}
		AmmoType type = ammoItem.getAmmoType();
		if (AMMO_ITEMS.containsKey(type.id())) {
			throw new IllegalStateException("Duplicate ammo registration: " + type.id());
		}
		AMMO_ITEMS.put(type.id(), item);
	}

	@Nullable
	public static Item getItem(String ammoId) {
		return AMMO_ITEMS.get(ammoId);
	}

	@Nullable
	public static Item getItem(AmmoType type) {
		return type != null ? AMMO_ITEMS.get(type.id()) : null;
	}

	@Nullable
	public static AmmoType getAmmoType(Item item) {
		return item instanceof AmmoItem ammoItem ? ammoItem.getAmmoType() : null;
	}

	@Nullable
	public static AmmoType getAmmoType(String ammoId) {
		Item item = AMMO_ITEMS.get(ammoId);
		return item instanceof AmmoItem ammoItem ? ammoItem.getAmmoType() : null;
	}

	public static Collection<Item> getAllItems() {
		return AMMO_ITEMS.values();
	}

	public static Map<String, AmmoType> getAllAmmoTypes() {
		Map<String, AmmoType> result = new HashMap<>();
		for (Map.Entry<String, Item> entry : AMMO_ITEMS.entrySet()) {
			if (entry.getValue() instanceof AmmoItem ammoItem) {
				result.put(entry.getKey(), ammoItem.getAmmoType());
			}
		}
		return result;
	}

	public static boolean isRegistered(String ammoId) {
		return AMMO_ITEMS.containsKey(ammoId);
	}
}