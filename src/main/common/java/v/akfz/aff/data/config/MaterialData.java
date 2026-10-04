package v.akfz.aff.data.config;

import v.akfz.aff.world.Material;
import v.akfz.aslib.util.json.JsonData;

import java.util.LinkedHashMap;
import java.util.Map;

public record MaterialData(Map<String, Material> materials) implements JsonData {

	public static MaterialData empty() {
		return new MaterialData(new LinkedHashMap<>());
	}

	public static MaterialData defaults() {
		Map<String, Material> defaults = new LinkedHashMap<>();

		defaults.put("minecraft:iron_block",
				new Material(7.8, 15.0, 8000.0, false, false));

		defaults.put("minecraft:diamond_block",
				new Material(5.0, 25.0, 15000.0, false, false));

		defaults.put("minecraft:oak_planks",
				new Material(0.7, 2.0, 600.0, false, true));

		defaults.put("minecraft:stone",
				new Material(2.5, 8.0, 2500.0, false, true));

		defaults.put("minecraft:dirt",
				new Material(1.5, 1.0, 200.0, false, true));

		defaults.put("minecraft:sand",
				new Material(1.6, 0.8, 150.0, false, true));

		defaults.put("minecraft:glass",
				new Material(2.5, 0.5, 80.0, true, false));

		defaults.put("minecraft:obsidian",
				new Material(2.4, 40.0, 50000.0, false, false));

		return new MaterialData(defaults);
	}
}