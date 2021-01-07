package tannerplayer;
import battlecode.common.*;

public strictfp class Muckraker extends Unit {
    RobotController rc;
    MapLocation where_i_saw_enemy_slanderer = null;
    int round_when_i_saw_enemy_slanderer = -1;
    Muckraker(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
    public void runTurn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation myLoc = rc.getLocation();

        /*
        Note, within the sensor radius, I can detect all attributes of robots.
        Within the detection radius, I can only detect their location.
        So use the sensor radius.
        */
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if(
                    round_when_i_saw_enemy_slanderer != rc.getRoundNum()
                    || where_i_saw_enemy_slanderer == null
                    || myLoc.distanceSquaredTo(robot.location) < myLoc.distanceSquaredTo(where_i_saw_enemy_slanderer)
                ) {
                    where_i_saw_enemy_slanderer = robot.location;
                    round_when_i_saw_enemy_slanderer = rc.getRoundNum();
                }
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }

        // Try to move toward enemy slanderers seen recently
        if (
            where_i_saw_enemy_slanderer != null
            && 30 > rc.getRoundNum() - round_when_i_saw_enemy_slanderer
            && fuzzyStep(where_i_saw_enemy_slanderer)
        ) {
            System.out.println("I (muckraker) stepped toward target: " + where_i_saw_enemy_slanderer.toString());
        }

        tryMove(randomDirection());
    }
}
