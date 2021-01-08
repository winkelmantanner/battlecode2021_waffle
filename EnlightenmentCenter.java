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
    public RobotType getWhichTypeToBuild() {
        double randomNumber = Math.random();
        RobotType returnable = null;
        if(randomNumber < 0.2) {
            returnable = RobotType.MUCKRAKER;
        } else if(randomNumber < 0.7) {
            returnable = RobotType.POLITICIAN;
        } else {
            returnable = RobotType.SLANDERER;
        }
        return returnable;
    }
    public void runTurn() throws GameActionException {
        RobotType toBuild = getWhichTypeToBuild();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                System.out.println("Building " + toBuild.toString() + " to the " + dir.toString());
                rc.buildRobot(toBuild, dir, influence);
                numRobotsBuilt++;
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
            int amount = (int) (Math.random() * rc.getInfluence() / 20);
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
