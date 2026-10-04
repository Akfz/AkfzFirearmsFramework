package v.akfz.aff.event.client;

import v.akfz.aff.data.bind.Bind;
import v.akfz.aslib.event.api.Event;

public class PressedBindEvent extends Event {
	private final Bind pressed;

	public PressedBindEvent(Bind pressed) {
		this.pressed = pressed;
	}

	public Bind getPressed() {
		return pressed;
	}
}
