#pragma once

#pragma pack(push, 1)

namespace aff {

struct BulletState {
    float px, py, pz;
    float vx, vy, vz;
}; // 24 bytes

struct BulletConfig {
    float muzzleVelocity;
    float mass;
    float drag;
    float gravity;
    float maxSteps;
    float penPower;
    float friendlyFire;
    float dmgMult;
    float caliber;
    int shooterEntityId;
}; // 40 bytes

struct Entity {
    float minX, minY, minZ;
    float maxX, maxY, maxZ;
    int entityId;
}; // 28 bytes

struct Material {
    float density;
    float hardness;
    float maxPenetrationJoules;
}; // 12 bytes

struct WindSample {
    float wx, wy, wz;
    int enclosed;
    float fluidDragMult; 
}; // 20 bytes

struct Block {
    float minX, minY, minZ;
    float maxX, maxY, maxZ;
    int materialId;
    int bx, by, bz;     
}; // 40 bytes

struct HitResult {
    int status;
    float hx, hy, hz;
    float extra;
    int penCount;
    float penX[16];
    float penY[16];
    float penZ[16];
    int penMatId[16];
    int penBX[16];    
    int penBY[16];
    int penBZ[16];
}; // 472 bytes

struct FrameData {
    BulletState* bullets;
    BulletConfig* configs;
    Entity* entities;
    Block* blocks;
    Material* materials;
    WindSample* winds;
    HitResult* results;

    int bulletCount;
    int entityCount;
    int blockCount;
    int materialCount;
    double deltaTime;
    bool isDebug;
};

}

#pragma pack(pop)