package v.akfz.aff.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import v.akfz.aff.data.bind.Bind;
import v.akfz.aff.data.bind.BindMode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BindManager {

	private static final long DEFAULT_DOUBLE_CLICK_WINDOW = 250;

	private static final AtomicBoolean justScrolledUp = new AtomicBoolean(false);
	private static final AtomicBoolean justScrolledDown = new AtomicBoolean(false);

	private static final Map<String, KeyGroup> GROUPS = new ConcurrentHashMap<>();
	private static final Map<Bind, KeyGroup> BIND_TO_GROUP = new ConcurrentHashMap<>();
	private static final List<Bind> STANDALONE_BINDS = new CopyOnWriteArrayList<>();
	private static final Map<Bind, BindState> STANDALONE_STATES = new ConcurrentHashMap<>();

	private BindManager() {}

	private static class KeyGroup {
		final List<Bind> binds = new CopyOnWriteArrayList<>();
		volatile boolean wasDown = false;
		volatile long firstPressTime = 0;
		volatile long currentPressStart = 0;
		volatile boolean waitingForDoubleClick = false;
		volatile boolean releasePending = false;
		volatile Bind clickWinner = null;
		volatile Bind holdWinner = null;
		volatile boolean toggleState = false;
		volatile boolean holdActivated = false;
		volatile boolean doubleClickFired = false;
	}

	private static class BindState {
		volatile boolean wasDown = false;
		final AtomicBoolean releaseQueued = new AtomicBoolean(false);
		final AtomicBoolean clickQueued = new AtomicBoolean(false);
		final AtomicInteger doubleClickQueued = new AtomicInteger(0);
		volatile long lastClickTime = 0;
		volatile boolean toggleState = false;
		volatile boolean holdActive = false;
		volatile long pressStartTime = 0;
	}

	private static final String STANDALONE_KEY = "";
	private static final Map<Bind, String> BIND_CURRENT_KEY = new ConcurrentHashMap<>();

	public static void register(Bind bind) {
		String key = getGroupKey(bind);
		String mapKey = (key != null) ? key : STANDALONE_KEY;
		String oldMapKey = BIND_CURRENT_KEY.get(bind);

		if (oldMapKey != null && !oldMapKey.equals(mapKey)) {
			String oldKey = oldMapKey.equals(STANDALONE_KEY) ? null : oldMapKey;
			removeFrom(oldKey, bind);
		}

		if (key == null) {
			if (!STANDALONE_BINDS.contains(bind)) {
				STANDALONE_BINDS.add(bind);
				STANDALONE_STATES.put(bind, new BindState());
			}
		} else {
			STANDALONE_BINDS.remove(bind);
			STANDALONE_STATES.remove(bind);

			KeyGroup group = GROUPS.computeIfAbsent(key, k -> new KeyGroup());
			if (!group.binds.contains(bind)) {
				group.binds.add(bind);
			}
			BIND_TO_GROUP.put(bind, group);
		}
		BIND_CURRENT_KEY.put(bind, mapKey);
	}

	private static void removeFrom(String key, Bind bind) {
		if (key == null) {
			STANDALONE_BINDS.remove(bind);
			STANDALONE_STATES.remove(bind);
			return;
		}
		KeyGroup group = GROUPS.get(key);
		if (group != null) {
			group.binds.remove(bind);
			if (group.clickWinner == bind) group.clickWinner = null;
			if (group.holdWinner == bind) group.holdWinner = null;
			if (group.binds.isEmpty()) {
				GROUPS.remove(key);
			}
		}
		BIND_TO_GROUP.remove(bind);
	}

	public static void refreshAll() {
		for (Bind bind : BIND_CURRENT_KEY.keySet()) {
			register(bind);
		}
	}

	public static void update() {
		long now = System.currentTimeMillis();
		for (KeyGroup group : GROUPS.values()) {
			updateGroup(group, now);
		}
		for (Bind bind : STANDALONE_BINDS) {
			updateStandalone(bind, now);
		}
	}

	public static void onScroll(double amount) {
		if (amount > 0) justScrolledUp.set(true);
		else if (amount < 0) justScrolledDown.set(true);
	}

	public static void resetScrollFlags() {
		justScrolledUp.set(false);
		justScrolledDown.set(false);
	}

	public static boolean isTriggered(Bind bind) {
		KeyGroup group = BIND_TO_GROUP.get(bind);
		if (group != null) {
			return isTriggeredInGroup(bind, group);
		}
		return isTriggeredStandalone(bind);
	}

	public static boolean isPressed(Bind bind) {
		return isPressedInternal(bind, getWindow());
	}

	private static boolean isTriggeredInGroup(Bind bind, KeyGroup group) {
		return switch (bind.getMode()) {
			case CLICK,RELEASE,DOUBLE_CLICK -> {
				Bind winner = group.clickWinner;
				if (winner == bind) {
					group.clickWinner = null;
					yield true;
				}
				yield false;
			}
			case HOLD -> group.holdWinner == bind;
			case TOGGLE -> group.toggleState;
		};
	}

	private static boolean isTriggeredStandalone(Bind bind) {
		BindState state = STANDALONE_STATES.get(bind);
		if (state == null) return false;

		return switch (bind.getMode()) {
			case CLICK -> state.clickQueued.getAndSet(false);
			case RELEASE -> state.releaseQueued.getAndSet(false);
			case DOUBLE_CLICK -> state.doubleClickQueued.getAndDecrement() > 0;
			case HOLD -> state.holdActive;
			case TOGGLE -> state.toggleState;
		};
	}

	private static void updateGroup(KeyGroup group, long now) {
		boolean down = isGroupDown(group);
		long window = getMaxWindow(group);

		if (down && !group.wasDown) {
			group.holdActivated = false;
			group.doubleClickFired = false;
			group.currentPressStart = now;

			if (group.waitingForDoubleClick && (now - group.firstPressTime) <= window) {
				Bind dbl = findBind(group, BindMode.DOUBLE_CLICK);
				if (dbl != null) group.clickWinner = dbl;

				group.waitingForDoubleClick = false;
				group.releasePending = false;
				group.doubleClickFired = true;
			} else {
				group.firstPressTime = now;
				Bind dbl = findBind(group, BindMode.DOUBLE_CLICK);

				if (dbl != null) {
					group.waitingForDoubleClick = true;
					group.releasePending = false;
				} else {
					Bind click = findBind(group, BindMode.CLICK);
					if (click != null) group.clickWinner = click;
				}

				Bind toggle = findBind(group, BindMode.TOGGLE);
				if (toggle != null) group.toggleState = !group.toggleState;
			}
		}

		if (down && group.wasDown) {
			long holdDuration = now - group.currentPressStart;
			Bind hold = findBind(group, BindMode.HOLD);
			if (hold != null && hold.getHoldActivationTime() >= 0) {
				if (holdDuration >= hold.getHoldActivationTime()) {
					group.holdWinner = hold;
					group.holdActivated = true;
					group.waitingForDoubleClick = false;
					group.releasePending = false;
				}
			}
		}

		if (!down && group.wasDown) {
			group.holdWinner = null;

			if (group.waitingForDoubleClick) {
				group.releasePending = true;
			} else if (!group.holdActivated && !group.doubleClickFired) {
				Bind release = findBind(group, BindMode.RELEASE);
				if (release != null) group.clickWinner = release;
			}
		}

		if (group.waitingForDoubleClick && !down && (now - group.firstPressTime) > window) {
			if (group.releasePending) {
				Bind release = findBind(group, BindMode.RELEASE);
				if (release != null) group.clickWinner = release;
			}
			group.waitingForDoubleClick = false;
			group.releasePending = false;
		}

		group.wasDown = down;
	}

	private static void updateStandalone(Bind bind, long now) {
		BindState state = STANDALONE_STATES.get(bind);
		if (state == null) return;

		boolean down = isPressedInternal(bind, getWindow());

		if (down && !state.wasDown) {
			long lastClick = state.lastClickTime;
			if (bind.getMode() == BindMode.DOUBLE_CLICK
					&& lastClick > 0
					&& (now - lastClick) <= bind.getDoubleClickWindow()) {
				state.doubleClickQueued.incrementAndGet();
			} else if (bind.getMode() == BindMode.CLICK) {
				state.clickQueued.set(true);
			}
			state.lastClickTime = now;
			state.pressStartTime = now;
		}

		if (down && state.wasDown) {
			if (bind.getMode() == BindMode.HOLD) {
				long holdDuration = now - state.pressStartTime;
				state.holdActive = bind.getHoldActivationTime() >= 0
						&& holdDuration >= bind.getHoldActivationTime();
			}
		} else if (!down) {
			if (bind.getMode() == BindMode.HOLD) {
				state.holdActive = false;
			}
		}

		if (!down && state.wasDown) {
			if (bind.getMode() == BindMode.RELEASE) {
				state.releaseQueued.set(true);
			}
			if (bind.getMode() == BindMode.TOGGLE) {
				state.toggleState = !state.toggleState;
			}
		}

		state.wasDown = down;
	}

	private static String getGroupKey(Bind bind) {
		if (bind.getKeyboardBinds() != null && bind.getKeyboardBinds().length > 0) {
			return "K" + bind.getKeyboardBinds()[0];
		}
		if (bind.getMouseBinds() != null && bind.getMouseBinds().length > 0) {
			return "M" + bind.getMouseBinds()[0];
		}
		return null;
	}

	private static boolean isGroupDown(KeyGroup group) {
		if (group.binds.isEmpty()) return false;
		return isPressedInternal(group.binds.get(0), getWindow());
	}

	private static long getMaxWindow(KeyGroup group) {
		long max = DEFAULT_DOUBLE_CLICK_WINDOW;
		for (Bind b : group.binds) {
			if (b.getDoubleClickWindow() > max) {
				max = b.getDoubleClickWindow();
			}
		}
		return max;
	}

	private static Bind findBind(KeyGroup group, BindMode mode) {
		for (Bind b : group.binds) {
			if (b.getMode() == mode) return b;
		}
		return null;
	}

	private static boolean isPressedInternal(Bind bind, long window) {
		boolean hasAnyKeys = (bind.getKeyboardBinds() != null && bind.getKeyboardBinds().length > 0)
				|| (bind.getMouseBinds() != null && bind.getMouseBinds().length > 0)
				|| bind.isWheelUp()
				|| bind.isWheelDown();

		if (!hasAnyKeys) return false;

		if (bind.getKeyboardBinds() != null) {
			for (int key : bind.getKeyboardBinds()) {
				if (GLFW.glfwGetKey(window, key) != GLFW.GLFW_PRESS) return false;
			}
		}

		if (bind.getMouseBinds() != null) {
			for (int btn : bind.getMouseBinds()) {
				if (GLFW.glfwGetMouseButton(window, btn) != GLFW.GLFW_PRESS) return false;
			}
		}

		if (bind.isWheelUp() && !justScrolledUp.get()) return false;
		if (bind.isWheelDown() && !justScrolledDown.get()) return false;

		return true;
	}

	private static long getWindow() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getWindow().getWindow();
	}
}