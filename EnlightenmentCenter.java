package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    RobotController rc;
    int numRobotsBuilt = 0;

    // This does not get initialized.
    // Use it to determine if other friendly ECs are bidding.
    int last_round_team_votes = 0;

    boolean i_should_bid = true;
    boolean i_bidded_last_round = false;

    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
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

    double square(final double x) {return x*x;}
    double getMaxFractionOfInfluenceToBid() {
        // Bid up to all influence at the end but almost none at the beginning
        return square(square(square(
            ((double)rc.getRoundNum())
            / GameConstants.GAME_MAX_NUMBER_OF_ROUNDS
        )));
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
            if(Math.random() < 0.9) {
                myBuild(
                    RobotType.MUCKRAKER,
                    1,
                    directions
                );
            } else {
                myBuild(
                    RobotType.POLITICIAN,
                    STANDARD_UNIT_INFLUENCE,
                    directions
                );
            }
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
        if(i_should_bid) {
            int amount = (int) (
                Math.random()
                * rc.getInfluence()
                * getMaxFractionOfInfluenceToBid()
            );
            if(rc.canBid(amount)) {
                rc.bid(amount);
                i_bidded_last_round = true;
                System.out.println("I bidded " + String.valueOf(amount));
            } else {
                System.out.println("I could not bid " + String.valueOf(amount));
            }
            // When amount is zero, the EC will not bid.
            // That is how it sees if other friendly ECs are bidding.
        }
        last_round_team_votes = rc.getTeamVotes();
    }
}
