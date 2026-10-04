package v.akfz.aff.data.bind;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeyBinds {

	public List<Bind> customBinds = new ArrayList<>();

	public final Bind SHOOT           = new Bind("", "aff:shoot").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_LEFT);
	public final Bind AIM             = new Bind("", "aff:aim").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	public final Bind CHAMBER         = new Bind("", "aff:chamber").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
	public final Bind CHANGE_FIREMOD  = new Bind("", "aff:changefiremod").setMode(BindMode.CLICK).setKeyboardBinds(GLFW.GLFW_KEY_B);
	public final Bind RELOAD          = new Bind("", "aff:reload").setMode(BindMode.RELEASE).setKeyboardBinds(GLFW.GLFW_KEY_R);
	public final Bind UNLOAD          = new Bind("", "aff:unload").setMode(BindMode.DOUBLE_CLICK).setKeyboardBinds(GLFW.GLFW_KEY_R).setDoubleClickWindow(150);
	public final Bind CHECK           = new Bind("", "aff:check").setMode(BindMode.HOLD).setKeyboardBinds(GLFW.GLFW_KEY_R).setHoldActivationTime(1000);

	private KeyBindsData lastLoadedData = null;

	public void register(Bind bind) {
		if (bind == null) return;
		if (!customBinds.contains(bind)) {
			customBinds.add(bind);
		}
		if (lastLoadedData != null) {
			applyOne(bind, lastLoadedData);
		}
	}

	public KeyBindsData toData() {
		Map<String, BindData> result = new LinkedHashMap<>();

		collect(SHOOT,          result);
		collect(AIM,            result);
		collect(CHAMBER,        result);
		collect(CHANGE_FIREMOD, result);
		collect(RELOAD,         result);
		collect(UNLOAD,         result);
		collect(CHECK,          result);
		for (Bind b : customBinds) collect(b, result);

		return new KeyBindsData(result);
	}

	public void applyData(KeyBindsData data) {
		this.lastLoadedData = data;
		if (data == null || data.binds() == null) return;

		applyOne(SHOOT,          data);
		applyOne(AIM,            data);
		applyOne(CHAMBER,        data);
		applyOne(CHANGE_FIREMOD, data);
		applyOne(RELOAD,         data);
		applyOne(UNLOAD,         data);
		applyOne(CHECK,          data);
		for (Bind b : customBinds) applyOne(b, data);
	}

	private static void collect(Bind bind, Map<String, BindData> out) {
		if (bind == null) return;
		String id = bind.getKeyInteractionId();
		if (id == null) return;
		out.putIfAbsent(id, bind.toData());
	}

	private static void applyOne(Bind bind, KeyBindsData data) {
		if (bind == null) return;
		String id = bind.getKeyInteractionId();
		if (id == null) return;
		BindData bd = data.binds().get(id);
		if (bd != null) bind.applyData(bd);
	}
}