package v.akfz.aff.data.config;

import v.akfz.aff.data.gun.GunData;
import v.akfz.aslib.util.json.JsonData;

import java.util.HashMap;
import java.util.Map;

public record GunDataCollection(Map<String,GunData> guns) implements JsonData {

	public static GunDataCollection empty() {
		return new GunDataCollection(new HashMap<>());
	}
}
