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
        final double conv_available = (
            rc.getConviction() * rc.getEmpowerFactor(rc.getTeam(), 0)
        ) - 10;
        for(int r_unsquared = 1; r_unsquared * r_unsquared <= rc.getType().actionRadiusSquared; r_unsquared++) {
            int actionR2 = r_unsquared * r_unsquared;
            double transferrableConviction = 0;
            boolean just_do_it = false;
            if(conv_available >= 1) {
                RobotInfo [] rbts = rc.senseNearbyRobots(actionR2);
                for(RobotInfo rbt : rbts) {
                    if(rbt.team != rc.getTeam()) {
                        if(
                            rbt.type.equals(RobotType.ENLIGHTENMENT_CENTER)
                            && conv_available / rbts.length > rbt.conviction
                        ) {
                            // It will convert the EC: just do it
                            just_do_it = true;
                        }
                        if(rbt.type.equals(RobotType.MUCKRAKER)) {
                            transferrableConviction += Math.min(rbt.conviction, conv_available / rbts.length);
                        } else if(rbt.type.equals(RobotType.POLITICIAN)) {
                            transferrableConviction += Math.min(rbt.conviction + rbt.influence, conv_available / rbts.length);
                        } else {
                            transferrableConviction += conv_available / rbts.length;
                        }
                    }
                }
            }
            if (rc.canEmpower(actionR2) &&
                (
                    just_do_it
                    || (transferrableConviction
                        >= conv_available
                            * recipDecay(
                                rc.getRoundNum() - roundNumCreated,
                                100
                            )
                    )
                )
            ) {
                System.out.println("empowering  my conviction:" + String.valueOf(rc.getConviction()) + " actionR2:" + String.valueOf(actionR2) + " transferrableConviction:" + String.valueOf(transferrableConviction));
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
                fuzzyStep(target_rbt.location);
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
