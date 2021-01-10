package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
    }

    final int NEUTRAL_EC = 1;
    int getValueForFlag(
        final int what_the_flag_represents,
        MapLocation loc
    ) {
        // These two variables need to be int type.
        // If they are byte type, if they are negative they will be incorrectly casted to a negative int.
        int x = 0b11111111 & (loc.x - where_i_spawned.x);
        int y = 0b11111111 & (loc.y - where_i_spawned.y);
        int result = what_the_flag_represents;
        result <<= 8;
        result |= x;
        result <<= 8;
        result |= y;

        return result;
    }


    boolean flagNeutralECs() throws GameActionException {
        boolean did_set_flag = false;
        int neutral_ec_id = -1;
        RobotInfo neutral_ec = null;
        for(RobotInfo rbt : rc.senseNearbyRobots(
            rc.getType().sensorRadiusSquared,
            Team.NEUTRAL
        )) {
            neutral_ec = rbt;
        }
        if(20 < rc.getRoundNum() - round_when_i_last_set_my_flag) {
            // Because trySetFlag sets round_when_i_last_set_my_flag, this runs once per 20 rounds
            trySetFlag(0);
        }
        if(neutral_ec != null) {
            int value_for_flag = getValueForFlag(
                NEUTRAL_EC,
                neutral_ec.location
            );
            if(trySetFlag(value_for_flag)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
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
