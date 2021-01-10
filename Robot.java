
package tannerplayer;
import battlecode.common.*;

abstract public strictfp class Robot {
    RobotController rc;
    int roundNumCreated = -1;
    int last_round_conviction = 0;
    MapLocation where_i_spawned = null;
    boolean preferRotateRight = false;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static final Direction[] diagonal_directions = {
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTHWEST,
    };

    Robot(RobotController rbt_controller) {
        rc = rbt_controller;
        roundNumCreated = rc.getRoundNum();
        last_round_conviction = rc.getConviction();
        where_i_spawned = rc.getLocation();
    }

    public int roundNumAtStartOfRound = -1;
    public void beforeRunTurn() {
        roundNumAtStartOfRound = rc.getRoundNum();

        // preferRotateRight == unit ID is odd
        // I used the robot ID because Math.random() returns the same value for different robots.
        preferRotateRight = ((int)(rc.getID() * Math.random())) % 10 < 5;

        // if(rc.getConviction() != last_round_conviction) {
        //     System.out.println("conviction changed Location:" + rc.getLocation().toString() + "  Conviction:" + String.valueOf(rc.getConviction()) + "  last_round_conviction:" + String.valueOf(last_round_conviction));
        // }
    }
    public void afterRunTurn() {
        last_round_conviction = rc.getConviction();
        if(rc.getRoundNum() != roundNumAtStartOfRound) {
            System.out.println("roundNum increased from " + String.valueOf(roundNumAtStartOfRound) + " to " + rc.getRoundNum());
        }
    }

    public void runRobotTurn() throws GameActionException {
        beforeRunTurn();
        runTurn();
        afterRunTurn();
    }
    abstract public void runTurn() throws GameActionException;


    public double recipDecay(final double x, final double half_life) {
        return half_life / (x + half_life);
    }

    int round_when_i_last_set_my_flag = -1;
    public boolean trySetFlag(final int flag_val) throws GameActionException {
        boolean did_set_flag = false;
        if(rc.canSetFlag(flag_val)) {
            rc.setFlag(flag_val);
            did_set_flag = true;
            round_when_i_last_set_my_flag = rc.getRoundNum();
        }
        return did_set_flag;
    }



    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
