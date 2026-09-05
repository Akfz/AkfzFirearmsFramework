package v.akfz.aff.data.config;

import net.minecraft.resources.ResourceLocation;
import v.akfz.aff.world.Material;
import v.akfz.aslib.util.json.JsonData;

import java.util.Map;

public record MaterialData(Map<ResourceLocation,Material> materials) implements JsonData {
	public static MaterialData generate() {
		return new MaterialData(Map.of(new ResourceLocation("minecraft:iron_block"),new Material("metal", 7.8, 15.0, 2000.0)));
	}
}
