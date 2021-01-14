package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
    }

    final int NEUTRAL_EC = 1;
    final int ENEMY_ROBOT = 2;
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

    boolean flagEnemies() throws GameActionException {
        boolean did_set_flag = false;
        int x_sum = 0;
        int y_sum = 0;
        int count = 0;
        for(RobotInfo rbt : rc.senseNearbyRobots(
            rc.getType().sensorRadiusSquared,
            rc.getTeam().opponent()
        )) {
            x_sum += rbt.location.x;
            y_sum += rbt.location.y;
            count++;
        }
        if(count > 0) {
            MapLocation enemy_centroid = new MapLocation(x_sum / count, y_sum / count);
            int flag_val = getValueForFlag(
                ENEMY_ROBOT,
                enemy_centroid
            );
            if(trySetFlag(flag_val)) {
                did_set_flag = true;
                System.out.println("Flagged centroid " + enemy_centroid.toString() + " of " + String.valueOf(count) + " enemies.");
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
