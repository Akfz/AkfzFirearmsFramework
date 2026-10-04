package v.akfz.aff.physics.cpp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import v.akfz.aff.bullet.BulletConfig;
import v.akfz.aff.bullet.BulletManager;
import v.akfz.aff.world.Material;

public class CSnapshot {

	private static final int INIT_BULLETS   = 128;
	private static final int INIT_ENTITIES  = 256;
	private static final int INIT_BLOCKS    = 512;
	private static final int INIT_MATERIALS = 32;

	private static final int RESULT_SIZE = 472;

	private static final int BULLET_SIZE   = 24; // 6 * float
	private static final int CONFIG_SIZE   = 36; // 8 * float + 1 * int
	private static final int ENTITY_SIZE   = 28; // 6 * float + 1 * int
	private static final int BLOCK_SIZE    = 40; // 6 * float + 4 * int
	private static final int MATERIAL_SIZE = 12; // 3 * float
	private static final int WIND_SIZE     = 20; // 3 * float + 1 * int + 1 * float

	public ByteBuffer bulletBuf;
	public ByteBuffer configBuf;
	public ByteBuffer entityBuf;
	public ByteBuffer blockBuf;
	public ByteBuffer materialBuf;
	public ByteBuffer windBuf;
	public ByteBuffer resultBuf;

	public int bulletCount   = 0;
	public int entityCount   = 0;
	public int blockCount    = 0;
	public int materialCount = 0;
	public double deltaTime  = 0.025;
	public boolean isDebug   = BulletManager.DEBUG;

	public CSnapshot() {
		this.bulletBuf   = alloc(INIT_BULLETS   * BULLET_SIZE);
		this.configBuf   = alloc(INIT_BULLETS   * CONFIG_SIZE);
		this.entityBuf   = alloc(INIT_ENTITIES  * ENTITY_SIZE);
		this.blockBuf    = alloc(INIT_BLOCKS    * BLOCK_SIZE);
		this.materialBuf = alloc(INIT_MATERIALS * MATERIAL_SIZE);
		this.windBuf     = alloc(INIT_BULLETS   * WIND_SIZE);
		this.resultBuf   = alloc(INIT_BULLETS   * RESULT_SIZE);
	}

	private static ByteBuffer alloc(int bytes) {
		return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
	}

	private static ByteBuffer ensureCapacity(ByteBuffer buf, int requiredBytes) {
		if (buf.capacity() - buf.position() >= requiredBytes) {
			return buf;
		}
		int newCapacity = Math.max(buf.capacity() * 2, buf.position() + requiredBytes);
		try {
			ByteBuffer newBuf = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder());
			buf.flip();
			newBuf.put(buf);
			return newBuf;
		} catch (OutOfMemoryError e) {
			System.err.println("[AFF_CRITICAL] Direct memory limit reached! "
					+ "Cannot grow buffer to " + newCapacity + " bytes.");
			throw e;
		}
	}

	public void addBullet(float px, float py, float pz, float vx, float vy, float vz) {
		bulletBuf = ensureCapacity(bulletBuf, BULLET_SIZE);

		int neededResult = (bulletCount + 1) * RESULT_SIZE;
		if (resultBuf.capacity() < neededResult) {
			resultBuf = alloc(Math.max(resultBuf.capacity() * 2, neededResult));
		}

		bulletBuf.putFloat(px).putFloat(py).putFloat(pz)
				.putFloat(vx).putFloat(vy).putFloat(vz);
		bulletCount++;
	}

	public void addConfig(BulletConfig config, int shooterEntityId) {
		configBuf = ensureCapacity(configBuf, CONFIG_SIZE);
		configBuf.putFloat((float) config.muzzleVelocity())
				.putFloat((float) config.mass())
				.putFloat((float) config.dragCoefficient())
				.putFloat((float) config.gravityMultiplier())
				.putFloat((float) config.maxSubSteps())
				.putFloat((float) config.penetrationPower())
				.putFloat(config.damageMultiplier())
				.putFloat((float) config.caliber())
				.putInt(shooterEntityId);
	}

	public void addEntity(float minX, float minY, float minZ,
	                      float maxX, float maxY, float maxZ, int id) {
		entityBuf = ensureCapacity(entityBuf, ENTITY_SIZE);
		entityBuf.putFloat(minX).putFloat(minY).putFloat(minZ)
				.putFloat(maxX).putFloat(maxY).putFloat(maxZ)
				.putInt(id);
		entityCount++;
	}

	public void addBlock(float minX, float minY, float minZ,
	                     float maxX, float maxY, float maxZ,
	                     int materialId, int bx, int by, int bz) {
		blockBuf = ensureCapacity(blockBuf, BLOCK_SIZE);
		blockBuf.putFloat(minX).putFloat(minY).putFloat(minZ)
				.putFloat(maxX).putFloat(maxY).putFloat(maxZ)
				.putInt(materialId)
				.putInt(bx).putInt(by).putInt(bz);
		blockCount++;
	}

	public void addMaterial(Material mat) {
		materialBuf = ensureCapacity(materialBuf, MATERIAL_SIZE);
		materialBuf.putFloat((float) mat.density())
				.putFloat((float) mat.hardness())
				.putFloat((float) mat.maxPenetrationJoules());
		materialCount++;
	}

	public void addWind(float wx, float wy, float wz, boolean enclosed, float fluidDragMult) {
		windBuf = ensureCapacity(windBuf, WIND_SIZE);
		windBuf.putFloat(wx).putFloat(wy).putFloat(wz)
				.putInt(enclosed ? 1 : 0)
				.putFloat(fluidDragMult);
	}

	public void prepareForNative() {
		bulletBuf.flip();
		configBuf.flip();
		entityBuf.flip();
		blockBuf.flip();
		materialBuf.flip();
		windBuf.flip();
		resultBuf.clear();
	}

	public void reset() {
		bulletCount   = 0;
		entityCount   = 0;
		blockCount    = 0;
		materialCount = 0;

		bulletBuf.clear();
		configBuf.clear();
		entityBuf.clear();
		blockBuf.clear();
		materialBuf.clear();
		windBuf.clear();
		resultBuf.clear();
	}
}