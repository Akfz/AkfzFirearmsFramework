package v.akfz.aff;

import net.minecraft.resources.ResourceLocation;
import v.akfz.aff.bullet.BulletManager;
import v.akfz.aff.data.bind.KeyBinds;
import v.akfz.aff.data.config.MaterialData;
import v.akfz.aff.world.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoaderConfigs {
	public static final LoaderConfigs INSTANCE = new LoaderConfigs();
	private boolean ready = false;
	private LoaderConfigs() {}

	public KeyBinds binds;
	private MaterialData materials;

	private final List<Material> MATERIALS_BY_INDEX = new ArrayList<>();
	private final Map<ResourceLocation, Integer> ID_TO_INDEX = new HashMap<>();

	public Material getMaterialByIntId(int id) {
		if (id >= 0 && id < MATERIALS_BY_INDEX.size()) {
			return MATERIALS_BY_INDEX.get(id);
		}
		if (BulletManager.DEBUG) System.out.println("[AFF_DEBUG_JAVA] Dont find material : " + id);
		return new Material(99999, 99999.0, 99999999, false, true);
	}

	public int getMaterialId(Material material) {
		int id = MATERIALS_BY_INDEX.indexOf(material);
		if (id == -1) {
			id = MATERIALS_BY_INDEX.size();
			MATERIALS_BY_INDEX.add(material);
		}
		return id;
	}

	private void register(ResourceLocation id, Material material) {
		int index = MATERIALS_BY_INDEX.size();
		MATERIALS_BY_INDEX.add(material);
		ID_TO_INDEX.put(id, index);
	}

	public Material getMaterialByRL(ResourceLocation rl) {
		Integer index = ID_TO_INDEX.get(rl);
		if (index != null && index >= 0 && index < MATERIALS_BY_INDEX.size()) {
			return MATERIALS_BY_INDEX.get(index);
		}

		if (materials != null && materials.materials() != null) {
			Material mat = materials.materials().get(rl);
			if (mat != null) {
				register(rl, mat);
				return mat;
			}
		}

		if (BulletManager.DEBUG) System.out.println("[AFF_DEBUG_JAVA] Dont find material : " + rl);
		return new Material(99999, 99999.0, 99999999, false, true);
	}

	//TODO реальная загрузка
	public void load() {
		binds = new KeyBinds();
		materials = MaterialData.generate();

		if (materials != null && materials.materials() != null) {
			for (Map.Entry<ResourceLocation, Material> entry : materials.materials().entrySet()) {
				register(entry.getKey(), entry.getValue());
			}
		}

		binds.customBinds.add(binds.AIM);
		binds.customBinds.add(binds.CHAMBER);
		binds.customBinds.add(binds.CHANGE_FIREMOD);
		binds.customBinds.add(binds.CHECK);
		binds.customBinds.add(binds.RELOAD);
		binds.customBinds.add(binds.UNLOAD);
		binds.customBinds.add(binds.SHOOT);

		ready = true;
		System.out.println("[AFF] LoaderConfigs loaded. Materials registered: " + MATERIALS_BY_INDEX.size());
	}

	public boolean isReady() {
		return this.ready;
	}

	public void save() {
		// TODO
	}
}