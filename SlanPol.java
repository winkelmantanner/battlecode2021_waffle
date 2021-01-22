package tannerplayer;
import battlecode.common.*;

public strictfp class SlanPol extends Unit {
    RobotType last_round_type = null;


    boolean is_defender = false;

    SlanPol(RobotController rbt_controller) {
        super(rbt_controller);
        last_round_type = rc.getType();
        loc_to_guard = where_i_spawned;
        System.out.println("roundNumCreated:" + String.valueOf(roundNumCreated));
    }

    double getEmpowerConvAvailable(final int conv) throws GameActionException {
        return (
            conv * rc.getEmpowerFactor(rc.getTeam(), 0)
        ) - GameConstants.EMPOWER_TAX;
    }
    double getEmpowerConvAvailable() throws GameActionException {
        return getEmpowerConvAvailable(rc.getConviction());
    }
    void attackerEmpower() throws GameActionException {
        final double conv_available = getEmpowerConvAvailable();

        double friendlyConvInArea = 0;
        RobotInfo enemy_ec = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.ENLIGHTENMENT_CENTER);
        boolean convert_mode = false;
        if(enemy_ec != null) {
            RobotInfo [] friendlyRbts = rc.senseNearbyRobots(-1, rc.getTeam());
            for(RobotInfo rbt : friendlyRbts) {
                if(rbt.type.equals(RobotType.POLITICIAN)) {
                    friendlyConvInArea += getEmpowerConvAvailable(rbt.conviction);
                }
            }
            convert_mode = (friendlyConvInArea > 1.5 * enemy_ec.conviction);
        }
        // These two vars are used only if convert_mode
        double best_blockers_eliminatable = 0;
        int best_r_unsquared = -1;

        for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
            final int actionR2 = r_unsquared * r_unsquared;
            double transferrableConviction = 0;
            boolean just_do_it = false;
            int blockers_eliminatable = 0;
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
                        } else if(target_loc_from_flag == null) {
                            if(rbt.type.equals(RobotType.MUCKRAKER)) {
                                transferrableConviction += Math.min(rbt.conviction + 1, Math.floor(conv_available / rbts.length));
                            } else if(rbt.type.equals(RobotType.POLITICIAN)) {
                                transferrableConviction += Math.min(rbt.conviction + rbt.influence, conv_available / rbts.length);
                            } else {
                                transferrableConviction += conv_available / rbts.length;
                            }
                        }

                        if(enemy_ec != null
                            && rbt.location.distanceSquaredTo(enemy_ec.location) <= 2
                            && Math.floor(conv_available / rbts.length) >= 1 + rbt.conviction
                        ) {
                            blockers_eliminatable++;
                        }
                    }
                }
            }
            if(convert_mode && !just_do_it) {
                if(blockers_eliminatable > best_blockers_eliminatable) {
                    best_blockers_eliminatable = blockers_eliminatable;
                    best_r_unsquared = r_unsquared;
                }
            } else if(
                rc.canEmpower(actionR2)
                && (
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
                System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()) + " actionR2:" + String.valueOf(actionR2) + " transferrableConviction:" + String.valueOf(transferrableConviction) + " conv_available:" + String.valueOf(conv_available) + " rbts_len:" + String.valueOf(rbts_len) + " just_do_it:" + String.valueOf(just_do_it) + " target_loc_from_flag == null:" + (target_loc_from_flag == null));
                rc.empower(actionR2);
            }
        }
        if(convert_mode
            && best_blockers_eliminatable >= 3 // Important constant here
            && rc.canEmpower(best_r_unsquared * best_r_unsquared)
        ) {
            System.out.println("empowering  convert_mode==true  my conviction:" + String.valueOf(rc.getConviction()) + " best_r_unsquared:" + String.valueOf(best_r_unsquared) + " best_blockers_eliminatable:" + String.valueOf(best_blockers_eliminatable) + " conv_available:" + String.valueOf(conv_available));
            rc.empower(best_r_unsquared * best_r_unsquared);
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
        if(loc_of_ec_to_look_to != null) {
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
        if(threatening_mr != null) {
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
                stepWithPassability(target_rbt.location);
            }
        }
    }

    void moveTowardEnemyMuckrakers() throws GameActionException {
        if(rc.isReady()) {
            RobotInfo target_rbt = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.MUCKRAKER);
            if(target_rbt != null
                && where_i_spawned.distanceSquaredTo(target_rbt.location) < THREATENING_MR_FOLLOW_DIST2
            ) {
                stepWithPassability(target_rbt.location);
            }
        }
    }

    MapLocation loc_to_guard = null;
    final int GUARD_DIST2 = 12*12;
    final int EDGE_DISTANCE = 2;
    boolean spreadNearHome() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        boolean moved = false;
        AverageLocation slan_al = new AverageLocation();
        RobotInfo [] friendly_rbts = rc.senseNearbyRobots(-1, rc.getTeam());
        for(RobotInfo rbt : friendly_rbts) {
            if(rbt.type.equals(RobotType.SLANDERER)) {
                slan_al.add(rbt.location);
            }
        }
        if(       map_max_x != UNKNOWN && map_max_x - myLoc.x < EDGE_DISTANCE) {
            stepWithPassability(myLoc.translate(-1, 0));
        } else if(map_min_x != UNKNOWN && myLoc.x - map_min_x < EDGE_DISTANCE) {
            stepWithPassability(myLoc.translate(1, 0));
        } else if(map_max_y != UNKNOWN && map_max_y - myLoc.y < EDGE_DISTANCE) {
            stepWithPassability(myLoc.translate(0, -1));
        } else if(map_min_y != UNKNOWN && myLoc.y - map_min_y < EDGE_DISTANCE) {
            stepWithPassability(myLoc.translate(0, 1));
        }
        if(!slan_al.is_empty()) {
            loc_to_guard = slan_al.get();
        }
        if(rc.getLocation().distanceSquaredTo(loc_to_guard) >= GUARD_DIST2) {
            moved = stepWithPassability(loc_to_guard);
        } else {
            moved = tryMove(randomDirection());
        }
        return moved;
    }

    void updateAssignmentFromFlag() throws GameActionException {
        // Only does anything on the round I was created
        if(rc.getRoundNum() == roundNumCreated
            && id_of_ec_to_look_to != -1
            && rc.canGetFlag(id_of_ec_to_look_to)
        ) {
            int flag_val = rc.getFlag(id_of_ec_to_look_to);
            if(getMeaningWithoutConv(flag_val) == ASSIGNING_DEFENDER) {
                is_defender = true;
                System.out.println("I'm " + (is_defender ? "a defender" : "an attacker") + " due to flag from the EC");
            } else {
                System.out.println("Not defender");
            }
        }
    }

    MapLocation target_loc_from_flag = null;
    int round_num_of_flag_read = -1;
    void polFlagReceivingStuff() throws GameActionException {
        if(target_loc_from_flag == null) {
            if(rc.canGetFlag(id_of_ec_to_look_to)) {
                int flag_val = rc.getFlag(id_of_ec_to_look_to);
                int meaning = getMeaningWithoutConv(flag_val);
                if(meaning == NEUTRAL_EC
                    || (meaning == ENEMY_EC
                        && rc.getConviction() >= STANDARD_POLITICIAN_INFLUENCE
                    )
                ) {
                    target_loc_from_flag = getMapLocationFromMaskedFlagValue(flag_val);
                    round_num_of_flag_read = rc.getRoundNum();
                    System.out.println("Received target_loc_from_flag:" + target_loc_from_flag.toString());
                }
            }
        } else { // target_loc_from_flag != null
            stepWithPassability(target_loc_from_flag);
        }

        boolean should_clear_target_loc = (
            100 < rc.getRoundNum() - round_num_of_flag_read
        );
        if(target_loc_from_flag != null
            && rc.canSenseLocation(target_loc_from_flag)
        ) {
            RobotInfo rbt = rc.senseRobotAtLocation(target_loc_from_flag);
            if(rbt == null || rbt.team.equals(rc.getTeam())) {
                should_clear_target_loc = true;
            }
        }
        if(should_clear_target_loc) {
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
                if(getMeaningWithoutConv(flag_val) == ENEMY_ROBOT) {
                    enemy_loc_from_flag = getMapLocationFromMaskedFlagValue(flag_val);
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
            if(stepWithPassability(hiding_spot)) {
                System.out.println("HID BASED ON FLAGGED ENEMY at " + enemy_loc_from_flag.toString());
            }
        }
        if(10 < rc.getRoundNum() - round_of_enemy_loc_from_flag) {
            enemy_loc_from_flag = null;
        }
    }

    public void runTurnPolitician() throws GameActionException {
        updateAssignmentFromFlag();
        mapEdgeFlagReceivingStuffNonEc();

        flagEnemies();
        flagMapEdges();
        flagEnemyEcs();
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

            polFlagReceivingStuff();

            moveTowardNonfriendlyEcs();

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
            if(stepWithPassability(myLoc.directionTo(loc_of_nearest_enemy_mr).opposite())) {
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
