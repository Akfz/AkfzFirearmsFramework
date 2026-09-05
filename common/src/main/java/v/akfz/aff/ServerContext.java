package v.akfz.aff;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class ServerContext {
	private static volatile MinecraftServer server;

	private ServerContext() {}

	public static void set(@Nullable MinecraftServer server) {
		ServerContext.server = server;
	}

	@Nullable
	public static MinecraftServer get() {
		return server;
	}

	public static boolean isAvailable() {
		return server != null;
	}
}