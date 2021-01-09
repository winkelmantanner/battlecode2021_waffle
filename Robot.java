
package tannerplayer;
import battlecode.common.*;

abstract public strictfp class Robot {
    RobotController rc;
    int roundNumCreated = -1;
    int last_round_conviction = 0;

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
    }

    public int roundNumAtStartOfRound = -1;
    public void beforeRunTurn() {
        roundNumAtStartOfRound = rc.getRoundNum();
        if(rc.getConviction() != last_round_conviction) {
            System.out.println("conviction changed Location:" + rc.getLocation().toString() + "  Conviction:" + String.valueOf(rc.getConviction()) + "  last_round_conviction:" + String.valueOf(last_round_conviction));
        }
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
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
