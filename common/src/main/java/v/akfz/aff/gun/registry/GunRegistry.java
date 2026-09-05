package v.akfz.aff.gun.registry;

import v.akfz.aff.data.config.GunDataCollection;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.aslib.util.json.JsonFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GunRegistry {
	private static final Map<String,GunData> REGISTERED_GUNS = new ConcurrentHashMap<>();
	private static final Path GUNS_PATH = Paths.get("").resolve("AFF").resolve("guns.json");

	public static void registerDefault(GunData data) {
		REGISTERED_GUNS.putIfAbsent(data.id(), data);
	}

	public static void load() {
		GunDataCollection collection = GsonHelper.read(GUNS_PATH, GunDataCollection.class);

		if (collection == null || collection.guns() == null || collection.guns().isEmpty()) {
			collection = new GunDataCollection(new ConcurrentHashMap<>(REGISTERED_GUNS));
			save(collection);
		} else {
			for (Map.Entry<String, GunData> entry : REGISTERED_GUNS.entrySet()) {
				collection.guns().putIfAbsent(entry.getKey(), entry.getValue());
			}
			save(collection);
		}

		REGISTERED_GUNS.clear();
		REGISTERED_GUNS.putAll(collection.guns());
	}

	private static void save(GunDataCollection collection) {
		GsonHelper.write(new JsonFile<>() {
			@Override
			public GunDataCollection data() {
				return collection;
			}

			@Override
			public Path getPath() {
				return GUNS_PATH;
			}
		});
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