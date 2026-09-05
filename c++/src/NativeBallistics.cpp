#include "java/v_akfz_aff_physics_cpp_NativeBallistics.h"
#include "aff_types.h"
#include <cmath>
#include <algorithm>
#include <random>
#include <cstdio>

namespace {
    struct FieldCache {
        jfieldID bulletCount, entityCount, blockCount, materialCount, deltaTime, isDebug;
        jfieldID bulletBuf, configBuf, entityBuf, blockBuf, materialBuf, windBuf, resultBuf;
        bool initialized = false;

        void init(JNIEnv* env, jclass clazz) {
            if (initialized) return;
            bulletCount    = env->GetFieldID(clazz, "bulletCount",    "I");
            entityCount    = env->GetFieldID(clazz, "entityCount",    "I");
            blockCount     = env->GetFieldID(clazz, "blockCount",     "I");
            materialCount  = env->GetFieldID(clazz, "materialCount",  "I");
            deltaTime      = env->GetFieldID(clazz, "deltaTime",      "D");
            isDebug        = env->GetFieldID(clazz, "isDebug",        "Z");
            bulletBuf      = env->GetFieldID(clazz, "bulletBuf",      "Ljava/nio/ByteBuffer;");
            configBuf      = env->GetFieldID(clazz, "configBuf",      "Ljava/nio/ByteBuffer;");
            entityBuf      = env->GetFieldID(clazz, "entityBuf",      "Ljava/nio/ByteBuffer;");
            blockBuf       = env->GetFieldID(clazz, "blockBuf",       "Ljava/nio/ByteBuffer;");
            materialBuf    = env->GetFieldID(clazz, "materialBuf",    "Ljava/nio/ByteBuffer;");
            windBuf        = env->GetFieldID(clazz, "windBuf",        "Ljava/nio/ByteBuffer;");
            resultBuf      = env->GetFieldID(clazz, "resultBuf",      "Ljava/nio/ByteBuffer;");
            initialized = true;
        }
        void reset() {
            initialized = false;
            bulletCount = entityCount = blockCount = materialCount = deltaTime = isDebug = nullptr;
            bulletBuf = configBuf = entityBuf = blockBuf = materialBuf = windBuf = resultBuf = nullptr;
        }
    };

    FieldCache g_cache;
    std::mt19937 g_rng(std::random_device{}());

    template <typename T>
    T* getDirectBuffer(JNIEnv* env, jobject obj, jfieldID field) {
        jobject buf = env->GetObjectField(obj, field);
        if (!buf) return nullptr;
        return static_cast<T*>(env->GetDirectBufferAddress(buf));
    }

    bool extractFrameData(JNIEnv* env, jobject snapshotObj, aff::FrameData& out) {
        if (!snapshotObj) return false;
        jclass clazz = env->GetObjectClass(snapshotObj);
        g_cache.init(env, clazz);

        out.bulletCount   = env->GetIntField(snapshotObj, g_cache.bulletCount);
        out.entityCount   = env->GetIntField(snapshotObj, g_cache.entityCount);
        out.blockCount    = env->GetIntField(snapshotObj, g_cache.blockCount);
        out.materialCount = env->GetIntField(snapshotObj, g_cache.materialCount);
        out.deltaTime     = env->GetDoubleField(snapshotObj, g_cache.deltaTime);
        out.isDebug       = env->GetBooleanField(snapshotObj, g_cache.isDebug);

        out.bullets   = getDirectBuffer<aff::BulletState>(env, snapshotObj, g_cache.bulletBuf);
        out.configs   = getDirectBuffer<aff::BulletConfig>(env, snapshotObj, g_cache.configBuf);
        out.entities  = getDirectBuffer<aff::Entity>(env, snapshotObj, g_cache.entityBuf);
        out.blocks    = getDirectBuffer<aff::Block>(env, snapshotObj, g_cache.blockBuf);
        out.materials = getDirectBuffer<aff::Material>(env, snapshotObj, g_cache.materialBuf);
        out.winds     = getDirectBuffer<aff::WindSample>(env, snapshotObj, g_cache.windBuf);
        out.results   = getDirectBuffer<aff::HitResult>(env, snapshotObj, g_cache.resultBuf);

        return out.bullets && out.configs && out.results;
    }

    bool rayAABB(float ox, float oy, float oz, float dx, float dy, float dz,
                 float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                 float& outTmin, float& outTmax) {
        float tmin = -1e30f, tmax = 1e30f;
        if (std::fabs(dx) > 1e-6f) {
            float t1 = (minX - ox) / dx, t2 = (maxX - ox) / dx;
            if (t1 > t2) std::swap(t1, t2);
            tmin = std::fmax(tmin, t1); tmax = std::fmin(tmax, t2);
            if (tmin > tmax) return false;
        } else if (ox < minX || ox > maxX) return false;

        if (std::fabs(dy) > 1e-6f) {
            float t1 = (minY - oy) / dy, t2 = (maxY - oy) / dy;
            if (t1 > t2) std::swap(t1, t2);
            tmin = std::fmax(tmin, t1); tmax = std::fmin(tmax, t2);
            if (tmin > tmax) return false;
        } else if (oy < minY || oy > maxY) return false;

        if (std::fabs(dz) > 1e-6f) {
            float t1 = (minZ - oz) / dz, t2 = (maxZ - oz) / dz;
            if (t1 > t2) std::swap(t1, t2);
            tmin = std::fmax(tmin, t1); tmax = std::fmin(tmax, t2);
            if (tmin > tmax) return false;
        } else if (oz < minZ || oz > maxZ) return false;

        if (tmax < 0.0f) return false;
        outTmin = std::fmax(tmin, 0.0f);
        outTmax = tmax;
        return true;
    }

    void calculateBlockNormal(float px, float py, float pz, const aff::Block& block, float& nx, float& ny, float& nz) {
        nx = px - (block.minX + block.maxX) * 0.5f;
        ny = py - (block.minY + block.maxY) * 0.5f;
        nz = pz - (block.minZ + block.maxZ) * 0.5f;
        float len = std::sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 0.0f) { nx /= len; ny /= len; nz /= len; }
        else { nx = 0; ny = 1; nz = 0; }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_v_akfz_aff_physics_cpp_NativeBallistics_simulateFrame(JNIEnv* env, jclass, jobject snapshotObj) {
    aff::FrameData frame{};
    if (!extractFrameData(env, snapshotObj, frame)) return;
    if (frame.bulletCount == 0) return;

    float dt = static_cast<float>(frame.deltaTime);

    for (int i = 0; i < frame.bulletCount; ++i) {
        aff::BulletState& b = frame.bullets[i];
        const aff::BulletConfig& cfg = frame.configs[i];
        const aff::WindSample& wind = frame.winds[i];
        aff::HitResult& res = frame.results[i];

        res.status = 0;
        res.penCount = 0;

        float px = b.px, py = b.py, pz = b.pz;
        float vx = b.vx, vy = b.vy, vz = b.vz;
        
        if (frame.isDebug) {
            printf("[AFF_NATIVE] Bullet %d START: pos=(%.2f, %.2f, %.2f), vel=(%.2f, %.2f, %.2f)\n",
                   i, px, py, pz, vx, vy, vz);
            fflush(stdout);
        }
        
        float mass = cfg.mass;
        float dragCoeff = cfg.drag;
        float gravity = -0.08f * cfg.gravity;
        
        bool alive = true;
        float remainingDt = dt;
        
        while (remainingDt > 1e-4f && alive) {
            float speed = std::sqrt(vx*vx + vy*vy + vz*vz);
            if (speed < 0.1f) { 
                alive = false; 
                break; 
            }

            float stepDt = std::min(remainingDt, 0.05f); 
            
            float relVelX = vx - wind.wx, relVelY = vy - wind.wy, relVelZ = vz - wind.wz;
            float relSpeed = std::sqrt(relVelX*relVelX + relVelY*relVelY + relVelZ*relVelZ);
            
            float dragFactor = relSpeed > 0.0f ? -dragCoeff * relSpeed * wind.fluidDragMult : 0.0f;
            float ax = dragFactor * relVelX;
            float ay = gravity + dragFactor * relVelY;
            float az = dragFactor * relVelZ;

            float nextPx = px + vx * stepDt + ax * 0.5f * stepDt * stepDt;
            float nextPy = py + vy * stepDt + ay * 0.5f * stepDt * stepDt;
            float nextPz = pz + vz * stepDt + az * 0.5f * stepDt * stepDt;
            
            float dx = nextPx - px, dy = nextPy - py, dz = nextPz - pz;
            float segLen = std::sqrt(dx*dx + dy*dy + dz*dz);

            float closestT = 1.0f;
            int hitType = 0;
            int hitIndex = -1;
            float hitTmin = 0, hitTmax = 0;

            for (int blk = 0; blk < frame.blockCount; ++blk) {
                const aff::Block& block = frame.blocks[blk];
                float tmin, tmax;
                if (rayAABB(px, py, pz, dx, dy, dz, block.minX, block.minY, block.minZ, block.maxX, block.maxY, block.maxZ, tmin, tmax)) {
                    if (tmin < closestT && tmax >= 0.0f) {
                        closestT = tmin;
                        hitType = 1;
                        hitIndex = blk;
                        hitTmin = tmin;
                        hitTmax = tmax;
                    }
                }
            }

            for (int ent = 0; ent < frame.entityCount; ++ent) {
                const aff::Entity& entity = frame.entities[ent];
                if (cfg.friendlyFire < 0.5f && entity.entityId == cfg.shooterEntityId) continue;
                
                float tmin, tmax;
                if (rayAABB(px, py, pz, dx, dy, dz, entity.minX, entity.minY, entity.minZ, entity.maxX, entity.maxY, entity.maxZ, tmin, tmax)) {
                    if (tmin < closestT && tmax >= 0.0f) {
                        closestT = tmin;
                        hitType = 2;
                        hitIndex = ent;
                    }
                }
            }

            if (hitType == 0) {
                px = nextPx; py = nextPy; pz = nextPz;
                vx += ax * stepDt; vy += ay * stepDt; vz += az * stepDt;
                remainingDt -= stepDt;
                continue;
            }

            float hitX = px + dx * closestT;
            float hitY = py + dy * closestT;
            float hitZ = pz + dz * closestT;

            if (hitType == 2) {
                res.status = 2;
                res.hx = hitX; res.hy = hitY; res.hz = hitZ;
                res.extra = static_cast<float>(frame.entities[hitIndex].entityId);
                alive = false;
                break;
            }

            const aff::Block& block = frame.blocks[hitIndex];
            
            // Защита от мусорного materialId
            int matId = block.materialId;
            if (matId < 0 || matId >= frame.materialCount) matId = 0;
            const aff::Material& material = frame.materials[matId];

            float normalX, normalY, normalZ;
            calculateBlockNormal(hitX, hitY, hitZ, block, normalX, normalY, normalZ);

            float dot = vx*normalX + vy*normalY + vz*normalZ;
            float cosAngle = std::clamp(std::fabs(dot) / speed, 0.0f, 1.0f);
            float impactAngleDeg = std::acos(cosAngle) * 180.0f / 3.14159265f;

            float currentEnergy = 0.5f * mass * speed * speed;

            float thickness = (hitTmax - hitTmin) * segLen;
            float requiredEnergy = material.maxPenetrationJoules * thickness * (1.0f + 0.25f * (1.0f - cosAngle));

            float grazing = (impactAngleDeg - 50.0f) / 40.0f;
            if (grazing > 0.0f) {
                float hardnessFactor = std::clamp(material.hardness / 10.0f, 0.1f, 2.5f);
                float speedRatio  = speed / std::fmax(cfg.muzzleVelocity, 1.0f);
                float speedFactor = 0.4f + 0.6f * std::clamp(speedRatio, 0.0f, 1.5f);
                float penFactor   = (currentEnergy < requiredEnergy) ? 1.5f : 1.0f;
                float chance = std::clamp(grazing * hardnessFactor * 0.45f * speedFactor * penFactor, 0.0f, 0.85f);

                if (frame.isDebug) {
                    printf("[AFF_NATIVE] Ricochet check: angle=%.1f deg, chance=%.2f\n", impactAngleDeg, chance);
                    fflush(stdout);
                }

                std::uniform_real_distribution<float> dist(0.0f, 1.0f);
                if (dist(g_rng) < chance) {
                    res.status = 3;
                    res.hx = hitX; res.hy = hitY; res.hz = hitZ;

                    float energyLoss = 0.3f + dist(g_rng) * 0.3f;
                    vx = (vx - 2.0f * dot * normalX) * (1.0f - energyLoss);
                    vy = (vy - 2.0f * dot * normalY) * (1.0f - energyLoss);
                    vz = (vz - 2.0f * dot * normalZ) * (1.0f - energyLoss);

                    px = hitX + normalX * 0.05f;
                    py = hitY + normalY * 0.05f;
                    pz = hitZ + normalZ * 0.05f;

                    remainingDt -= closestT * stepDt;
                    continue;
                }
            }

            if (frame.isDebug) {
                printf("[AFF_NATIVE] Bullet %d HIT BLOCK MatID=%d | E=%.1fJ Req=%.1fJ Thick=%.3f\n",
                       i, block.materialId, currentEnergy, requiredEnergy, thickness);
                fflush(stdout);
            }

            if (currentEnergy > requiredEnergy) {
                float dirLen = segLen > 0.001f ? segLen : 1.0f;
                float normDx = dx / dirLen;
                float normDy = dy / dirLen;
                float normDz = dz / dirLen;

                float penX = hitX + normDx * 0.02f;
                float penY = hitY + normDy * 0.02f;
                float penZ = hitZ + normDz * 0.02f;

                if (res.penCount < 16) {
                    res.penX[res.penCount] = penX;
                    res.penY[res.penCount] = penY;
                    res.penZ[res.penCount] = penZ;
                    res.penMatId[res.penCount] = block.materialId;
                    
                    res.penBX[res.penCount] = static_cast<int>(std::floor((block.minX + block.maxX) * 0.5f));
                    res.penBY[res.penCount] = static_cast<int>(std::floor((block.minY + block.maxY) * 0.5f));
                    res.penBZ[res.penCount] = static_cast<int>(std::floor((block.minZ + block.maxZ) * 0.5f));
                    
                    res.penCount++;
                }
                res.status = 5; 

                float newEnergy = currentEnergy - requiredEnergy;
                float newSpeed = std::sqrt((2.0f * newEnergy) / mass);
                
                vx = normDx * newSpeed;
                vy = normDy * newSpeed;
                vz = normDz * newSpeed;

                float exitX = px + dx * hitTmax;
                float exitY = py + dy * hitTmax;
                float exitZ = pz + dz * hitTmax;
                px = exitX + normDx * 0.02f;
                py = exitY + normDy * 0.02f;
                pz = exitZ + normDz * 0.02f;

                remainingDt -= hitTmax * stepDt;
            } else {
                res.status = 4;
                res.hx = hitX; res.hy = hitY; res.hz = hitZ;
                res.extra = static_cast<float>(block.materialId);
                alive = false;
                break;
            }
        }

        if (alive && py < -64.0f) {
            res.status = -1;
            alive = false;
        }

        b.px = px; b.py = py; b.pz = pz;
        b.vx = vx; b.vy = vy; b.vz = vz;
    }

    if (frame.isDebug) {
        printf("[AFF_NATIVE] === RESULT BUFFER DUMP ===\n");
        for (int i = 0; i < frame.bulletCount; ++i) {
            const aff::HitResult& r = frame.results[i];
            printf("[AFF_NATIVE] Bullet %d: status=%d, penCount=%d, hx=%.2f, hy=%.2f, hz=%.2f, extra=%.0f\n",
                   i, r.status, r.penCount, r.hx, r.hy, r.hz, r.extra);
            if (r.penCount > 0) {
                for (int p = 0; p < r.penCount; ++p) {
                    printf("[AFF_NATIVE]   -> PEN[%d]: x=%.2f, y=%.2f, z=%.2f, matId=%d\n",
                           p, r.penX[p], r.penY[p], r.penZ[p], r.penMatId[p]);
                }
            }
        }
        printf("[AFF_NATIVE] === END DUMP ===\n");
        fflush(stdout);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_v_akfz_aff_physics_cpp_NativeBallistics_discard(JNIEnv*, jclass) {
    g_cache.reset();
}