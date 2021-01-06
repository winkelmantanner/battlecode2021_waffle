package tannerplayer;
import battlecode.common.*;

public strictfp class EnlightenmentCenter extends Robot {
    RobotController rc;
    EnlightenmentCenter(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }
}
