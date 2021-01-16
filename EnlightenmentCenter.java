package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    final int SHIELD_FACTOR = 50;
    final int STANDARD_POLITICIAN_INFLUENCE = 50;
    final int MUCKRAKER_INFLUENCE = 1;

    // This does not get initialized.
    // Use it to determine if other friendly ECs are bidding.
    int last_round_team_votes = 0;

    boolean i_should_bid = false;
    boolean i_bidded_last_round = false;
    int round_when_i_last_bidded = -1;

    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
    }

    int shield_conviction = 0;

    boolean should_build_slans = true;
    final int [] SLAN_STEPS = {21, 107, 400}; // https://www.desmos.com/calculator/ydkbaqrx7v
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
    void doFlagStuff(RobotInfo nearest_enemy) throws GameActionException {
        standardFlagReset();
        switch(rc.getRoundNum() % 10) {
            case MAP_MAX_X:  broadcastMapEdgeIfApplicable(MAP_MAX_X, map_max_x);  break;
            case MAP_MIN_X:  broadcastMapEdgeIfApplicable(MAP_MIN_X, map_min_x);  break;
            case MAP_MAX_Y:  broadcastMapEdgeIfApplicable(MAP_MAX_Y, map_max_y);  break;
            case MAP_MIN_Y:  broadcastMapEdgeIfApplicable(MAP_MIN_Y, map_min_y);  break;
            default:
                while(
                    current_built_robot_array_index < numRobotsBuilt
                    && Clock.getBytecodesLeft() > 1000
                    && rc.getRoundNum() != round_when_i_last_set_my_flag
                ) {
                    int target_robot_id = robots_i_built[current_built_robot_array_index];
                    if(rc.canGetFlag(target_robot_id)) {
                        int flag_val = rc.getFlag(target_robot_id);
                        if(flag_val != 0) {
                            boolean was_flag_map_edge = updateMapEdgesBasedOnFlagIfApplicable(flag_val);
                            if(!was_flag_map_edge) {
                                if(flag_val >> 16 == ENEMY_ROBOT
                                    && nearest_enemy != null
                                ) {
                                    // If we (the EC) see an enemy, flag it instead of what other robots say. 
                                    // This only runs if we san an ENEMY_ROBOT flag from another robot.
                                    flag_val = getValueForFlagRelative(
                                        ENEMY_ROBOT,
                                        nearest_enemy.location
                                    );
                                }
                                if(trySetFlag(flag_val)) {
                                    // Because trySetFlag sets round_when_i_last_set_my_flag,
                                    //   the loop will exit after this iteration.
                                    // The ++ statement at the end needs to execute still.
                                }
                            }
                        }
                    }
                    current_built_robot_array_index++;
                }
                if(current_built_robot_array_index >= numRobotsBuilt) {
                    current_built_robot_array_index = 0;
                }
                break;
        }
    }

    boolean should_build_pols = true;
    int total_inf_spent_on_pols = 0;
    int influence_to_put_into_next_politician = STANDARD_POLITICIAN_INFLUENCE;
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

    boolean built_slan_last = false;
    int round_when_i_last_built_slan = -12345;

    public void runTurnRobot() throws GameActionException {
        shield_conviction = SHIELD_FACTOR * getEcPassiveIncome(rc.getRoundNum());
        System.out.println("passive ec income: " + String.valueOf(getEcPassiveIncome(rc.getRoundNum())) + " shield:" + String.valueOf(shield_conviction));
        int available_influence = rc.getInfluence() - shield_conviction;

        // This is used in more than one place
        RobotInfo nearest_enemy = nearestRobot(null, -1, rc.getTeam().opponent(), null);

        if(nearest_enemy != null) {
            should_build_slans = false;
        }
        if(should_build_slans && !built_slan_last) {
            if(myBuild(
                RobotType.SLANDERER,
                getAmountToPutInSlan(available_influence),
                diagonal_directions
            )) {
                round_when_i_last_built_slan = rc.getRoundNum();
                built_slan_last = true;
            }
        } else if(
            nearest_enemy != null
            && !getIfAll4CardinalDirectionsAreOccupied()
        ) {
            // Make sure we have the basic shield of muckrakers
            myBuild(
                RobotType.MUCKRAKER,
                1,
                Direction.cardinalDirections()
            );
        } else if(
            should_build_pols
            && (
                total_inf_spent_on_pols < 1000
                || rc.getTeamVotes() > rc.getRoundNum() * 0.6
            )
            && available_influence
              > shield_conviction
              // This will mean 2*sheild_conviction is required to build pols
              // The total influence will converge on 2*shield_conviction by the bidding code
        ) {
            System.out.println("I have enough inf");
            if(myBuild(
                RobotType.POLITICIAN,
                influence_to_put_into_next_politician,
                directions
            )) {
                built_slan_last = false;
                total_inf_spent_on_pols += influence_to_put_into_next_politician;
                if(rc.getRoundNum() - round_when_i_last_built_slan > 0.75 * GameConstants.CAMOUFLAGE_NUM_ROUNDS
                    || influence_to_put_into_next_politician <= MAX_DEFENDER_INFLUENCE
                ) {
                    influence_to_put_into_next_politician = STANDARD_POLITICIAN_INFLUENCE;
                } else {
                    influence_to_put_into_next_politician = randInt(
                        MIN_DEFENDER_INFLUENCE,
                        MAX_DEFENDER_INFLUENCE
                    );
                }
                System.out.println("I want to put " + String.valueOf(influence_to_put_into_next_politician) + " into my next pol");
            }
        } else if(available_influence >= MUCKRAKER_INFLUENCE) {
            myBuild(
                RobotType.MUCKRAKER,
                MUCKRAKER_INFLUENCE,
                directions
            );
        }

        if(
            rc.getRoundNum() - round_when_i_last_bidded > 100
            && rc.getRoundNum() > (rc.getID() % 500)
        ) {
            i_should_bid = true;
        }
        if(
            rc.getTeamVotes() > last_round_team_votes
            && !i_bidded_last_round
        ) {
            if(i_should_bid) {
                System.out.println("changing i_should_bid to false");
            }
            // Another friendly EC is bidding
            i_should_bid = false;
        }
        i_bidded_last_round = false;
        if(
            i_should_bid
            && Math.random() < 0.9
        ) {
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
            // When amount is zero, the EC will not bid.
            // That is how it sees if other friendly ECs are bidding.
        }
        last_round_team_votes = rc.getTeamVotes();

        // This needs to be last since it goes until the bytecode limit is hit
        doFlagStuff(nearest_enemy);
    }
}
