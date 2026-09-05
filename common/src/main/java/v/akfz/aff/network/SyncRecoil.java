package v.akfz.aff.network;

import v.akfz.aslib.network.annotation.NetworkPacket;
import v.akfz.aslib.network.api.Packet;
import v.akfz.aslib.network.api.PacketDecoder;
import v.akfz.aslib.network.api.PacketEncoder;

@NetworkPacket("aff:recoil")
public class SyncRecoil implements Packet {
	public final float strength;

	public SyncRecoil(float strength) {
		this.strength = strength;
	}

	@Override
	public PacketEncoder<? extends Packet> encoder() {
		return (PacketEncoder<Packet>) (packet,buf) -> buf.writeFloat(((SyncRecoil) packet).strength);
	}

	@Override
	public PacketDecoder<? extends Packet> decoder() {
		return (PacketDecoder<Packet>) buf -> new SyncRecoil(buf.readFloat());
	}
}