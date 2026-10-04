package v.akfz.aff;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.bullet.BulletManager;
import v.akfz.aff.data.bind.KeyBinds;
import v.akfz.aff.data.bind.KeyBindsData;
import v.akfz.aff.data.config.MaterialData;
import v.akfz.aff.world.Material;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.aslib.util.json.JsonFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LoaderConfigs {

	public static final LoaderConfigs INSTANCE = new LoaderConfigs();

	public static final Material UNKNOWN_MATERIAL =
			new Material(99999.0, 99999.0, 99999999.0, false, false);

	private static final Path CONFIG_DIR    = GlobalUtils.getAsLibCFGPath().resolve("aff");
	private static final Path MATERIALS_FP  = CONFIG_DIR.resolve("materials.json");
	private static final Path BINDS_FP = CONFIG_DIR.resolve("binds.json");

	private volatile boolean ready = false;

	private final Map<ResourceLocation, Material> MATERIALS_BY_RL = new ConcurrentHashMap<>();
	private final Map<BlockState, Material> MATERIALS_BY_STATE = new ConcurrentHashMap<>();

	public KeyBinds binds;

	private LoaderConfigs() {}

	public boolean isReady() { return ready; }

	public void load() {
		if (ready) {
			System.out.println("[AFF] LoaderConfigs already loaded, skipping.");
			return;
		}

		ensureConfigDir();
		loadMaterials();
		loadBinds();

		ready = true;
		System.out.println("[AFF] LoaderConfigs ready. Materials: " + MATERIALS_BY_RL.size());
	}

	public void invalidateCaches() {
		MATERIALS_BY_STATE.clear();
	}

	public void registerMaterial(ResourceLocation rl, Material material) {
		if (rl == null || material == null) return;
		MATERIALS_BY_RL.put(rl, material);
		invalidateCaches();
	}

	public Material getMaterialByRL(ResourceLocation rl) {
		Material cached = MATERIALS_BY_RL.get(rl);
		if (cached != null) return cached;

		if (BulletManager.DEBUG) {
			System.out.println("[AFF_DEBUG] Unknown material: " + rl);
		}
		return UNKNOWN_MATERIAL;
	}

	public Material getMaterialByState(BlockState state) {
		Material cached = MATERIALS_BY_STATE.get(state);
		if (cached != null) return cached;

		ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		Material mat = getMaterialByRL(rl);
		MATERIALS_BY_STATE.putIfAbsent(state, mat);
		return mat;
	}

	public void save() {
		if (binds == null) return;
		try {
			Files.createDirectories(CONFIG_DIR);
			writeBinds(binds.toData());
		} catch (Exception e) {
			System.err.println("[AFF] Failed to save binds: " + e.getMessage());
		}
	}

	public void reloadBinds() {
		if (binds != null) binds.applyData(readOrCreateBinds());
	}

	public void resetBindsToDefaults() {
		this.binds = new KeyBinds();
		loadBinds();
		save();
	}

	public KeyBindsData exportBinds() {
		return binds.toData();
	}

	private void loadMaterials() {
		MATERIALS_BY_RL.clear();
		MATERIALS_BY_STATE.clear();

		MaterialData data = readOrCreateMaterials();

		for (Map.Entry<String, Material> entry : data.materials().entrySet()) {
			try {
				ResourceLocation rl = new ResourceLocation(entry.getKey());
				MATERIALS_BY_RL.put(rl, entry.getValue());
			} catch (Exception e) {
				System.err.println("[AFF] Skipping invalid material id: " + entry.getKey());
			}
		}
	}

	private MaterialData readOrCreateMaterials() {
		try {
			if (!Files.exists(MATERIALS_FP)) {
				MaterialData defaults = MaterialData.defaults();
				writeMaterials(defaults);
				System.out.println("[AFF] Created default materials at " + MATERIALS_FP);
				return defaults;
			}

			MaterialData loaded = GsonHelper.read(MATERIALS_FP, MaterialData.class);

			if (loaded == null || loaded.materials() == null || loaded.materials().isEmpty()) {
				System.err.println("[AFF] materials.json is empty or invalid, using defaults.");
				return MaterialData.defaults();
			}

			return loaded;
		} catch (Exception e) {
			System.err.println("[AFF] Failed to read materials.json: " + e.getMessage());
			return MaterialData.defaults();
		}
	}

	private void writeMaterials(MaterialData data) {
		try {
			GsonHelper.write(new JsonFile<MaterialData>() {
				@Override public MaterialData data() { return data; }
				@Override public Path getPath()     { return MATERIALS_FP; }
			});
		} catch (Exception e) {
			System.err.println("[AFF] Failed to write materials.json: " + e.getMessage());
		}
	}

	private void loadBinds() {
		if (binds == null) {
			binds = new KeyBinds();
		}

		binds.register(binds.SHOOT);
		binds.register(binds.AIM);
		binds.register(binds.CHAMBER);
		binds.register(binds.CHANGE_FIREMOD);
		binds.register(binds.CHECK);
		binds.register(binds.RELOAD);
		binds.register(binds.UNLOAD);

		KeyBindsData data = readOrCreateBinds();

		binds.applyData(data);
	}

	private KeyBindsData readOrCreateBinds() {
		try {
			if (!Files.exists(BINDS_FP)) {
				KeyBindsData defaults = binds.toData();
				writeBinds(defaults);
				System.out.println("[AFF] Created default binds at " + BINDS_FP);
				return defaults;
			}

			KeyBindsData loaded = GsonHelper.read(BINDS_FP, KeyBindsData.class);
			if (loaded == null || loaded.binds() == null || loaded.binds().isEmpty()) {
				System.err.println("[AFF] binds.json is empty or invalid, using defaults.");
				return binds.toData();
			}
			return loaded;
		} catch (Exception e) {
			System.err.println("[AFF] Failed to read binds.json: " + e.getMessage());
			return binds.toData();
		}
	}

	private void writeBinds(KeyBindsData data) {
		try {
			GsonHelper.write(new JsonFile<KeyBindsData>() {
				@Override public KeyBindsData data() { return data; }
				@Override public Path        getPath() { return BINDS_FP; }
			});
		} catch (Exception e) {
			System.err.println("[AFF] Failed to write binds.json: " + e.getMessage());
		}
	}

	private void ensureConfigDir() {
		try {
			if (!Files.exists(CONFIG_DIR)) {
				Files.createDirectories(CONFIG_DIR);
			}
		} catch (IOException e) {
			System.err.println("[AFF] Failed to create config dir " + CONFIG_DIR + ": " + e.getMessage());
		}
	}

	@Nullable
	public KeyBinds getBinds() { return binds; }
}