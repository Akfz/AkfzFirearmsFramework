package v.akfz.aff;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import v.akfz.aff.item.GItem;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerBindManager {
	private static final Map<UUID, Set<String>> HELD_BINDS = new ConcurrentHashMap<>();

	public static void updateBindState(UUID playerUuid, String interactionId, boolean pressed, boolean spam) {
		if (spam && pressed) {
			HELD_BINDS.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet()).add(interactionId);
		} else if (!pressed) {
			Set<String> binds = HELD_BINDS.get(playerUuid);
			if (binds != null) {
				binds.remove(interactionId);
				if (binds.isEmpty()) {
					HELD_BINDS.remove(playerUuid);
				}
			}
		}
	}

	public static void tick() {
		MinecraftServer server = ServerContext.get();
		if (server == null) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Set<String> held = HELD_BINDS.get(player.getUUID());
			if (held == null || held.isEmpty()) continue;

			ItemStack stack = player.getMainHandItem();
			if (stack.getItem() instanceof GItem gItem) {
				for (String bindId : held) {
					gItem.useByKeyEvent(bindId, player, true);
				}
			}
		}

		Set<UUID> onlineUuids = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			onlineUuids.add(player.getUUID());
		}
		HELD_BINDS.keySet().retainAll(onlineUuids);
	}

	public static void clear() {
		HELD_BINDS.clear();
	}
}