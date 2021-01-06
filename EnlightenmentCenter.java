package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    RobotController rc;
    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        RobotType toBuild = RobotType.SLANDERER;
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                System.out.println("Building " + toBuild.toString() + " to the " + dir.toString());
                rc.buildRobot(toBuild, dir, influence);
            }
        }
        if(rc.canBid(1)) {
            rc.bid(1);
        }
    }
}
