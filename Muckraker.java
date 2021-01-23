package tannerplayer;
import battlecode.common.*;

public strictfp class Muckraker extends Unit {
    MapLocation where_i_saw_enemy_slanderer = null;
    int round_when_i_saw_enemy_slanderer = -1;

    MapLocation where_flag_indicated_enemy_slanderer = null;
    int round_when_flag_indicated_enemy_slanderer = -1;

    boolean is_cruncher = false;
    Muckraker(RobotController rbt_controller) {
        super(rbt_controller);
        is_cruncher = (rc.getID() % 10 >= 2); // 4/5 will be crunchers
        System.out.println("is_cruncher:" + String.valueOf(is_cruncher));
    }

    boolean moveForSlandererFromFlag() throws GameActionException {
        boolean did_move = false;
        if(
            id_of_ec_to_look_to != -1
            && rc.canGetFlag(id_of_ec_to_look_to)
        ) {
            int flag_val = rc.getFlag(id_of_ec_to_look_to);
            if(getMeaningWithoutConv(flag_val) == ENEMY_SLANDERER) {
                where_flag_indicated_enemy_slanderer = getMapLocationFromMaskedFlagValue(flag_val);
                round_when_flag_indicated_enemy_slanderer = rc.getRoundNum();
            }
        }
        if(where_flag_indicated_enemy_slanderer != null
            && 50 > rc.getRoundNum() - round_when_flag_indicated_enemy_slanderer
        ) {
            did_move = stepWithPassability(where_flag_indicated_enemy_slanderer);
        }
        return did_move;
    }

    boolean flagEnemySlanderer() throws GameActionException {
        // PRE: where_i_saw_enemy_slanderer and round_when_i_saw_enemy_slanderer
        //   must be up-to-date for the current round.
        boolean did_set_flag = false;
        if(where_i_saw_enemy_slanderer != null
            && rc.getRoundNum() - round_when_i_saw_enemy_slanderer < 3
        ) {
            int flag_val = getValueForFlagMaskedLocation(ENEMY_SLANDERER, where_i_saw_enemy_slanderer);
            did_set_flag = trySetFlag(flag_val);
            System.out.println("Tried to flag enemy slan at " + where_i_saw_enemy_slanderer.toString());
        }
        return did_set_flag;
    }


    public void runTurnUnit() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation myLoc = rc.getLocation();

        mapEdgeFlagReceivingStuffNonEc();

        /*
        Note, within the sensor radius, I can detect all attributes of robots.
        Within the detection radius, I can only detect their location.
        So use the sensor radius.
        */
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if(
                    robot.influence >= 5 // Don't chase trick slanderers (but still expose them)
                    && (
                        round_when_i_saw_enemy_slanderer != rc.getRoundNum()
                        || where_i_saw_enemy_slanderer == null
                        || myLoc.distanceSquaredTo(robot.location) < myLoc.distanceSquaredTo(where_i_saw_enemy_slanderer)
                    )
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
        // Now that where_i_saw_enemy_slanderer is accurate, we can flag the slanderer and more

        flagEnemies();
        flagAndUpdateMapEdges();
        flagNeutralECs();
        flagEnemySlanderer();
        // The later flag overrides the earlier
        

        RobotInfo nearest_enemy_pol = nearestRobot(null, -1, enemy, RobotType.POLITICIAN);

        if(
            nearest_enemy_pol == null
            || (
                // We're not in a cardinal direction from a friendly EC
                null == nearestRobot(null, 1, rc.getTeam(), RobotType.ENLIGHTENMENT_CENTER)
                // We're not blocking an enemy EC
                && null == nearestRobot(null, 2, enemy, RobotType.ENLIGHTENMENT_CENTER)
            )
        ) {
            // Move only if not a guard

            // Try to move toward enemy slanderers seen recently
            if (
                where_i_saw_enemy_slanderer != null
                && 10 > rc.getRoundNum() - round_when_i_saw_enemy_slanderer
                && stepWithPassability(where_i_saw_enemy_slanderer)
            ) {
                System.out.println("I (muckraker) stepped toward target: " + where_i_saw_enemy_slanderer.toString());
            }

            if(is_cruncher) {
                // This means if we are a guard,
                //  then an enemy slan is reported,
                //  then we stop being a guard,
                //  we will not know about the enemy slan.
                moveForSlandererFromFlag();
            }

            exploreMove();
        }
    }
}
