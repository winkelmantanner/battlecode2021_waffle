package tannerplayer;
import battlecode.common.*;

public strictfp class Muckraker extends Unit {
    MapLocation where_i_saw_enemy_slanderer = null;
    int round_when_i_saw_enemy_slanderer = -1;
    boolean is_cruncher = false;
    Muckraker(RobotController rbt_controller) {
        super(rbt_controller);
        is_cruncher = (rc.getID() % 10 >= 2); // 4/5 will be crunchers
        System.out.println("is_cruncher:" + String.valueOf(is_cruncher));
    }


    boolean getIfFriendlyEcIsInCardinalDirection() throws GameActionException {
        boolean is_friendly_ec_in_card_dir = false;
        for(Direction card_dir : Direction.cardinalDirections()) {
            MapLocation ml = rc.adjacentLocation(card_dir);
            if(rc.canSenseLocation(ml)) {
                RobotInfo rbt = rc.senseRobotAtLocation(ml);
                if(
                    rbt != null
                    && rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                    && rbt.team.equals(rc.getTeam())
                ) {
                    is_friendly_ec_in_card_dir = true;
                    break;
                }
            }
        }
        return is_friendly_ec_in_card_dir;
    }

    boolean knowMapDims() {
        return map_max_x != UNKNOWN
            && map_min_x != UNKNOWN
            && map_max_y != UNKNOWN
            && map_min_y != UNKNOWN;
    }
    MapLocation [] getPlacesEnemyEcMightBe() throws GameActionException {
        // precondtion: knowMapDims() == true
        boolean vertical_first = (rc.getID() % 2 == 1);
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
        if(
            knowMapDims()
            && index_in_places < 3
        ) {
            MapLocation [] places = getPlacesEnemyEcMightBe();
            if(rc.canSenseLocation(places[index_in_places])) {
                RobotInfo rbt = rc.senseRobotAtLocation(places[index_in_places]);
                if(rbt != null
                    && rbt.team.equals(rc.getTeam().opponent())
                    && rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                ) {
                    //FOUND IT
                    if(rc.getLocation().isAdjacentTo(rbt.location)) {
                        is_adj_to_enemy_ec = true;
                        System.out.println("is_adj_to_enemy_ec:" + String.valueOf(is_adj_to_enemy_ec) + "; I'm done moving");
                        // We're done moving!
                    }
                } else {
                    // We sense that the enemy EC is not there
                    index_in_places++;
                }
            }
            if(index_in_places < 3) {
                did_move = fuzzyStep(places[index_in_places]);
            }
        }
        return did_move;
    }


    public void runTurnUnit() throws GameActionException {
        mapEdgeFlagReceivingStuffNonEc();

        flagEnemies();
        flagMapEdges();
        flagNeutralECs();
        // The second flag overrides the first

        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation myLoc = rc.getLocation();

        /*
        Note, within the sensor radius, I can detect all attributes of robots.
        Within the detection radius, I can only detect their location.
        So use the sensor radius.
        */
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if(
                    round_when_i_saw_enemy_slanderer != rc.getRoundNum()
                    || where_i_saw_enemy_slanderer == null
                    || myLoc.distanceSquaredTo(robot.location) < myLoc.distanceSquaredTo(where_i_saw_enemy_slanderer)
                ) {
                    where_i_saw_enemy_slanderer = robot.location;
                    round_when_i_saw_enemy_slanderer = rc.getRoundNum();
                }
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }


        if(
            !getIfFriendlyEcIsInCardinalDirection()
            && !is_adj_to_enemy_ec
        ) {
            // Move only if not a guard

            // Try to move toward enemy slanderers seen recently
            if (
                where_i_saw_enemy_slanderer != null
                && 30 > rc.getRoundNum() - round_when_i_saw_enemy_slanderer
                && fuzzyStep(where_i_saw_enemy_slanderer)
            ) {
                System.out.println("I (muckraker) stepped toward target: " + where_i_saw_enemy_slanderer.toString());
            }

            if(is_cruncher) {
                moveForSymmetricEnemyEc();
            }

            if(!is_adj_to_enemy_ec) {
                exploreMove();
            }
        }
    }
}
