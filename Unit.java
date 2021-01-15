package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
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
    boolean exploreMove() throws GameActionException {
        boolean moved = false;
        if(explore_dir == null) {
            explore_dir = directions[rc.getID() % directions.length];
        }
        if(rc.isReady()) {
            for(int k = 0; k < directions.length; k++) {
                if(tryMove(explore_dir)) {
                    moved = true;
                    break;
                } else {
                    explore_dir = randomDirection();
                }
            }
        }
        return moved;
    }
}
