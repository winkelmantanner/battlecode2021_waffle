package tannerplayer;
import battlecode.common.*;

public strictfp class Muckraker extends Unit {
    RobotController rc;
    Muckraker(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection())) {}
    }
}
