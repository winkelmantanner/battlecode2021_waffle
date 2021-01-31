/**
 * Tanner says:
 * This file is the only required file.
 * Most of the contents of this file come from the examplefuncsplayer provided by Teh Devs.
 * All the other files were created by me, Tanner.
 **/

package tannerplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static int turnCount;

    static Robot me = null;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot becomes unresponsive!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;


        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: me = new EnlightenmentCenter(rc); break;
            case POLITICIAN:           me = new SlanPol(rc);             break;
            case SLANDERER:            me = new SlanPol(rc);             break;
            case MUCKRAKER:            me = new Muckraker(rc);           break;
        }

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                me.runRobotTurn();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
