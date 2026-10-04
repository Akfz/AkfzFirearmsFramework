package v.akfz.aff.data.bind;

import v.akfz.aslib.util.json.JsonData;

import java.util.LinkedHashMap;
import java.util.Map;

public record KeyBindsData(Map<String, BindData> binds) implements JsonData {

	public static KeyBindsData empty() {
		return new KeyBindsData(new LinkedHashMap<>());
	}
}