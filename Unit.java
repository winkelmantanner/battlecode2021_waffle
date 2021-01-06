package tannerplayer;
import battlecode.common.*;

// A Unit is a Robot that can move.

abstract public strictfp class Unit extends Robot {
    RobotController rc;
    Unit(RobotController rbt_controller) {
        super(rbt_controller);
        rc = rbt_controller;
    }
}
