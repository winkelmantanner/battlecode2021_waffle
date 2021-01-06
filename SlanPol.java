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
        final int conv_available = rc.getConviction() - 10;
        final MapLocation myLoc = rc.getLocation();
        int actionR2 = rc.getType().actionRadiusSquared;
        int transferrableConviction = 0;
        if(conv_available >= 1) {
            RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
            for(RobotInfo rbt : rbts) {
                System.out.println(String.valueOf(rbt.getID()) + " " + rbt.getLocation().toString() + " " + rbt.getTeam().toString() + " " + rbt.getType().toString() + " " + String.valueOf(rbt.getInfluence()) + " " + String.valueOf(rbt.getConviction()));
                if(rbt.team != rc.getTeam()) {
                    transferrableConviction += (conv_available / rbts.length);
                }
            }
        }
        if (rc.canEmpower(actionR2) &&
            transferrableConviction
            > conv_available
                * recipDecay(
                    conv_available,
                    100
                )
        ) {
            System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()) + " actionR2:" + String.valueOf(actionR2));
            rc.empower(actionR2);
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
