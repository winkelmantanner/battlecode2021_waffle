package tannerplayer;
import battlecode.common.*;

public strictfp class SlanPol extends Unit {
    RobotType last_round_type = null;


    boolean is_defender = false;

    SlanPol(RobotController rbt_controller) {
        super(rbt_controller);
        last_round_type = rc.getType();
        is_defender = (
            id_of_ec_to_look_to != -1
            && rc.getType().equals(RobotType.POLITICIAN)
            && (rc.getInfluence() <= MAX_DEFENDER_INFLUENCE)
        );
    }

    double getEmpowerConvAvailable() throws GameActionException {
        return (
            rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0)
        ) - GameConstants.EMPOWER_TAX;
    }
    void attackerEmpower() throws GameActionException {
        final double conv_available = getEmpowerConvAvailable();
        for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
            int actionR2 = r_unsquared * r_unsquared;
            double transferrableConviction = 0;
            boolean just_do_it = false;
            int rbts_len = -1; // This variable is used for logs only
            if(conv_available >= 1) {
                RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
                rbts_len = rbts.length;
                for(RobotInfo rbt : rbts) {
                    if(rbt.team != rc.getTeam()) {
                        if(
                            rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                            && conv_available / rbts.length > rbt.conviction
                        ) {
                            // It will convert the EC: just do it
                            just_do_it = true;
                        }
                        if(rbt.type.equals(RobotType.MUCKRAKER)) {
                            transferrableConviction += Math.min(rbt.conviction + 1, Math.floor(conv_available / rbts.length));
                        } else if(rbt.type.equals(RobotType.POLITICIAN)) {
                            transferrableConviction += Math.min(rbt.conviction + rbt.influence, conv_available / rbts.length);
                        } else {
                            transferrableConviction += conv_available / rbts.length;
                        }
                    }
                }
            }
            if (rc.canEmpower(actionR2) &&
                (
                    just_do_it
                    || (transferrableConviction
                        >= conv_available
                            * recipDecay(
                                rc.getRoundNum() - roundNumCreated,
                                100
                            )
                    )
                )
            ) {
                System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()) + " actionR2:" + String.valueOf(actionR2) + " transferrableConviction:" + String.valueOf(transferrableConviction) + " conv_available:" + String.valueOf(conv_available) + " rbts_len:" + String.valueOf(rbts_len));
                rc.empower(actionR2);
            }
        }
    }

    void empowerIfDamagedBeyondUsability() throws GameActionException {
        if(getEmpowerConvAvailable() <= 0 && rc.canEmpower(1)) {
            rc.empower(1);
            System.out.println("Empowered due to being damaged beyond usability");
        }
    }

    final double SELF_EMPOWER_MIN_MULTIPLIER = 1.25;
    void empowerOnHomeEcIfBuffedEnough() throws GameActionException {
        int target_dist2 = rc.getLocation().distanceSquaredTo(loc_of_ec_to_look_to);
        if(target_dist2 <= 2) {
            final double conv_available = getEmpowerConvAvailable();
            RobotInfo [] rbts = rc.senseNearbyRobots(target_dist2);
            if(conv_available / rbts.length > SELF_EMPOWER_MIN_MULTIPLIER * rc.getInfluence()
                && rc.canEmpower(target_dist2)
            ) {
                System.out.println("I empowered on a friendly EC!!!");
                rc.empower(target_dist2);
            }
        }
    }

    final int THREATENING_MR_FOLLOW_DIST2 = 11*11;
    final int THREATENING_MR_EMPOWER_DIST2 = 9*9;
    void empowerOnThreateningMuckrakers() throws GameActionException {
        RobotInfo threatening_mr = nearestRobot(
            null,
            rc.getType().actionRadiusSquared,
            rc.getTeam().opponent(),
            RobotType.MUCKRAKER
        );
        if(threatening_mr != null
            && where_i_spawned.distanceSquaredTo(threatening_mr.location) < THREATENING_MR_EMPOWER_DIST2
        ) {
            // Do not empower unless destruction of the muckraker is guaranteed.
            // Try the dist2 to the muckraker and try the maximum dist2.
            int dist2_to_mr = rc.getLocation().distanceSquaredTo(threatening_mr.location);
            final int num_rbts_minimum = rc.senseNearbyRobots(dist2_to_mr).length;
            final int num_rbts_maximum = rc.senseNearbyRobots(rc.getType().actionRadiusSquared).length;
            final double conv_available = getEmpowerConvAvailable();
            int dist2_to_empower = -1;
            if(conv_available / num_rbts_maximum >= 1 + threatening_mr.conviction) {
                dist2_to_empower = rc.getType().actionRadiusSquared;
            } else if(conv_available / num_rbts_minimum >= 1 + threatening_mr.conviction) {
                dist2_to_empower = dist2_to_mr;
            } else {
                System.out.println("Did not empower solely because I could not guarantee the destruction of the muckraker at " + threatening_mr.location.toString());
            }
            if(dist2_to_empower > 0
                && rc.canEmpower(dist2_to_empower)
            ) {
                System.out.println("empowering on muckraker  myconviction:" + String.valueOf(rc.getConviction()) + " dist2_to_empower:" + String.valueOf(dist2_to_empower) + " conv_available:" + String.valueOf(conv_available));
                rc.empower(dist2_to_empower);
            }
        }
    }

    void moveTowardNonfriendlyEcs() throws GameActionException {
        if(rc.isReady()) {
            RobotInfo target_rbt = nearestRobot(null, -1, Team.NEUTRAL, RobotType.ENLIGHTENMENT_CENTER);
            if(target_rbt == null) {
                target_rbt = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.ENLIGHTENMENT_CENTER);
            }
            if(target_rbt != null) {
                fuzzyStep(target_rbt.location);
            }
        }
    }

    void moveTowardEnemyMuckrakers() throws GameActionException {
        if(rc.isReady()) {
            RobotInfo target_rbt = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.MUCKRAKER);
            if(target_rbt != null
                && where_i_spawned.distanceSquaredTo(target_rbt.location) < THREATENING_MR_FOLLOW_DIST2
            ) {
                fuzzyStep(target_rbt.location);
            }
        }
    }

    final int NEAR_HOME_DIST2 = 7*7;
    boolean spreadNearHome() throws GameActionException {
        boolean moved = false;
        RobotInfo nearby_robot = nearestRobot(null, 2, null, null);
        if(nearby_robot != null) {
            if(fuzzyStep(rc.getLocation().directionTo(nearby_robot.location).opposite())) {
                moved = true;
                System.out.println("I stepped away from robot at " + nearby_robot.location.toString());
            }
        } else if(rc.getLocation().distanceSquaredTo(where_i_spawned) >= NEAR_HOME_DIST2) {
            moved = fuzzyStep(where_i_spawned);
        } else {
            moved = tryMove(randomDirection());
        }
        return moved;
    }

    MapLocation target_loc_from_flag = null;
    int round_num_of_flag_read = -1;
    void polFlagReceivingStuff() throws GameActionException {
        if(target_loc_from_flag == null) {
            if(rc.canGetFlag(id_of_ec_to_look_to)) {
                int flag_val = rc.getFlag(id_of_ec_to_look_to);
                if(flag_val >> 16 == NEUTRAL_EC) {
                    target_loc_from_flag = getMapLocationFromFlagValue(flag_val);
                    round_num_of_flag_read = rc.getRoundNum();
                }
            }
        } else { // target_loc_from_flag != null
            fuzzyStep(target_loc_from_flag);
        }
        if(50 < rc.getRoundNum() - round_num_of_flag_read) {
            target_loc_from_flag = null;
        }
    }

    final int HIDING_DISTANCE = 5; // not squared
    MapLocation enemy_loc_from_flag = null;
    int round_of_enemy_loc_from_flag = -1;
    void slanFlagReceivingStuff() throws GameActionException {
        if(enemy_loc_from_flag == null) {
            if(rc.canGetFlag(id_of_ec_to_look_to)) {
                int flag_val = rc.getFlag(id_of_ec_to_look_to);
                if(flag_val >> 16 == ENEMY_ROBOT) {
                    enemy_loc_from_flag = getMapLocationFromFlagValue(flag_val);
                    round_of_enemy_loc_from_flag = rc.getRoundNum();
                }
            }
        } else {
            Direction enemy_dir = where_i_spawned.directionTo(enemy_loc_from_flag);
            Direction hiding_dir = enemy_dir.opposite();
            MapLocation hiding_spot = where_i_spawned.translate(
                hiding_dir.dx * HIDING_DISTANCE,
                hiding_dir.dy * HIDING_DISTANCE
            );
            if(fuzzyStep(hiding_spot)) {
                System.out.println("HID BASED ON FLAGGED ENEMY at " + enemy_loc_from_flag.toString());
            }
        }
        if(10 < rc.getRoundNum() - round_of_enemy_loc_from_flag) {
            enemy_loc_from_flag = null;
        }
    }

    public void runTurnPolitician() throws GameActionException {
        mapEdgeFlagReceivingStuffNonEc();

        flagEnemies();
        flagMapEdges();
        flagNeutralECs();
        // The later flag overrides the earlier

        empowerIfDamagedBeyondUsability();

        empowerOnHomeEcIfBuffedEnough();

        if(is_defender) { // influence is not reduced by damage
            // I'm a defender
            empowerOnThreateningMuckrakers();

            moveTowardEnemyMuckrakers();

            spreadNearHome();
        } else {
            // I'm an attacker
            attackerEmpower();

            moveTowardNonfriendlyEcs();

            polFlagReceivingStuff();

            exploreMove();
        }
    }

    public void runTurnSlanderer() throws GameActionException {
        mapEdgeFlagReceivingStuffNonEc();

        flagEnemies();

        MapLocation myLoc = rc.getLocation();
        MapLocation loc_of_nearest_enemy_mr = null;
        int dist2_to_nearest_enemy_mr = 123456;
        for(RobotInfo rbt : rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent())) {
            if(rbt.type.equals(RobotType.MUCKRAKER)
                && myLoc.distanceSquaredTo(rbt.location) < dist2_to_nearest_enemy_mr
            ) {
                loc_of_nearest_enemy_mr = rbt.location;
                dist2_to_nearest_enemy_mr = myLoc.distanceSquaredTo(rbt.location);
            }
        }
        if(loc_of_nearest_enemy_mr != null) {
            if(fuzzyStep(myLoc.directionTo(loc_of_nearest_enemy_mr).opposite())) {
                System.out.println("Ran from enemy muckraker at " + loc_of_nearest_enemy_mr.toString());
            }
        }
        
        slanFlagReceivingStuff();

        if (tryMove(randomDirection())) {}
    }

    public void runTurnUnit() throws GameActionException {
        if(!rc.getType().equals(last_round_type)) {
            sensor_radius_nonsquared = Math.sqrt(rc.getType().sensorRadiusSquared);
        }
        switch (rc.getType()) {
            case POLITICIAN: runTurnPolitician(); break;
            case SLANDERER:  runTurnSlanderer();  break;
        }
        last_round_type = rc.getType();
    }
}
