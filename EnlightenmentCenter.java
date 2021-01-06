package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    RobotController rc;
    int numRobotsBuilt = 0;
    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public RobotType getWhichTypeToBuild() {
        double randomNumber = Math.random();
        RobotType returnable = null;
        if(randomNumber < 0.1) {
            returnable = RobotType.MUCKRAKER;
        } else {
            if(randomNumber < 1 - ((double) rc.getRoundNum()) / GameConstants.GAME_MAX_NUMBER_OF_ROUNDS) {
                returnable = RobotType.SLANDERER;
            } else {
                returnable = RobotType.POLITICIAN;
            }
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
        if(rc.canBid(1)) {
            rc.bid(1);
        }
    }
}
