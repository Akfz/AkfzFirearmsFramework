package v.akfz.aff.event.client;

import v.akfz.aslib.event.api.Event;

public class RecoilReceiverEvent extends Event {
	private final float strength;
	public RecoilReceiverEvent(float str) {
		this.strength = str;
	}

	public float getStrength() {
		return strength;
	}
}
