package v.akfz.aff.data.config;

import net.minecraft.resources.ResourceLocation;
import v.akfz.aff.world.Material;
import v.akfz.aslib.util.json.JsonData;
import java.util.Map;

public record MaterialData(Map<ResourceLocation, Material> materials) implements JsonData {
	public static MaterialData generate() {
		return new MaterialData(Map.ofEntries(
				Map.entry(new ResourceLocation("minecraft:iron_block"),
						new Material(7.8, 15.0, 8000.0, false, false)),

				Map.entry(new ResourceLocation("minecraft:diamond_block"),
						new Material(5.0, 25.0, 15000.0, false, false)),

				Map.entry(new ResourceLocation("minecraft:oak_planks"),
						new Material( 0.7, 2.0, 600.0, true, true)),

				Map.entry(new ResourceLocation("minecraft:stone"), //2500
						new Material( 2.5, 8.0, 2500.0, true, true))
		));
	}
}