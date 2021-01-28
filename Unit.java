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
        vertical_first = (rc.getID() % 2 == 1); // Set later based on last unknown
    }
    boolean vertical_first = false;

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




    boolean knowMapDims() {
        return map_max_x != UNKNOWN
            && map_min_x != UNKNOWN
            && map_max_y != UNKNOWN
            && map_min_y != UNKNOWN;
    }
    MapLocation [] getPlacesEnemyEcMightBe() throws GameActionException {
        // precondtion: knowMapDims() == true
        MapLocation [] result = new MapLocation[3];
        // In the array, order them horizontal, rotational, vertical (unless vertical_first==true).
        // They are generated in a different order here for calculation convenience.
        result[0] = new MapLocation(-loc_of_ec_to_look_to.x + map_max_x + map_min_x, loc_of_ec_to_look_to.y);
        result[2] = new MapLocation( loc_of_ec_to_look_to.x, -loc_of_ec_to_look_to.y + map_max_y + map_min_y);
        result[1] = new MapLocation(result[0].x, result[2].y);
        if(vertical_first) {
            MapLocation temp = result[0];
            result[0] = result[2];
            result[2] = temp;
        }
        return result;
    }
    int index_in_places = 0;
    boolean is_adj_to_enemy_ec = false;
    boolean moveForSymmetricEnemyEc() throws GameActionException {
        boolean did_move = false;
        if(knowMapDims()) {
            if(index_in_places < 3) {
                MapLocation [] places = getPlacesEnemyEcMightBe();
                if(rc.canSenseLocation(places[index_in_places])) {
                    RobotInfo rbt = rc.senseRobotAtLocation(places[index_in_places]);
                    if(rbt != null
                        && rbt.team.equals(rc.getTeam().opponent())
                        && rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                    ) {
                        //FOUND IT
                    } else {
                        // We sense that the enemy EC is not there
                        index_in_places++;
                    }
                }
                if(index_in_places < 3) {
                    did_move = stepWithPassability(places[index_in_places]);
                    if(did_move) {
                        System.out.println("Stepped for symmetric ec loc: " + places[index_in_places]);
                    }
                }
            }
        } else { // !knowMapDims()
            int num_unknowns = 0;
            int dx = 0;
            int dy = 0;
            if(map_max_x == UNKNOWN) {dx =  1; dy =  0; num_unknowns++;}
            if(map_min_x == UNKNOWN) {dx = -1; dy =  0; num_unknowns++;}
            if(map_max_y == UNKNOWN) {dx =  0; dy =  1; num_unknowns++;}
            if(map_min_y == UNKNOWN) {dx =  0; dy = -1; num_unknowns++;}
            if(num_unknowns == 1) {
                final int dist = 10;
                vertical_first = (dy != 0);
                if(stepWithPassability(rc.getLocation().translate(
                    dist * dx,
                    dist * dy
                ))) {
                    System.out.println("STEPPED TOWARD UNKNOWN MAP EDGE, vertical_first:" + vertical_first);
                }
            }
            System.out.println("num_unknowns:" + num_unknowns);
        }
        return did_move;
    }





    final double PASSABILITY_RATIO = 0.6;
    boolean stepWithPassability(Direction target_dir) throws GameActionException {
        return stepWithPassability(rc.getLocation().add(target_dir).add(target_dir));
    }
    boolean stepWithPassability(MapLocation target_loc) throws GameActionException {
        final MapLocation myLoc = rc.getLocation();
        boolean did_move = false;
        Direction best_dir = null;
        double max_value = -12345;
        final int start_dist = myLoc.distanceSquaredTo(target_loc);
        for(Direction dir : directions) {
            MapLocation landing_loc = rc.adjacentLocation(dir);
            if(rc.canMove(dir)
                && rc.canSenseLocation(landing_loc)
            ) {
                int end_dist = landing_loc.distanceSquaredTo(target_loc);
                double value = start_dist - end_dist;
                // Allow value to be negative, representing moving away from the target.
                // Minimize the increase in distance to the target and maximize passability.
                if(value > 0) {
                    value *= rc.sensePassability(landing_loc);
                } else {
                    value /= rc.sensePassability(landing_loc);
                }
                // System.out.println("landing_loc:" + landing_loc.toString() + " " + String.valueOf(end_dist) + " myLoc:" + myLoc.toString() + " " + String.valueOf(start_dist) + " " + dir.toString());
                if(value > max_value) {
                    best_dir = dir;
                    max_value = value;
                }
            }
        }
        if(best_dir != null) {
            rc.move(best_dir);
            did_move = true;
        } // otherwise I'm stuck
        return did_move;
    }
    // boolean fuzzyStep(Direction target_dir) throws GameActionException {
    //     boolean moved = false;
    //     Direction dir = target_dir;
    //     for(int k = 0; k < 8; k++) {
    //         if(rc.canMove(dir)) {
    //             rc.move(dir);
    //             moved = true;
    //             break;
    //         }
    //         if(preferRotateRight ^ (k % 2 > 0)) {
    //             for(int j = 0; j < k; j++) {
    //                 dir = dir.rotateLeft();
    //             }
    //         } else {
    //             for(int j = 0; j < k; j++) {
    //                 dir = dir.rotateRight();
    //             }
    //         }
    //     }
    //     return moved;
    // }
    // boolean fuzzyStep(MapLocation dest) throws GameActionException {
    //     return fuzzyStep(rc.getLocation().directionTo(dest));
    // }

    MapLocation explore_loc = null;
    int getRandCoordOffset() {
        return (int)(((2 * Math.random()) - 1) * GameConstants.MAP_MAX_WIDTH); // the max height better be the same
    }
    boolean exploreMove() throws GameActionException {
        boolean moved = false;
        MapLocation myLoc = rc.getLocation();
        if(explore_loc == null) {
            explore_loc = new MapLocation(
                myLoc.x + getRandCoordOffset(),
                myLoc.y + getRandCoordOffset()
            );
        }
        // now we are guaranteed that explore_loc != null
        if(rc.isReady()
            && stepWithPassability(explore_loc)
        ) {
            final double edge_closeness = 0.5 * sensor_radius_nonsquared;
            if(rc.canSenseLocation(explore_loc)
                || (map_max_x != UNKNOWN && explore_loc.x > map_max_x && map_max_x - myLoc.x < edge_closeness)
                || (map_min_x != UNKNOWN && explore_loc.x < map_min_x && myLoc.x - map_min_x < edge_closeness)
                || (map_max_y != UNKNOWN && explore_loc.y > map_max_y && map_max_y - myLoc.y < edge_closeness)
                || (map_min_y != UNKNOWN && explore_loc.y < map_min_y && myLoc.y - map_min_y < edge_closeness)
            ) {
                explore_loc = null;
            }
        }
        return moved;
    }
}
