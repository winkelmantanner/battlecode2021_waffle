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


    int getValueFromNumBlockersDestroyed(final int num_blockers) {
        return 5 * num_blockers * num_blockers * num_blockers;
    }
    void attackerEmpower() throws GameActionException {
        final double conv_available = getEmpowerConvAvailable();

        RobotInfo enemy_ec = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.ENLIGHTENMENT_CENTER);
        final boolean convert_mode = getConvertMode(enemy_ec);

        int best_action_r2 = -1;
        double best_transf_conv = 0;

        for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
            final int actionR2 = r_unsquared * r_unsquared;
            double transferrableConviction = 0;
            int num_blockers_destroyed = 0;
            boolean just_do_it = false;
            int rbts_len = -1; // This variable is used for logs only
            if(conv_available >= 1) {
                RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
                rbts_len = rbts.length;
                final double conv_per_rbt = conv_available / rbts.length;
                for(RobotInfo rbt : rbts) {
                    if(rbt.team != rc.getTeam()) {
                        if(
                            rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                            && conv_per_rbt > rbt.conviction
                        ) {
                            // It will convert the EC: just do it
                            just_do_it = true;
                        } else if(target_loc_from_flag == null) {
                            // For defenders, target_loc_from_flag will always be null
                            if(rbt.type.equals(RobotType.MUCKRAKER)) {
                                transferrableConviction += Math.min(rbt.conviction + 1, Math.floor(conv_per_rbt));
                            } else if(rbt.type.equals(RobotType.POLITICIAN)) {
                                transferrableConviction += Math.min(rbt.conviction + rbt.influence, conv_per_rbt);
                            } else {
                                transferrableConviction += conv_per_rbt;
                            }
                        }
                        if(enemy_ec != null
                            && convert_mode
                            && conv_per_rbt >= 1 + rbt.conviction
                            && rbt.location.isAdjacentTo(enemy_ec.location)
                        ) {
                            num_blockers_destroyed++;
                        }
                    }
                }
            }
            // if !convert_mode, num blockers is 0, so this value will be 0
            transferrableConviction += getValueFromNumBlockersDestroyed(num_blockers_destroyed);
            if(just_do_it) {
                transferrableConviction += 12345;
            }
            // System.out.println("actionR2" + actionR2 + " convert_mode" + convert_mode + " num_blockers_destroyed" + num_blockers_destroyed + " transferrableConviction:" + transferrableConviction);

            if(
                rc.canEmpower(actionR2)
                && transferrableConviction > best_transf_conv
            ) {
                best_transf_conv = transferrableConviction;
                best_action_r2 = actionR2;
            }
        }
        
        if(rc.canEmpower(best_action_r2)
            && best_transf_conv
                >= conv_available
                    * recipDecay(
                        rc.getRoundNum() - round_num_became_pol,
                        100
                    )
        ) {
            System.out.println("empowering  convert_mode:" + convert_mode + " my conviction:" + String.valueOf(rc.getConviction()) + " best_action_r2:" + String.valueOf(best_action_r2) + " best_transf_conv:" + String.valueOf(best_transf_conv) + " conv_available:" + String.valueOf(conv_available) + " target_loc_from_flag == null:" + (target_loc_from_flag == null));
            rc.empower(best_action_r2);
        }
    }

    void empowerIfDamagedBeyondUsability() throws GameActionException {
        if(getEmpowerConvAvailable() <= 0 && rc.canEmpower(1)) {
            rc.empower(1);
            System.out.println("Empowered due to being damaged beyond usability");
        }
    }

    final int DESTRUCTION_VALUE = 5;
    final int DISABLE_VALUE = 3;
    final int IN_GUARD_RANGE_MULTIPLIER = 4;
    void defenderEmpower() throws GameActionException {
        final int [] r2s = {1, 2, 9};
        int best_val = -1;
        int best_r2 = -1;
        double unbuffed_conv_available = rc.getConviction() - GameConstants.EMPOWER_TAX;
        double buffed_conv_available = getEmpowerConvAvailable();
        for(int r2 : r2s) {
            int value = 0;
            RobotInfo [] rbts = rc.senseNearbyRobots(r2);
            for(RobotInfo rbt : rbts) {
                int value_gain = 0;
                if(rbt.team.equals(rc.getTeam().opponent())) {
                    if(buffed_conv_available / rbts.length >= 1 + rbt.conviction) {
                        value_gain = DESTRUCTION_VALUE + 1 + rbt.conviction;
                    }
                }
                if(loc_to_guard != null
                    && loc_to_guard.distanceSquaredTo(rbt.location) <= GUARD_DIST2
                ) {
                    value_gain *= IN_GUARD_RANGE_MULTIPLIER;
                }
                value += value_gain;
            }
            if(value > best_val) {
                best_r2 = r2;
                best_val = value;
            }
        }
        if(best_val >= unbuffed_conv_available
            && rc.canEmpower(best_r2)
        ) {
            System.out.println("empowering on enemy  myconviction:" + String.valueOf(rc.getConviction()) + " best_r2:" + String.valueOf(best_r2) + " buffed_conv_available:" + String.valueOf(buffed_conv_available) + " best_val:" + best_val);
            rc.empower(best_r2);
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

    void moveTowardSensableEnemies() throws GameActionException {
        if(rc.isReady()) {
            RobotInfo target_rbt = nearestRobot(null, -1, rc.getTeam().opponent(), null);
            if(target_rbt != null
                && getEmpowerConvAvailable() >= 1 + target_rbt.conviction
                && where_i_spawned.distanceSquaredTo(target_rbt.location) < GUARD_DIST2
            ) {
                stepWithPassability(target_rbt.location);
            }
        }
    }

    final int GUARD_DIST2 = 14*14;
    void moveTowardEnemiesFromFlag() throws GameActionException {
        // This is for heavier defenders
        // PRE: receiveEnemyLocFromFlag must be called regularly
        if(enemy_loc_from_flag != null
            && loc_of_ec_to_look_to != null
        ) {
            double conv_available = getEmpowerConvAvailable();
            if(conv_available >= 1 + enemy_max_conv_from_flag
                && conv_available <= IN_GUARD_RANGE_MULTIPLIER * (1 + enemy_min_conv_from_flag) // Note: min
                && loc_of_ec_to_look_to.distanceSquaredTo(enemy_loc_from_flag) <= GUARD_DIST2
            ) {
                if(stepWithPassability(enemy_loc_from_flag)) {
                    System.out.println("Stepped toward flagged enemy enemy_loc_from_flag:" + enemy_loc_from_flag + " enemy_max_conv_from_flag:" + enemy_max_conv_from_flag + " enemy_min_conv_from_flag:" + enemy_min_conv_from_flag);
                }
            }
        }
    }

    MapLocation loc_to_guard = null; // This is set to the spawn loc in the ctor
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

        if(!slan_al.is_empty()) {
            loc_to_guard = slan_al.get();
        }

        if(loc_to_guard != null
            && myLoc.distanceSquaredTo(loc_to_guard) > GUARD_DIST2
        ) {
            stepWithPassability(loc_to_guard);
        } else {
            for(int k = 0; k < 10; k++) {
                if(tryMove(randomDirection())) {
                    break;
                }
            }
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
        // FOR ATTACKERS ONLY
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
    int enemy_max_conv_from_flag = -1;
    int enemy_min_conv_from_flag = -1;
    int round_of_enemy_loc_from_flag = -1;
    void receiveEnemyLocFromFlag() throws GameActionException {
        if(enemy_loc_from_flag == null) {
            if(rc.canGetFlag(id_of_ec_to_look_to)) {
                int flag_val = rc.getFlag(id_of_ec_to_look_to);
                if(getMeaningWithoutConv(flag_val) == ENEMY_ROBOT) {
                    enemy_loc_from_flag = getMapLocationFromMaskedFlagValue(flag_val);
                    enemy_max_conv_from_flag = getMaxConvFromFlagVal(flag_val);
                    enemy_min_conv_from_flag = getMinConvFromFlagVal(flag_val);
                    round_of_enemy_loc_from_flag = rc.getRoundNum();
                }
            }
        }
        if(10 < rc.getRoundNum() - round_of_enemy_loc_from_flag) {
            enemy_loc_from_flag = null;
            enemy_max_conv_from_flag = -1;
            enemy_min_conv_from_flag = -1;
        }
    }
    void slanFlagReceivingStuff() throws GameActionException {
        // PRE: receiveEnemyLocFromFlag must be called regularly
        if(enemy_loc_from_flag != null) {
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
    }

    int round_num_became_pol = -1;
    public void runTurnPolitician() throws GameActionException {
        if(round_num_became_pol == -1) {
            round_num_became_pol = rc.getRoundNum();
        }
        updateAssignmentFromFlag();
        mapEdgeFlagReceivingStuffNonEc();
        receiveEnemyLocFromFlag();

        flagEnemies();
        flagAndUpdateMapEdges();
        flagEnemyEcs();
        flagNeutralECs();
        // The later flag overrides the earlier

        empowerIfDamagedBeyondUsability();

        if(is_defender) { // influence is not reduced by damage
            // I'm a defender
            defenderEmpower();

            moveTowardSensableEnemies();

            if(rc.getInfluence() > MAX_DEFENDER_INFLUENCE) {
                // We're a heavy defender
                moveTowardEnemiesFromFlag();

                spreadNearHome();
            } else {
                exploreMove();
            }
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
        receiveEnemyLocFromFlag();

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
        for(int k = 0; k < 10; k++) {
            if(tryMove(randomDirection())) {
                break;
            }
        }
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
