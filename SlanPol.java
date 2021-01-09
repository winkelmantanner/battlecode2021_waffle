package tannerplayer;
import battlecode.common.*;

public strictfp class SlanPol extends Unit {
    RobotType last_round_type = null;

    int id_of_ec_to_look_to = -1;

    SlanPol(RobotController rbt_controller) {
        super(rbt_controller);
        last_round_type = rc.getType();

        for(RobotInfo rbt : rc.senseNearbyRobots(
            RobotType.ENLIGHTENMENT_CENTER.actionRadiusSquared,
            rc.getTeam()
        )) {
            if(RobotType.ENLIGHTENMENT_CENTER.equals(rbt.type)) {
                id_of_ec_to_look_to = rbt.ID;
            }
        }
    }

    void empowerIfApplicable() throws GameActionException {
        final int conv_available = (
            (int)(rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0))
        ) - 10;
        for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
            int actionR2 = r_unsquared * r_unsquared;
            int transferrableConviction = 0;
            if(conv_available >= 1) {
                RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
                for(RobotInfo rbt : rbts) {
                    // System.out.println(String.valueOf(rbt.getID()) + " " + rbt.getLocation().toString() + " " + rbt.getTeam().toString() + " " + rbt.getType().toString() + " " + String.valueOf(rbt.getInfluence()) + " " + String.valueOf(rbt.getConviction()));
                    if(rbt.team != rc.getTeam()) {
                        if(rbt.type.equals(RobotType.MUCKRAKER)) {
                            transferrableConviction += rbt.conviction;
                        } else {
                            transferrableConviction += (conv_available / rbts.length);
                        }
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

    void moveTowardSensableRobotsIfApplicable() throws GameActionException {
        if(rc.isReady()) {
            int dist2_to_nearest_target = 12345;
            RobotInfo target_rbt = null;
            RobotInfo [] rbts = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            for(RobotInfo rbt : rbts) {
                if(
                    rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                    && !rbt.team.equals(rc.getTeam())
                    && rc.getLocation().distanceSquaredTo(rbt.location) < dist2_to_nearest_target
                ) {
                    target_rbt = rbt;
                    dist2_to_nearest_target = rc.getLocation().distanceSquaredTo(rbt.location);
                }
            }
            if(target_rbt != null) {
                if(fuzzyStep(target_rbt.location)) {
                    System.out.println("I (politician) stepped toward " + target_rbt.location.toString());
                } else {
                    System.out.println("I (politician) failed to step toward " + target_rbt.location.toString());
                }
            }
        }
    }

    void flagReceivingStuff() throws GameActionException {
        if(target_loc_from_flag == null) {
            if(rc.canGetFlag(id_of_ec_to_look_to)) {
                int flag_val = rc.getFlag(id_of_ec_to_look_to);
                if(flag_val >> 16 == NEUTRAL_EC) {
                    int x = (int)((byte)((flag_val >> 8) & 0b11111111));
                    int y = (int)((byte)(flag_val & 0b11111111));
                    target_loc_from_flag = where_i_spawned.translate(x, y);
                    System.out.println("Moving by flag toward " + target_loc_from_flag.toString() + " x:" + String.valueOf(x) + " y:" + String.valueOf(y) + " where_i_spawned:" + where_i_spawned.toString());
                    round_num_of_flag_read = rc.getRoundNum();
                }
            }
        } else { // target_loc_from_flag != null
            fuzzyStep(target_loc_from_flag);
        }
        if(50 < rc.getRoundNum() - round_num_of_flag_read) {
            target_loc_from_flag = null;
        }
    }

    MapLocation target_loc_from_flag = null;
    int round_num_of_flag_read = -1;

    public void runTurnPolitician() throws GameActionException {
        flagNeutralECs();

        empowerIfApplicable();

        moveTowardSensableRobotsIfApplicable();

        flagReceivingStuff();

        exploreMove();
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
