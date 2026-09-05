package v.akfz.aff.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentSystem {
	private static final Vec3 OVERWORLD_WIND = new Vec3(0.15, 0.0, 0.05);
	private static final long CACHE_TTL_MS = 5000;

	private record Cached(boolean enclosed, long expires) {}

	private static final Map<ResourceKey<Level>, Map<Long, Cached>> CACHE = new ConcurrentHashMap<>();

	private EnvironmentSystem() {}

	public static Vec3 globalWind(Level level) {
		return level.dimension() == Level.OVERWORLD ? OVERWORLD_WIND : Vec3.ZERO;
	}

	/** MAIN THREAD ONLY. */
	public static boolean isEnclosed(ServerLevel level, Vec3 position) {
		Map<Long, Cached> perLevel = CACHE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
		BlockPos pos = BlockPos.containing(position);
		long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

		long now = System.currentTimeMillis();
		Cached cached = perLevel.get(chunkKey);
		if (cached != null && cached.expires() > now) return cached.enclosed();

		boolean enclosed = evaluate(level, pos);
		perLevel.put(chunkKey, new Cached(enclosed, now + CACHE_TTL_MS));
		return enclosed;
	}

	/** MAIN THREAD ONLY. */
	public static Vec3 windAt(ServerLevel level, Vec3 position) {
		return isEnclosed(level, position) ? Vec3.ZERO : globalWind(level);
	}

	private static boolean evaluate(ServerLevel level, BlockPos pos) {
		int solid = 0;
		for (Direction dir : Direction.values()) {
			BlockPos n = pos.relative(dir);
			if (level.getBlockState(n).isSolidRender(level, n)) solid++;
		}
		int skyLight = level.getBrightness(LightLayer.SKY, pos);
		return solid >= 5 && skyLight < 5;
	}

	public static void clearCaches() {
		CACHE.clear();
	}
}