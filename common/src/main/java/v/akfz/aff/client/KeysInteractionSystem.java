package v.akfz.aff.client;

import v.akfz.aff.LoaderConfigs;
import v.akfz.aff.data.bind.Bind;
import v.akfz.aff.data.bind.BindMode;
import v.akfz.aff.event.client.PressedBindEvent;
import v.akfz.aff.network.BindsSyncPacket;
import v.akfz.aslib.AsLib;
import v.akfz.aslib.network.AsLibNetworking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeysInteractionSystem {
	private static final Map<Bind, Boolean> lastTrigger = new HashMap<>();

	public static void tick() {
		List<BindsSyncPacket.BindState> changes = new ArrayList<>();

		for (Bind bind : LoaderConfigs.INSTANCE.binds.customBinds) {
			if (bind.getKeyInteractionId() == null) continue;
			String id = bind.getKeyInteractionId();

			if (bind.getMode() == BindMode.HOLD) {
				boolean nowActive = BindManager.isTriggered(bind);
				boolean last = lastTrigger.getOrDefault(bind, false);

				if (nowActive) {
					changes.add(new BindsSyncPacket.BindState(id, true, true));
				} else if (last) {
					changes.add(new BindsSyncPacket.BindState(id, false, false));
				}

				lastTrigger.put(bind, nowActive);
			} else {
				boolean current = BindManager.isTriggered(bind);
				if (current) {
					AsLib.EVENT_BUS.post(new PressedBindEvent(bind));
					changes.add(new BindsSyncPacket.BindState(id, true, false));
				}
			}
		}

		if (!changes.isEmpty()) {
			AsLibNetworking.SENDER.sendToServer(new BindsSyncPacket(changes));
		}
	}
}