package v.akfz.aff.data.bind;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import v.akfz.aff.client.BindManager;

public final class Bind {
	@Expose private int[] mouseBinds = new int[0];
	@Expose private int[] keyboardBinds = new int[0];

	@Expose private boolean wheelUp = false;
	@Expose private boolean wheelDown = false;

	@Expose private BindMode mode = BindMode.CLICK;

	@Expose private long holdActivationTime = 0;
	@Expose private long doubleClickWindow = 250;

	private boolean dirty = false;
	private final String name;
	@Nullable private final String keyInteractionId;

	public Bind(String name, @Nullable String keyInteractionId) {
		BindManager.register(this);
		this.name = name;
		this.keyInteractionId = keyInteractionId;
	}

	public boolean isReady() {
		return BindManager.isTriggered(this);
	}
	public String getName() {
		return Component.translatable(name).getString();
	}
	@Nullable public String getKeyInteractionId() {
		return keyInteractionId;
	}

	public int[] getMouseBinds() { return mouseBinds; }
	public int[] getKeyboardBinds() { return keyboardBinds; }
	public boolean isWheelUp() { return wheelUp; }
	public boolean isWheelDown() { return wheelDown; }
	public BindMode getMode() { return mode; }
	public boolean isDirty() { return dirty; }
	public long getHoldActivationTime() { return holdActivationTime; }
	public long getDoubleClickWindow() { return doubleClickWindow; }

	public void setDirty(boolean dirty) { this.dirty = dirty; }

	public void markClean() {
		this.dirty = false;
	}

	public void markDirty() { this.dirty = true; }

	public Bind setMouseBinds(int... mouseBinds) {
		this.mouseBinds = mouseBinds;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}

	public Bind setKeyboardBinds(int... keyboardBinds) {
		this.keyboardBinds = keyboardBinds;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}

	public Bind setWheelUp(boolean b) {
		this.wheelUp = b;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}

	public Bind setWheelDown(boolean b) {
		this.wheelDown = b;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}

	public Bind setMode(BindMode mode) {
		this.mode = mode;
		this.dirty = true;
		return this;
	}

	public Bind setHoldActivationTime(long ms) {
		this.holdActivationTime = ms;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}

	public Bind setDoubleClickWindow(long ms) {
		this.doubleClickWindow = ms;
		this.dirty = true;
		BindManager.register(this);
		return this;
	}
}