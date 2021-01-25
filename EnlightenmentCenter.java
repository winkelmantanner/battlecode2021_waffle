package tannerplayer;
import battlecode.common.*;

import java.util.HashMap;

public strictfp class EnlightenmentCenter extends Robot {
    final int SHIELD_FACTOR = 50;
    final int MUCKRAKER_INFLUENCE = 1;

    // This does not get initialized.
    // Use it to determine if other friendly ECs are bidding.
    int last_round_team_votes = 0;

    double passability_of_my_tile = -1;

    boolean i_should_bid = false;
    boolean i_bidded_last_round = false;
    int round_when_i_last_bidded = -12345;

    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
    }

    int shield_conviction = 0;

    boolean should_build_slans = true;
    final int [] SLAN_STEPS = {21, 41, 63, 85, 107, 154, 228, 282, 339, 400}; // https://www.desmos.com/calculator/ydkbaqrx7v
    int getAmountToPutInSlan(final int available_influence) {
        int amount = SLAN_STEPS[0];
        for(int k = 0; k < SLAN_STEPS.length; k++) {
            if(available_influence >= SLAN_STEPS[k]) {
                amount = SLAN_STEPS[k];
            }
        }
        return amount;
    }

    boolean broadcastMapEdgeIfApplicable(
        final int which_edge,
        final int coord
    ) throws GameActionException {
        boolean did_broadcast = false;
        if(coord != UNKNOWN) {
            did_broadcast = trySetFlag(getValueForFlagRaw(which_edge, (short)coord));
        }
        return did_broadcast;
    }
    int numRobotsBuilt = 0;
    int [] robots_i_built = new int[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];
    int current_built_robot_array_index = 0;

    MapLocation enemy_loc_to_broadcast = null;
    int enemy_flag_round = -1;

    MapLocation neutral_ec_loc_to_broadcast = null;
    int neutral_ec_flag_round = -1;
    int neutral_ec_max_conv = -1;
    int getInfForNecConverter() {
        return (neutral_ec_max_conv == -1 ? -1 : (2 + GameConstants.EMPOWER_TAX + neutral_ec_max_conv));
    }

    MapLocation enemy_slanderer_loc_to_broadcast = null;
    int enemy_slanderer_flag_round = -1;

    MapLocation enemy_ec_loc_to_broadcast = null;
    int enemy_ec_flag_round = -1;

    int round_defender_was_built = -12345;

    void doFlagStuff(RobotInfo nearest_enemy) throws GameActionException {

        // NOT standardFlagReset
        if(rc.canSetFlag(0)) {
            trySetFlag(0);
        }

        if(2 == rc.getRoundNum() - round_last_built_nec_converter) {
            int flag_val = getValueForFlagMaskedLocation(
                getNeutralEcMeaningWithConv(NEUTRAL_EC, neutral_ec_max_conv),
                last_nec_converter_target_loc
            );
            if(trySetFlag(flag_val)) {
                necs_done.put(last_nec_converter_target_loc, true);
                System.out.println("Flagged for nec converter: " + String.valueOf(flag_val));
            } else {
                System.out.println("FAILED TO SET NEC FLAG FOR POL I MADE LAST ROUND");
            }
        } else if(1 == rc.getRoundNum() - round_defender_was_built) {
            int flag_val = getValueForFlagRaw(ASSIGNING_DEFENDER, (short)0);
            if(trySetFlag(flag_val)) {
                System.out.println("ASSIGNED DEFENDER");
            }
        } else {
            if(nearest_enemy != null) {
                enemy_loc_to_broadcast = nearest_enemy.location;
                enemy_flag_round = rc.getRoundNum();
            } else if(rc.getRoundNum() - enemy_flag_round > 5) {
                enemy_loc_to_broadcast = null;
            }
            if(rc.getRoundNum() - neutral_ec_flag_round > 20) {
                neutral_ec_loc_to_broadcast = null;
                neutral_ec_max_conv = -1;
            }
            if(rc.getRoundNum() - enemy_slanderer_flag_round > 10) {
                enemy_slanderer_loc_to_broadcast = null;
            }
            if(rc.getRoundNum() - enemy_ec_flag_round > 30) {
                enemy_ec_loc_to_broadcast = null;
            }

            switch((rc.getRoundNum() % MAX_FLAG_MEANING_VALUE) + 1) {
                case NEUTRAL_EC:
                    if(neutral_ec_loc_to_broadcast != null) {
                        trySetFlag(getValueForFlagMaskedLocation(
                            getNeutralEcMeaningWithConv(NEUTRAL_EC, neutral_ec_max_conv),
                            neutral_ec_loc_to_broadcast
                        ));
                    }
                    break;
                case ENEMY_ROBOT:
                    if(enemy_loc_to_broadcast != null) {
                        trySetFlag(getValueForFlagMaskedLocation(ENEMY_ROBOT, enemy_loc_to_broadcast));
                    }
                    break;
                case MAP_MAX_X:   broadcastMapEdgeIfApplicable(MAP_MAX_X, map_max_x);  break;
                case MAP_MIN_X:   broadcastMapEdgeIfApplicable(MAP_MIN_X, map_min_x);  break;
                case MAP_MAX_Y:   broadcastMapEdgeIfApplicable(MAP_MAX_Y, map_max_y);  break;
                case MAP_MIN_Y:   broadcastMapEdgeIfApplicable(MAP_MIN_Y, map_min_y);  break;
                case ENEMY_SLANDERER: 
                    if(enemy_slanderer_loc_to_broadcast != null) {
                        trySetFlag(getValueForFlagMaskedLocation(ENEMY_SLANDERER, enemy_slanderer_loc_to_broadcast));
                    }
                    break;
                case ENEMY_EC:
                    if(enemy_ec_loc_to_broadcast != null) {
                        trySetFlag(getValueForFlagMaskedLocation(ENEMY_EC, enemy_ec_loc_to_broadcast));
                    }
                    break;
            }
        }

        while(
            current_built_robot_array_index < numRobotsBuilt
            && Clock.getBytecodesLeft() > 1000
        ) {
            int target_robot_id = robots_i_built[current_built_robot_array_index];
            if(rc.canGetFlag(target_robot_id)) {
                int flag_val = rc.getFlag(target_robot_id);
                if(flag_val != 0) {
                    boolean was_flag_map_edge = updateMapEdgesBasedOnFlagIfApplicable(flag_val);
                    if(!was_flag_map_edge) {
                        switch(getMeaningWithoutConv(flag_val)) {
                            case ENEMY_ROBOT:
                                // If we (the EC) see an enemy, flag it instead of what other robots say. 
                                // This only runs if we saw an ENEMY_ROBOT flag from another robot.
                                if(nearest_enemy == null) {
                                    enemy_loc_to_broadcast = getMapLocationFromMaskedFlagValue(flag_val);
                                    enemy_flag_round = rc.getRoundNum();
                                }
                                break;
                            case NEUTRAL_EC:
                                final MapLocation ml = getMapLocationFromMaskedFlagValue(flag_val);
                                final int max_conv = getMaxConvFromMeaning(flag_val);
                                if(!necs_done.containsKey(ml)
                                    && (neutral_ec_max_conv == -1
                                        || max_conv < neutral_ec_max_conv
                                    )
                                ) {
                                    neutral_ec_loc_to_broadcast = ml;
                                    neutral_ec_flag_round = rc.getRoundNum();
                                    neutral_ec_max_conv = max_conv;
                                    System.out.println("Got better nec max_conv:" + max_conv + " ml:" + ml);
                                }
                                break;
                            case ENEMY_SLANDERER:
                                enemy_slanderer_loc_to_broadcast = getMapLocationFromMaskedFlagValue(flag_val);
                                enemy_slanderer_flag_round = rc.getRoundNum();
                                break;
                            case ENEMY_EC:
                                enemy_ec_loc_to_broadcast = getMapLocationFromMaskedFlagValue(flag_val);
                                enemy_ec_flag_round = rc.getRoundNum();
                                break;
                        }
                    }
                }
            }
            current_built_robot_array_index++;
        }
        if(current_built_robot_array_index >= numRobotsBuilt) {
            current_built_robot_array_index = 0;
        }
    }

    int num_defenders_built = 0;
    boolean myBuild(final RobotType type, final int influence, final Direction [] dirs) throws GameActionException {
        double max_passability = 0;
        Direction best_build_dir = null;
        for(Direction d : dirs) {
            if(rc.canBuildRobot(type, d, influence)) {
                MapLocation ml = rc.adjacentLocation(d);
                double p = rc.sensePassability(ml);
                if(p > max_passability) {
                    max_passability = p;
                    best_build_dir = d;
                }
            }
        }
        boolean built = false;
        if(best_build_dir != null) {
            rc.buildRobot(type, best_build_dir, influence);
            robots_i_built[numRobotsBuilt] = rc.senseRobotAtLocation(rc.adjacentLocation(best_build_dir)).ID;
            numRobotsBuilt++;
            built = true;
        }
        return built;
    }

    boolean getIfAll4CardinalDirectionsAreOccupied() throws GameActionException {
        boolean are_all_4_cardinals_occupied = true;
        for(Direction card_dir : Direction.cardinalDirections()) {
            MapLocation ml = rc.adjacentLocation(card_dir);
            if(
                rc.canDetectLocation(ml)
                && !rc.isLocationOccupied(ml)
            ) {
                are_all_4_cardinals_occupied = false;
                break;
            }
        }
        return are_all_4_cardinals_occupied;
    }

    final int SLAN_BUILD_INTERVAL = 3;
    int round_when_i_last_built_slan = -12345;
    int num_slans_built = 0;

    HashMap<MapLocation, Boolean> necs_done = new HashMap<MapLocation, Boolean>();

    int round_last_built_nec_converter = -12345;
    MapLocation last_nec_converter_target_loc = null;

    final double SLAN_DEFENDER_RATIO = 1.75;

    int round_last_built_attacker = -12345;
    final int MAX_ATTACKER_INF = 1000;

    public void runTurnRobot() throws GameActionException {
        shield_conviction = SHIELD_FACTOR * getEcPassiveIncome(rc.getRoundNum());
        int available_influence = rc.getInfluence() - shield_conviction;
        if(passability_of_my_tile < 0
            && rc.canSenseLocation(rc.getLocation())
        ) {
            passability_of_my_tile = rc.sensePassability(rc.getLocation());
        }

        // This is used in more than one place
        RobotInfo nearest_enemy = nearestRobot(null, -1, rc.getTeam().opponent(), null);
        RobotInfo nearest_enemy_muck = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.MUCKRAKER);
        RobotInfo nearest_enemy_pol = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.POLITICIAN);

        boolean in_second_half = rc.getRoundNum() > 0.5 * GameConstants.GAME_MAX_NUMBER_OF_ROUNDS;

        boolean need_another_defender = (
            (num_slans_built * SLAN_DEFENDER_RATIO)
                - num_defenders_built
                >= 10
            || (
                50 < rc.getRoundNum() - round_defender_was_built
                && in_second_half
            )
        );

        if(nearest_enemy_muck != null) {
            if(should_build_slans) {
                System.out.println("SETTINGS should_build_slans to false");
            }
            should_build_slans = false;
        }
        if(should_build_slans
            && !need_another_defender
            && rc.getRoundNum() - round_when_i_last_built_slan
                >= 2 * rc.getType().actionCooldown / passability_of_my_tile
            && available_influence >= shield_conviction + SLAN_STEPS[0]
        ) {
            if(myBuild(
                RobotType.SLANDERER,
                getAmountToPutInSlan(available_influence),
                diagonal_directions
            )) {
                round_when_i_last_built_slan = rc.getRoundNum();
                num_slans_built++;
            }
        } else if(
            nearest_enemy_pol != null
            && !getIfAll4CardinalDirectionsAreOccupied()
        ) {
            // Make sure we have the basic shield of muckrakers
            myBuild(
                RobotType.MUCKRAKER,
                1,
                Direction.cardinalDirections()
            );
        } else if(
            neutral_ec_loc_to_broadcast != null
            && !necs_done.containsKey(neutral_ec_loc_to_broadcast)
            && available_influence > getInfForNecConverter()
            && rc.getRoundNum() - round_last_built_nec_converter > 10
        ) {
            int influence = getInfForNecConverter();
            if(myBuild(
                RobotType.POLITICIAN,
                influence,
                directions
            )) {
                round_last_built_nec_converter = rc.getRoundNum();
                last_nec_converter_target_loc = neutral_ec_loc_to_broadcast;
                System.out.println("Built nec converter for " + String.valueOf(influence));
            }
        } else if(
            rc.getInfluence() >= 2000
            && rc.getRoundNum() % 7 <= 2
        ) {
            if(myBuild(
                RobotType.POLITICIAN,
                (int)(Math.random() * rc.getInfluence()),
                directions
            )) {}
        } else if(
            need_another_defender
            && available_influence > MAX_DEFENDER_INFLUENCE
        ) {
            int influence = randInt(MIN_DEFENDER_INFLUENCE, MAX_DEFENDER_INFLUENCE);
            while(Math.random() < 0.25) {
                influence *= 2;
            }
            boolean building_defender = true;
            if(myBuild(
                RobotType.POLITICIAN,
                influence,
                directions
            )) {
                if(building_defender) {
                    System.out.println("Built defender");
                    num_defenders_built++;
                    round_defender_was_built = rc.getRoundNum();
                }
            }
        } else if(
            rc.getInfluence() >= 5 + MUCKRAKER_INFLUENCE // Note: this is NOT available_influence
        ) {
            int influence = 1;
            if(available_influence > STANDARD_POLITICIAN_INFLUENCE
                && enemy_slanderer_loc_to_broadcast != null
                && Math.random() < 0.25
            ) {
                influence = 1 + (int)(Math.random() * STANDARD_POLITICIAN_INFLUENCE);
            }
            myBuild(
                RobotType.MUCKRAKER,
                influence,
                directions
            );
        }

        // The bidding system is really poorly documented.
        // See https://github.com/battlecode/battlecode21/blob/276480addacefb543e4470cbe3a4be7ff05dc659/engine/src/main/battlecode/world/GameWorld.java#L370
        int amount = (int) (
            Math.random() * available_influence
                / (SHIELD_FACTOR / 2) // divide by 2 since Math.random averages 0.5
            // This will result in the conviction converging on 2*shield_conviction
        );
        if(rc.canBid(amount)) {
            rc.bid(amount);
            i_bidded_last_round = true;
            round_when_i_last_bidded = rc.getRoundNum();
            System.out.println("I bidded " + String.valueOf(amount));
        } else {
            System.out.println("I could not bid " + String.valueOf(amount));
        }
        last_round_team_votes = rc.getTeamVotes();

        // This needs to be last since it goes until the bytecode limit is hit
        doFlagStuff(nearest_enemy);
    }
}
