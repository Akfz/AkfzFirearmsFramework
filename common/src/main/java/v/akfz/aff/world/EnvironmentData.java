package v.akfz.aff.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record EnvironmentData(
		boolean isEnclosed,
		Vec3 windVector
) {
	private static final Vec3 OVERWORLD_WIND = new Vec3(0.15, 0.0, 0.05);

	public static EnvironmentData evaluate(ServerLevel level, Vec3 position) {
		BlockPos pos = BlockPos.containing(position);

		boolean isOverworld = level.dimension() == Level.OVERWORLD;
		Vec3 baseWind = isOverworld ? OVERWORLD_WIND : Vec3.ZERO;

		int solidBlocks = 0;
		for (Direction dir : Direction.values()) {
			BlockPos checkPos = pos.relative(dir);
			if (level.getBlockState(checkPos).isSolidRender(level, checkPos)) {
				solidBlocks++;
			}
		}

		int skyLight = level.getBrightness(LightLayer.SKY, pos);

		boolean isEnclosed = solidBlocks >= 5 && skyLight < 5;

		Vec3 finalWind = isEnclosed ? Vec3.ZERO : baseWind;

		return new EnvironmentData(isEnclosed, finalWind);
	}
}