package v.akfz.aff.data.bind;

import com.google.gson.annotations.Expose;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

//TODO имена для lang
public class KeyBinds {
	@Expose public List<Bind> customBinds = new ArrayList<>();

	@Expose public final Bind SHOOT = new Bind("","aff:shoot").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_LEFT);
	@Expose public final Bind AIM = new Bind("","aff:aim").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	@Expose public final Bind CHAMBER = new Bind("","aff:chamber").setMode(BindMode.HOLD).setMouseBinds(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

	@Expose public final Bind CHANGE_FIREMOD = new Bind("","aff:changefiremod").setMode(BindMode.CLICK).setKeyboardBinds(GLFW.GLFW_KEY_B);

	@Expose public final Bind RELOAD = new Bind("","aff:reload").setMode(BindMode.RELEASE).setKeyboardBinds(GLFW.GLFW_KEY_R);
	@Expose public final Bind UNLOAD = new Bind("","aff:unload").setMode(BindMode.DOUBLE_CLICK).setKeyboardBinds(GLFW.GLFW_KEY_R).setDoubleClickWindow(150);
	@Expose public final Bind CHECK = new Bind("","aff:check").setMode(BindMode.HOLD).setKeyboardBinds(GLFW.GLFW_KEY_R).setHoldActivationTime(1000);
}
