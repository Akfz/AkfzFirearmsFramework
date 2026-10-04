package v.akfz.aff.event;

import v.akfz.aff.ServerBindManager;
import v.akfz.aff.client.BindManager;
import v.akfz.aff.client.KeysInteractionSystem;
import v.akfz.aslib.event.api.Listener;
import v.akfz.aslib.event.api.Subscribe;
import v.akfz.aslib.event.impl.TickUpdater;

public class TickListener implements Listener {
	@Subscribe
	public void tick(TickUpdater event) {
		if (event.client) {
			BindManager.update();
			KeysInteractionSystem.tick();
		} else {
			ServerBindManager.tick();
		}
	}
}
