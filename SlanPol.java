package tannerplayer;
import battlecode.common.*;

public strictfp class SlanPol extends Unit {
    RobotController rc;
    RobotType last_round_type = null;
    int round_of_last_move = -1;
    SlanPol(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
        last_round_type = rc.getType();
    }
    public void runTurnPolitician() throws GameActionException {
        if(rc.getRoundNum() - round_of_last_move < 50 || rc.getRoundNum() % 50 < 5) {
            // The entire purpose of this if is to prevent the client from crashing.

            final int conv_available = (
                (int)(rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0))
            ) - 10;
            final MapLocation myLoc = rc.getLocation();
            for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
                int actionR2 = r_unsquared * r_unsquared;
                int transferrableConviction = 0;
                if(conv_available >= 1) {
                    RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
                    for(RobotInfo rbt : rbts) {
                        // System.out.println(String.valueOf(rbt.getID()) + " " + rbt.getLocation().toString() + " " + rbt.getTeam().toString() + " " + rbt.getType().toString() + " " + String.valueOf(rbt.getInfluence()) + " " + String.valueOf(rbt.getConviction()));
                        if(rbt.team != rc.getTeam()) {
                            transferrableConviction += (conv_available / rbts.length);
                        }
                    }
                }
                if (rc.canEmpower(actionR2) &&
                    transferrableConviction
                    >= conv_available
                        * recipDecay(
                            rc.getRoundNum() - roundNumCreated,
                            100
                        )
                ) {
                    System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()) + " actionR2:" + String.valueOf(actionR2));
                    rc.empower(actionR2);
                }
            }
        }
        if(tryMove(randomDirection())) {
            round_of_last_move = rc.getRoundNum();
        }
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
