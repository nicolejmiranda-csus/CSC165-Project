package a3;

enum MushroomState { WANDER, CHASE, EXPLODED }

interface WanderBehavior {
    void step(MushroomMon mon);
}

class BackAndForth implements WanderBehavior {
    @Override
    public void step(MushroomMon mon) {
        if (mon.locationX >  10) mon.wanderDir = -mon.wanderSpeed;
        if (mon.locationX < -10) mon.wanderDir =  mon.wanderSpeed;
        mon.locationX += mon.wanderDir;
    }
}

class ExplosionEffect {
    // TODO: add slowFactor (float), duration (float), etc. when effect is implemented
    void apply(MushroomMon mon) {
        // placeholder — slow effect goes here
    }
}

public class MushroomMon {

    double locationX, locationY, locationZ;
    double wanderDir;
    double wanderSpeed    = 0.05;
    double chaseSpeed     = 0.15;
    double targetX, targetZ;
    double detectionRadius = 8.0;
    double explosionRadius = 1.5;

    MushroomState   state  = MushroomState.WANDER;
    WanderBehavior  wander = new BackAndForth();

    public MushroomMon() {
        locationX = 0.0;
        locationY = 0.0;
        locationZ = 0.0;
        wanderDir = wanderSpeed;
    }

    public void randomizeLocation(int seedX, int seedZ) {
        locationX = ((double) seedX) / 4.0 - 5.0;
        locationY = 0;
        locationZ = -2;
    }

    public double getX() { return locationX; }
    public double getY() { return locationY; }
    public double getZ() { return locationZ; }

    public MushroomState getState()            { return state; }
    public void          setState(MushroomState s) { state = s; }

    public void setTarget(double x, double z) {
        targetX = x;
        targetZ = z;
    }

    public void updateLocation() {
        switch (state) {
            case WANDER: wander.step(this);      break;
            case CHASE:  moveTowardTarget();     break;
            case EXPLODED:                       break;
        }
    }

    public void moveTowardTarget() {
        double dx  = targetX - locationX;
        double dz  = targetZ - locationZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return;
        locationX += (dx / len) * chaseSpeed;
        locationZ += (dz / len) * chaseSpeed;
    }

    public boolean isCloseEnoughToExplode() {
        double dx = targetX - locationX;
        double dz = targetZ - locationZ;
        return Math.sqrt(dx * dx + dz * dz) <= explosionRadius;
    }

    public void explode() {
        state = MushroomState.EXPLODED;
        new ExplosionEffect().apply(this);
    }

    public double getDetectionRadius() { return detectionRadius; }
    public double getExplosionRadius() { return explosionRadius; }
}
