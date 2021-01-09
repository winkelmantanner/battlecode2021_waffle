package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    RobotController rc;
    boolean preferRotateRight = false;
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;

        preferRotateRight = (rc.getID() % 2 == 1);
        // preferRotateRight == unit ID is odd
        // I used the unit ID because Math.random() returns the same value for different units.

        System.out.println("I prefer to rotate " + (preferRotateRight ? "right" : "left"));
    }

    boolean fuzzyStep(Direction target_dir) throws GameActionException {
        boolean moved = false;
        Direction dir = target_dir;
        for(int k = 0; k < 8; k++) {
            if(rc.canMove(dir)) {
                rc.move(dir);
                moved = true;
                break;
            }
            if(preferRotateRight ^ (k % 2 > 0)) {
                for(int j = 0; j < k; j++) {
                    dir = dir.rotateLeft();
                }
            } else {
                for(int j = 0; j < k; j++) {
                    dir = dir.rotateRight();
                }
            }
        }
        return moved;
    }
    boolean fuzzyStep(MapLocation dest) throws GameActionException {
        return fuzzyStep(rc.getLocation().directionTo(dest));
    }

    Direction explore_dir = null;
    boolean need_dir = true;
    boolean exploreMove() throws GameActionException {
        if(need_dir) {
            explore_dir = randomDirection();
            need_dir = false;
        }
        boolean moved = false;
        if(rc.canMove(explore_dir)) {
            rc.move(explore_dir);
            moved = true;
        } else if(rc.isReady()) {
            need_dir = true;
        }
        return moved;
    }
}
