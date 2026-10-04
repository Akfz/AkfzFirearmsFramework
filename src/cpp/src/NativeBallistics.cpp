#include "java/v_akfz_aff_physics_cpp_NativeBallistics.h"
#include "aff_types.h"
#include <cmath>
#include <algorithm>
#include <random>
#include <cstdio>

namespace {

    constexpr float MC_GRAVITY           = 32.0f;
    constexpr float KMH_TO_MS            = 1.0f / 3.6f;
    constexpr float MAX_SUBSTEP_DISTANCE = 0.4f;
    constexpr float MIN_SPEED            = 0.5f;
    constexpr int   MAX_SUBSTEPS         = 128;

    constexpr float EPS                  = 1e-4f;
    constexpr float PI                   = 3.14159265358979323846f;

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

    inline bool rayAABB(float ox, float oy, float oz, float dx, float dy, float dz,
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

    inline void calculateFaceNormal(float ex, float ey, float ez,
                                    const aff::Block& block,
                                    float& nx, float& ny, float& nz) {
        const float dMinX = std::fabs(ex - block.minX);
        const float dMaxX = std::fabs(ex - block.maxX);
        const float dMinY = std::fabs(ey - block.minY);
        const float dMaxY = std::fabs(ey - block.maxY);
        const float dMinZ = std::fabs(ez - block.minZ);
        const float dMaxZ = std::fabs(ez - block.maxZ);

        float best = dMinX;
        nx = -1.0f; ny = 0.0f; nz = 0.0f;

        if (dMaxX < best) { best = dMaxX; nx =  1.0f; ny =  0.0f; nz =  0.0f; }
        if (dMinY < best) { best = dMinY; nx =  0.0f; ny = -1.0f; nz =  0.0f; }
        if (dMaxY < best) { best = dMaxY; nx =  0.0f; ny =  1.0f; nz =  0.0f; }
        if (dMinZ < best) { best = dMinZ; nx =  0.0f; ny =  0.0f; nz = -1.0f; }
        if (dMaxZ < best) {                 nx =  0.0f; ny =  0.0f; nz =  1.0f; }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_v_akfz_aff_physics_cpp_NativeBallistics_simulateFrame(JNIEnv* env, jclass, jobject snapshotObj) {
    aff::FrameData frame{};
    if (!extractFrameData(env, snapshotObj, frame)) return;
    if (frame.bulletCount == 0) return;

    const float dt = static_cast<float>(frame.deltaTime);
    const aff::WindSample zeroWind{0.0f, 0.0f, 0.0f, 1, 1.0f};

    const bool haveMaterials = (frame.materials != nullptr) && (frame.materialCount > 0);

    for (int i = 0; i < frame.bulletCount; ++i) {
        aff::BulletState&        b    = frame.bullets[i];
        const aff::BulletConfig& cfg  = frame.configs[i];
        const aff::WindSample&   wind = frame.winds ? frame.winds[i] : zeroWind;
        aff::HitResult&          res  = frame.results[i];

        res.status   = 0;
        res.penCount = 0;

        float px = b.px, py = b.py, pz = b.pz;
        float vx = b.vx, vy = b.vy, vz = b.vz;

        const float mass      = std::fmax(cfg.mass, 1e-6f);
        const float dragCoeff = cfg.drag;
        const float gravity   = -MC_GRAVITY * cfg.gravity;

        const float windX = wind.wx * KMH_TO_MS;
        const float windY = wind.wy * KMH_TO_MS;
        const float windZ = wind.wz * KMH_TO_MS;
        const float fluidDrag = std::fmax(wind.fluidDragMult, 0.0f);

        if (frame.isDebug) {
            printf("[AFF_NATIVE] Bullet %d START pos=(%.2f,%.2f,%.2f) vel=(%.2f,%.2f,%.2f) g=%.2f\n",
                   i, px, py, pz, vx, vy, vz, gravity);
            fflush(stdout);
        }

        bool  alive       = true;
        float remainingDt = dt;
        int   substeps    = 0;

        while (remainingDt > EPS && alive && substeps < MAX_SUBSTEPS) {
            ++substeps;

            const float speed = std::sqrt(vx*vx + vy*vy + vz*vz);
            if (speed < MIN_SPEED) { alive = false; break; }

            float stepDt = std::min(remainingDt, MAX_SUBSTEP_DISTANCE / speed);
            stepDt = std::fmax(stepDt, EPS);

            const float relVX = vx - windX;
            const float relVY = vy - windY;
            const float relVZ = vz - windZ;
            const float relSpeed = std::sqrt(relVX*relVX + relVY*relVY + relVZ*relVZ);

            const float dragFactor = (relSpeed > 1e-6f)
                ? -dragCoeff * relSpeed * fluidDrag
                : 0.0f;

            const float ax = dragFactor * relVX;
            const float ay = gravity + dragFactor * relVY;
            const float az = dragFactor * relVZ;

            const float nextPx = px + vx * stepDt + 0.5f * ax * stepDt * stepDt;
            const float nextPy = py + vy * stepDt + 0.5f * ay * stepDt * stepDt;
            const float nextPz = pz + vz * stepDt + 0.5f * az * stepDt * stepDt;

            const float dx = nextPx - px;
            const float dy = nextPy - py;
            const float dz = nextPz - pz;
            const float segLen = std::sqrt(dx*dx + dy*dy + dz*dz);
            if (segLen < 1e-6f) { remainingDt -= stepDt; continue; }

            float closestT = 1.0f;
            int   hitType  = 0;
            int   hitIndex = -1;
            float hitTmin  = 0.0f;
            float hitTmax  = 0.0f;

            for (int blk = 0; blk < frame.blockCount; ++blk) {
                const aff::Block& block = frame.blocks[blk];
                float tmin, tmax;
                if (rayAABB(px, py, pz, dx, dy, dz,
                            block.minX, block.minY, block.minZ,
                            block.maxX, block.maxY, block.maxZ,
                            tmin, tmax)) {
                    if (tmin < closestT && tmax >= 0.0f) {
                        closestT = tmin;
                        hitType  = 1;
                        hitIndex = blk;
                        hitTmin  = tmin;
                        hitTmax  = tmax;
                    }
                }
            }

            for (int ent = 0; ent < frame.entityCount; ++ent) {
                const aff::Entity& entity = frame.entities[ent];

                if (entity.entityId == cfg.shooterEntityId) continue;

                float tmin, tmax;
                if (rayAABB(px, py, pz, dx, dy, dz,
                            entity.minX, entity.minY, entity.minZ,
                            entity.maxX, entity.maxY, entity.maxZ,
                            tmin, tmax)) {
                    if (tmin < closestT && tmax >= 0.0f) {
                        closestT = tmin;
                        hitType  = 2;
                        hitIndex = ent;
                        hitTmin  = tmin;
                        hitTmax  = tmax;
                    }
                }
            }

            if (hitType == 0) {
                px = nextPx; py = nextPy; pz = nextPz;
                vx += ax * stepDt;
                vy += ay * stepDt;
                vz += az * stepDt;
                remainingDt -= stepDt;
                continue;
            }

            const float hitX = px + dx * closestT;
            const float hitY = py + dy * closestT;
            const float hitZ = pz + dz * closestT;

            if (hitType == 2) {
                res.status = 2;
                res.hx = hitX; res.hy = hitY; res.hz = hitZ;
                res.extra = static_cast<float>(frame.entities[hitIndex].entityId);
                alive = false;
                break;
            }

            const aff::Block& block = frame.blocks[hitIndex];

            if (!haveMaterials) {
                res.status = 4;
                res.hx = hitX; res.hy = hitY; res.hz = hitZ;
                res.extra = static_cast<float>(block.materialId);
                alive = false;
                break;
            }

            int matId = block.materialId;
            if (matId < 0 || matId >= frame.materialCount) matId = 0;
            const aff::Material& material = frame.materials[matId];

            float normalX, normalY, normalZ;
            calculateFaceNormal(hitX, hitY, hitZ, block, normalX, normalY, normalZ);

            const float dot       = vx*normalX + vy*normalY + vz*normalZ;
            const float cosAngle  = std::clamp(std::fabs(dot) / std::fmax(speed, 1e-6f), 0.0f, 1.0f);
            const float impactDeg = std::acos(cosAngle) * 180.0f / PI;
            const float curEnergy = 0.5f * mass * speed * speed;

            const float thickness = (hitTmax - hitTmin) * segLen;
            const float reqEnergy = material.maxPenetrationJoules * thickness;

            if (impactDeg > 50.0f) {
                const float grazing        = (impactDeg - 50.0f) / 40.0f;
                const float hardnessFactor = std::clamp(material.hardness / 10.0f, 0.1f, 2.5f);
                const float speedRatio     = speed / std::fmax(cfg.muzzleVelocity, 1.0f);
                const float speedFactor    = 0.4f + 0.6f * std::clamp(speedRatio, 0.0f, 1.5f);
                const float penFactor      = (curEnergy < reqEnergy) ? 1.5f : 1.0f;
                const float chance         = std::clamp(
                        grazing * hardnessFactor * 0.45f * speedFactor * penFactor,
                        0.0f, 0.85f);

                std::uniform_real_distribution<float> dist(0.0f, 1.0f);
                if (dist(g_rng) < chance) {
                    res.status = 3;
                    res.hx = hitX; res.hy = hitY; res.hz = hitZ;

                    const float energyLoss = 0.3f + dist(g_rng) * 0.3f;
                    vx = (vx - 2.0f * dot * normalX) * (1.0f - energyLoss);
                    vy = (vy - 2.0f * dot * normalY) * (1.0f - energyLoss);
                    vz = (vz - 2.0f * dot * normalZ) * (1.0f - energyLoss);

                    px = hitX + normalX * 0.02f;
                    py = hitY + normalY * 0.02f;
                    pz = hitZ + normalZ * 0.02f;

                    remainingDt -= closestT * stepDt;
                    continue;
                }
            }

            if (curEnergy > reqEnergy) {
                const float dirLen = (segLen > EPS) ? segLen : 1.0f;
                const float ndx = dx / dirLen;
                const float ndy = dy / dirLen;
                const float ndz = dz / dirLen;

                if (res.penCount < 16) {
                    res.penX[res.penCount]     = hitX + ndx * 0.02f;
                    res.penY[res.penCount]     = hitY + ndy * 0.02f;
                    res.penZ[res.penCount]     = hitZ + ndz * 0.02f;
                    res.penMatId[res.penCount] = block.materialId;

                    res.penBX[res.penCount] = static_cast<int>(std::floor((block.minX + block.maxX) * 0.5f));
                    res.penBY[res.penCount] = static_cast<int>(std::floor((block.minY + block.maxY) * 0.5f));
                    res.penBZ[res.penCount] = static_cast<int>(std::floor((block.minZ + block.maxZ) * 0.5f));

                    res.penCount++;
                }
                res.status = 5;

                const float newEnergy = std::fmax(curEnergy - reqEnergy, 0.0f);
                const float newSpeed  = std::sqrt((2.0f * newEnergy) / mass);

                const float dirScale = newSpeed / std::fmax(speed, 1e-6f);
                vx *= dirScale;
                vy *= dirScale;
                vz *= dirScale;

                const float exitT = std::min(hitTmax + 1e-3f, 1.0f);
                px = px + dx * exitT + ndx * 0.02f;
                py = py + dy * exitT + ndy * 0.02f;
                pz = pz + dz * exitT + ndz * 0.02f;

                remainingDt -= hitTmax * stepDt;
                continue;
            }

            if (frame.isDebug) {
                printf("[AFF_NATIVE] Bullet %d DESTROYED MatID=%d E=%.1f Req=%.1f Thick=%.3f Angle=%.1f\n",
                       i, block.materialId, curEnergy, reqEnergy, thickness, impactDeg);
                fflush(stdout);
            }
            res.status = 4;
            res.hx = hitX; res.hy = hitY; res.hz = hitZ;
            res.extra = static_cast<float>(block.materialId);
            alive = false;
            break;
        }

        if (alive && py < -64.0f) {
            res.status = -1;
            alive = false;
        }

        b.px = px; b.py = py; b.pz = pz;
        b.vx = vx; b.vy = vy; b.vz = vz;

        if (frame.isDebug) {
            printf("[AFF_NATIVE] Bullet %d END pos=(%.2f,%.2f,%.2f) vel=(%.2f,%.2f,%.2f) substeps=%d\n",
                   i, px, py, pz, vx, vy, vz, substeps);
            fflush(stdout);
        }
    }

    if (frame.isDebug) {
        printf("[AFF_NATIVE] === RESULT BUFFER DUMP ===\n");
        for (int i = 0; i < frame.bulletCount; ++i) {
            const aff::HitResult& r = frame.results[i];
            printf("[AFF_NATIVE] Bullet %d: status=%d, penCount=%d, hx=%.2f, hy=%.2f, hz=%.2f, extra=%.0f\n",
                   i, r.status, r.penCount, r.hx, r.hy, r.hz, r.extra);
            for (int p = 0; p < r.penCount; ++p) {
                printf("[AFF_NATIVE]   PEN[%d]: (%.2f,%.2f,%.2f) matId=%d block=(%d,%d,%d)\n",
                       p, r.penX[p], r.penY[p], r.penZ[p], r.penMatId[p],
                       r.penBX[p], r.penBY[p], r.penBZ[p]);
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
