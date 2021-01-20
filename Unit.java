package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
        for(RobotInfo rbt : rc.senseNearbyRobots(
            RobotType.ENLIGHTENMENT_CENTER.actionRadiusSquared,
            rc.getTeam()
        )) {
            if(RobotType.ENLIGHTENMENT_CENTER.equals(rbt.type)) {
                id_of_ec_to_look_to = rbt.ID;
                loc_of_ec_to_look_to = rbt.location;
            }
        }
    }

    int id_of_ec_to_look_to = -1;
    MapLocation loc_of_ec_to_look_to = null;

    public int roundNumAtStartOfRound = -1;
    public void unitBeforeRunTurn() {
    }
    public void unitAfterRunTurn() {
    }
    public void runTurnRobot() throws GameActionException{
        unitBeforeRunTurn();
        runTurnUnit();
        unitAfterRunTurn();
    }
    abstract public void runTurnUnit() throws GameActionException;

    void mapEdgeFlagReceivingStuffNonEc() throws GameActionException {
        if(rc.canGetFlag(id_of_ec_to_look_to)) {
            int flag_val = rc.getFlag(id_of_ec_to_look_to);
            updateMapEdgesBasedOnFlagIfApplicable(flag_val);
        }
    }


    final double PASSABILITY_RATIO = 0.6;
    boolean stepWithPassability(Direction target_dir) throws GameActionException {
        return stepWithPassability(rc.getLocation().add(target_dir).add(target_dir));
    }
    boolean stepWithPassability(MapLocation target_loc) throws GameActionException {
        boolean did_move = false;
        Direction best_dir = null;
        double max_passability = 0;
        Direction dir_to_target = rc.getLocation().directionTo(target_loc);
        if(rc.canMove(dir_to_target)
            && rc.canSenseLocation(rc.adjacentLocation(dir_to_target))
        ) {
            best_dir = dir_to_target;
            max_passability = rc.sensePassability(rc.adjacentLocation(dir_to_target));
        }
        int my_dist_from_target = rc.getLocation().distanceSquaredTo(target_loc);
        for(Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            int dist_from_target = loc.distanceSquaredTo(target_loc);
            if(dist_from_target < my_dist_from_target
                && rc.canMove(dir)
                && rc.canSenseLocation(loc)
                && rc.sensePassability(loc) * PASSABILITY_RATIO > max_passability
            ) {
                best_dir = dir;
                max_passability = rc.sensePassability(loc);
            }
        }
        if(best_dir != null) {
            rc.move(best_dir);
            did_move = true;
        } else {
            did_move = fuzzyStep(target_loc);
        }
        return did_move;
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
