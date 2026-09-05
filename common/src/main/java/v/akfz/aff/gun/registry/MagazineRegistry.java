package v.akfz.aff.gun.registry;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.item.MagazineItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class MagazineRegistry {
	private static final Map<String, Item> MAGAZINE_ITEMS = new HashMap<>();

	public static void register(Item item) {
		if (!(item instanceof MagazineItem magItem)) {
			throw new IllegalArgumentException("Item must be a MagazineItem: " + item);
		}
		MagazineData data = magItem.getMagazineData();
		if (MAGAZINE_ITEMS.containsKey(data.id())) {
			throw new IllegalStateException("Duplicate magazine registration: " + data.id());
		}
		MAGAZINE_ITEMS.put(data.id(), item);
	}

	@Nullable
	public static Item getItem(String magazineId) {
		return MAGAZINE_ITEMS.get(magazineId);
	}

	@Nullable
	public static Item getItem(MagazineData data) {
		return data != null ? MAGAZINE_ITEMS.get(data.id()) : null;
	}

	@Nullable
	public static MagazineData getMagazineData(Item item) {
		return item instanceof MagazineItem magItem ? magItem.getMagazineData() : null;
	}

	@Nullable
	public static MagazineData getMagazineData(String magazineId) {
		Item item = MAGAZINE_ITEMS.get(magazineId);
		return item instanceof MagazineItem magItem ? magItem.getMagazineData() : null;
	}

	public static Collection<Item> getAllItems() {
		return MAGAZINE_ITEMS.values();
	}

	public static Map<String, MagazineData> getAllMagazineData() {
		Map<String, MagazineData> result = new HashMap<>();
		for (Map.Entry<String, Item> entry : MAGAZINE_ITEMS.entrySet()) {
			if (entry.getValue() instanceof MagazineItem magItem) {
				result.put(entry.getKey(), magItem.getMagazineData());
			}
		}
		return result;
	}

	public static boolean isRegistered(String magazineId) {
		return MAGAZINE_ITEMS.containsKey(magazineId);
	}
}