package v.akfz.aff;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import v.akfz.aff.event.TickListener;
import v.akfz.aff.event.client.RecoilReceiverEvent;
import v.akfz.aff.event.registry.PreLoadRegistryEvent;
import v.akfz.aff.item.GItem;
import v.akfz.aff.network.BindsSyncPacket;
import v.akfz.aff.network.SyncRecoil;
import v.akfz.aslib.AsLib;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.aslib.network.api.PacketHandler;
import v.akfz.db.generator.GenerateInitializer;

@GenerateInitializer(modId = "aff")
public class AFFramework {

	public void init() {
		AsLibNetworking.REGISTRY.register(new SyncRecoil(-1),new PacketHandler<>() {
			@Override
			public void handle(SyncRecoil packet) {
				AsLib.EVENT_BUS.post(new RecoilReceiverEvent(packet.strength));
			}
		});

		AsLibNetworking.REGISTRY.register(new BindsSyncPacket(null), new PacketHandler<>() {
			@Override
			public void handle(BindsSyncPacket packet, ServerPlayer player) {
				if (packet.changes == null || packet.changes.isEmpty()) return;
				for (BindsSyncPacket.BindState state : packet.changes) {
					ServerBindManager.updateBindState(player.getUUID(), state.interactionId(), state.pressed(), state.spam());
					if (!state.spam()) {
						ItemStack handI = player.getMainHandItem();
						if (handI.getItem() instanceof GItem gi) {
							gi.useByKeyEvent(state.interactionId(), player, state.pressed());
						}
					}
				}
			}
		});
		AsLib.EVENT_BUS.post(new PreLoadRegistryEvent());
		LoaderConfigs.INSTANCE.load();
		AsLib.EVENT_BUS.register(new TickListener());
	}
}