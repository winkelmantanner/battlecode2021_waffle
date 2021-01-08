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

    boolean fuzzyStep(MapLocation dest) throws GameActionException {
        boolean moved = false;
        Direction dir = rc.getLocation().directionTo(dest);
        for(int k = 0; k < 8; k++) {
            if(rc.canMove(dir)) {
                rc.move(dir);
                moved = true;
                break;
            }
            if(preferRotateRight ^ (k % 2 > 0)) {
                for(int j = 0; j < k; j++) {
                    dir.rotateLeft();
                }
            } else {
                for(int j = 0; j < k; j++) {
                    dir.rotateRight();
                }
            }
        }
        return moved;
    }
}
