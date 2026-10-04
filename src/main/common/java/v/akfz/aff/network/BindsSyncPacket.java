package v.akfz.aff.network;

import v.akfz.aslib.network.annotation.NetworkPacket;
import v.akfz.aslib.network.api.Packet;
import v.akfz.aslib.network.api.PacketDecoder;
import v.akfz.aslib.network.api.PacketEncoder;

import java.util.ArrayList;
import java.util.List;

@NetworkPacket("aff:gis")
public class BindsSyncPacket implements Packet {
	public final List<BindState> changes;
	public record BindState(String interactionId, boolean pressed, boolean spam) {}

	public BindsSyncPacket(List<BindState> states) {
		this.changes = states;
	}

	public PacketEncoder<BindsSyncPacket> encoder() {
		return (packet,buf) -> {
			buf.writeInt(packet.changes.size());
			for (BindState state : packet.changes) {
				buf.writeUtf(state.interactionId);
				buf.writeBoolean(state.pressed);
				buf.writeBoolean(state.spam);
			}
		};
	}

	public PacketDecoder<BindsSyncPacket> decoder() {
		return buf -> {
			int size = buf.readInt();
			List<BindState> states = new ArrayList<>(size);
			for (int i =0; i < size; i++) {
				states.add(new BindState(
						buf.readUtf(),buf.readBoolean(),buf.readBoolean()
				));
			}
			return new BindsSyncPacket(states);
		};
	}
}
