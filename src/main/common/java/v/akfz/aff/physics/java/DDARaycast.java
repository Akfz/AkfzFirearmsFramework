package v.akfz.aff.physics.java;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class DDARaycast {

	private DDARaycast() {}

	public static boolean raycast(ServerLevel level,
	                              double x0, double y0, double z0,
	                              double x1, double y1, double z1,
	                              double[] outHitPos, int[] outBlockPos, Direction[] outFace) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double dz = z1 - z0;

		int i = floor(x0); int j = floor(y0); int k = floor(z0);
		int endI = floor(x1); int endJ = floor(y1); int endK = floor(z1);

		int stepX = (int) Math.signum(dx);
		int stepY = (int) Math.signum(dy);
		int stepZ = (int) Math.signum(dz);

		double tDeltaX = dx != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
		double tDeltaY = dy != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
		double tDeltaZ = dz != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

		double tMaxX = computeTMax(x0, i, stepX, tDeltaX);
		double tMaxY = computeTMax(y0, j, stepY, tDeltaY);
		double tMaxZ = computeTMax(z0, k, stepZ, tDeltaZ);

		Direction lastFace = Direction.UP;
		double lastT = 0.0;

		int maxSteps = Math.abs(endI - i) + Math.abs(endJ - j) + Math.abs(endK - k) + 2;

		for (int step = 0; step <= maxSteps; step++) {
			if (level.hasChunkAt(new BlockPos(i, j, k))) {
				BlockPos pos = new BlockPos(i, j, k);
				BlockState state = level.getBlockState(pos);

				if (!state.isAir()) {
					VoxelShape shape = state.getCollisionShape(level, pos);

					if (!shape.isEmpty()) {
						List<AABB> aabbs = shape.toAabbs();
						double bestT = Double.MAX_VALUE;

						for (AABB aabb : aabbs) {
							AABB worldAABB = aabb.move(i, j, k);
							double t = rayAABB(x0, y0, z0, dx, dy, dz,
									worldAABB.minX, worldAABB.minY, worldAABB.minZ,
									worldAABB.maxX, worldAABB.maxY, worldAABB.maxZ);

							if (t >= 0.0 && t <= 1.0 && t < bestT) {
								bestT = t;
							}
						}

						if (bestT < Double.MAX_VALUE) {
							outHitPos[0] = x0 + dx * bestT;
							outHitPos[1] = y0 + dy * bestT;
							outHitPos[2] = z0 + dz * bestT;
							outBlockPos[0] = i;
							outBlockPos[1] = j;
							outBlockPos[2] = k;
							outFace[0] = lastFace;
							return true;
						}
					}
				}
			}

			if (i == endI && j == endJ && k == endK) {
				return false;
			}

			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					lastT = tMaxX;
					if (lastT > 1.0) return false;
					lastFace = stepX > 0 ? Direction.WEST : Direction.EAST;
					i += stepX;
					tMaxX += tDeltaX;
				} else {
					lastT = tMaxZ;
					if (lastT > 1.0) return false;
					lastFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
					k += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else {
				if (tMaxY < tMaxZ) {
					lastT = tMaxY;
					if (lastT > 1.0) return false;
					lastFace = stepY > 0 ? Direction.DOWN : Direction.UP;
					j += stepY;
					tMaxY += tDeltaY;
				} else {
					lastT = tMaxZ;
					if (lastT > 1.0) return false;
					lastFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
					k += stepZ;
					tMaxZ += tDeltaZ;
				}
			}
		}
		return false;
	}

	private static double rayAABB(
			double ox, double oy, double oz,
			double dx, double dy, double dz,
			double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {

		double tmin = Double.NEGATIVE_INFINITY;
		double tmax = Double.POSITIVE_INFINITY;

		if (Math.abs(dx) > 1e-10) {
			double t1 = (minX - ox) / dx;
			double t2 = (maxX - ox) / dx;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (ox < minX || ox > maxX) return -1;
		}

		if (Math.abs(dy) > 1e-10) {
			double t1 = (minY - oy) / dy;
			double t2 = (maxY - oy) / dy;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (oy < minY || oy > maxY) return -1;
		}

		if (Math.abs(dz) > 1e-10) {
			double t1 = (minZ - oz) / dz;
			double t2 = (maxZ - oz) / dz;
			if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
			if (t1 > tmin) tmin = t1;
			if (t2 < tmax) tmax = t2;
			if (tmin > tmax) return -1;
		} else {
			if (oz < minZ || oz > maxZ) return -1;
		}

		if (tmax < 0) return -1;
		return Math.max(tmin, 0.0);
	}

	private static double computeTMax(double start, int cell, int step, double tDelta) {
		if (step > 0) {
			return ((cell + 1.0) - start) * tDelta;
		} else if (step < 0) {
			return (start - cell) * tDelta;
		}
		return Double.MAX_VALUE;
	}

	private static int floor(double v) {
		int i = (int) v;
		return v < i ? i - 1 : i;
	}
}