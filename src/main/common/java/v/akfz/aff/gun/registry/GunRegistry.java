package v.akfz.aff.gun.registry;

import v.akfz.aff.data.gun.GunData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GunRegistry {
	private static final Map<String,GunData> REGISTERED_GUNS = new ConcurrentHashMap<>();

	public static void registerDefault(GunData data) {
		REGISTERED_GUNS.putIfAbsent(data.id(), data);
	}

	public static GunData getGun(String id) {
		return REGISTERED_GUNS.get(id);
	}

	public static Map<String, GunData> getAll() {
		return REGISTERED_GUNS;
	}

	public static void registerCustom(GunData data) {
		REGISTERED_GUNS.put(data.id(), data);
	}
}