package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    int numRobotsBuilt = 0;
    int [] robots_i_built = new int[GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_HEIGHT];

    // This does not get initialized.
    // Use it to determine if other friendly ECs are bidding.
    int last_round_team_votes = 0;

    boolean i_should_bid = false;
    boolean i_bidded_last_round = false;
    int round_when_i_last_bidded = -1;

    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
    }

    int current_built_robot_array_index = 0;
    void doFlagStuff() throws GameActionException {
        if(20 < rc.getRoundNum() - round_when_i_last_set_my_flag) {
            // Because trySetFlag sets round_when_i_last_set_my_flag, this runs once per 20 rounds
            if(trySetFlag(0)) {
                System.out.println("set my flag to 0");
            }
        }
        while(
            current_built_robot_array_index < numRobotsBuilt
            && Clock.getBytecodesLeft() > 1000
            && rc.getRoundNum() != round_when_i_last_set_my_flag
        ) {
            int target_robot_id = robots_i_built[current_built_robot_array_index];
            if(rc.canGetFlag(target_robot_id)) {
                int flag_val = rc.getFlag(target_robot_id);
                if(flag_val != 0) {
                    if(trySetFlag(flag_val)) {
                        // Because trySetFlag sets round_when_i_last_set_my_flag,
                        //   the loop will exit after this iteration.
                        // The ++ statement at the end needs to execute still.
                    }
                }
            }
            current_built_robot_array_index++;
        }
        if(current_built_robot_array_index >= numRobotsBuilt) {
            current_built_robot_array_index = 0;
        }
    }

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

    final int OPTIMAL_SLANDERER_INFLUENCE = 21; // https://www.desmos.com/calculator/ydkbaqrx7v
    final int STANDARD_UNIT_INFLUENCE = 50;
    public void runTurn() throws GameActionException {
        //At the very beginning, build a few slanderers
        if(rc.getRoundNum() <= 3) {
            myBuild(
                RobotType.SLANDERER,
                OPTIMAL_SLANDERER_INFLUENCE,
                diagonal_directions
            );
        } else if(!getIfAll4CardinalDirectionsAreOccupied()) {
            // Make sure we have the basic shield of muckrakers
            myBuild(
                RobotType.MUCKRAKER,
                1,
                Direction.cardinalDirections()
            );
        } else if(rc.getInfluence() > STANDARD_UNIT_INFLUENCE) {
            if(Math.random() < 0.75) {
                myBuild(
                    RobotType.MUCKRAKER,
                    1,
                    directions
                );
            } else {
                int influence = STANDARD_UNIT_INFLUENCE;
                if(rc.getInfluence() - STANDARD_UNIT_INFLUENCE > 20 * STANDARD_UNIT_INFLUENCE) {
                    influence = STANDARD_UNIT_INFLUENCE + (int)(
                        Math.random() * (rc.getInfluence() / 2)
                    );
                }
                myBuild(
                    RobotType.POLITICIAN,
                    influence,
                    directions
                );
            }
        }

        if(
            rc.getRoundNum() - round_when_i_last_bidded > 100
            && rc.getRoundNum() > (rc.getID() % 1000)
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
            int amount = (int) (Math.random() * rc.getInfluence() / 20);
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
        doFlagStuff();
    }
}
