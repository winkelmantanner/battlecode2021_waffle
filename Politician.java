package tannerplayer;
import battlecode.common.*;

public strictfp class Politician extends Unit {
    RobotController rc;
    Politician(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}
