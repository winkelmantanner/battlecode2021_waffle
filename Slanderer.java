package tannerplayer;
import battlecode.common.*;

public strictfp class Slanderer extends Unit {
    RobotController rc;
    Slanderer(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}
