package tannerplayer;
import battlecode.common.*;

public strictfp class SlanPol extends Unit {
    RobotController rc;
    RobotType last_round_type = null;
    SlanPol(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
        last_round_type = rc.getType();
    }
    public void runTurnPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()));
            rc.empower(actionRadius);
            return;
        }
        if (tryMove(randomDirection())) {}
        // if(rc.getRoundNum() == 1950) {
        //     System.out.println("my conviction:" + String.valueOf(rc.getConviction()));
        // }
    }
    public void runTurnSlanderer() throws GameActionException {
        if (tryMove(randomDirection())) {}
    }
    public void runTurn() throws GameActionException {
        switch (rc.getType()) {
            case POLITICIAN: runTurnPolitician(); break;
            case SLANDERER:  runTurnSlanderer();  break;
        }
        last_round_type = rc.getType();
    }
}
