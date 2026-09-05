package v.akfz.aff.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentSystem {

	private static final Map<ResourceKey<Level>, Vec3> DIMENSION_WINDS = new ConcurrentHashMap<>();

	private static final long CACHE_TTL_MS = 5000;
	private static final int WIND_RAYCAST_DISTANCE = 8;

	private record CachedWind(Vec3 wind, long expires) {}
	private static final Map<ResourceKey<Level>, Map<Long, CachedWind>> WIND_CACHE = new ConcurrentHashMap<>();

	static {
		DIMENSION_WINDS.put(Level.OVERWORLD, new Vec3(15.0, 0.0, 5.0));
	}

	private EnvironmentSystem() {}

	/**
	 * Can overwrite, km/h
	 */
	public static void registerWind(ResourceKey<Level> dimension, Vec3 wind) {
		DIMENSION_WINDS.put(dimension, wind);
	}

	public static Vec3 globalWind(Level level) {
		return DIMENSION_WINDS.getOrDefault(level.dimension(), Vec3.ZERO);
	}

	public static Vec3 windAt(ServerLevel level, Vec3 position) {
		Vec3 baseWind = globalWind(level);
		if (baseWind.equals(Vec3.ZERO)) return Vec3.ZERO;

		BlockPos pos = BlockPos.containing(position);
		long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
		long now = System.currentTimeMillis();

		Map<Long, CachedWind> perLevel = WIND_CACHE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
		CachedWind cached = perLevel.get(chunkKey);

		if (cached != null && cached.expires() > now) {
			return cached.wind();
		}

		Vec3 evaluatedWind = evaluateWind(level, pos, baseWind);
		perLevel.put(chunkKey, new CachedWind(evaluatedWind, now + CACHE_TTL_MS));
		return evaluatedWind;
	}

	public static boolean isEnclosed(ServerLevel level, Vec3 position) {
		Vec3 baseWind = globalWind(level);
		if (baseWind.equals(Vec3.ZERO)) return true;

		BlockPos pos = BlockPos.containing(position);
		long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
		long now = System.currentTimeMillis();

		Map<Long, CachedWind> perLevel = WIND_CACHE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
		CachedWind cached = perLevel.get(chunkKey);

		if (cached != null && cached.expires() > now) {
			return cached.wind().equals(Vec3.ZERO);
		}

		Vec3 evaluatedWind = evaluateWind(level, pos, baseWind);
		perLevel.put(chunkKey, new CachedWind(evaluatedWind, now + CACHE_TTL_MS));

		return evaluatedWind.equals(Vec3.ZERO);
	}

	private static Vec3 evaluateWind(ServerLevel level, BlockPos start, Vec3 baseWind) {
		Vec3 windDir = baseWind.normalize();
		Vec3 step = windDir.scale(-1.0);
		double multiplier = 1.0;
		Vec3 currentPos = new Vec3(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5);

		for (int i = 0; i < WIND_RAYCAST_DISTANCE; i++) {
			currentPos = currentPos.add(step);
			BlockPos checkPos = BlockPos.containing(currentPos);
			if (!level.hasChunkAt(checkPos)) continue;

			BlockState state = level.getBlockState(checkPos);
			if (state.isAir()) continue;

 			if (state.is(BlockTags.IMPERMEABLE)) {
				multiplier = 0.0;
				break;
			}

			VoxelShape shape = state.getCollisionShape(level, checkPos);
			if (shape.isEmpty()) continue;

			double blockVolume = getApproximateVolume(shape);
			if (blockVolume > 0.85) {
				multiplier = 0.0;
				break;
			} else {
				multiplier -= (blockVolume * 0.6);
				if (multiplier <= 0.0) {
					multiplier = 0.0;
					break;
				}
			}
		}
		if (multiplier <= 0.0) return Vec3.ZERO;
		return baseWind.scale(multiplier);
	}

	private static double getApproximateVolume(VoxelShape shape) {
		double volume = 0.0;
		for (AABB aabb : shape.toAabbs()) {
			volume += (aabb.getXsize() * aabb.getYsize() * aabb.getZsize());
		}
		return Math.min(1.0, volume);
	}

	public static void clearCaches() {
		WIND_CACHE.clear();
	}
}