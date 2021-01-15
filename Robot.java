
package tannerplayer;
import battlecode.common.*;

abstract public strictfp class Robot {
    RobotController rc;
    int roundNumCreated = -1;
    int last_round_conviction = 0;
    MapLocation where_i_spawned = null;
    boolean preferRotateRight = false;

    final int MAX_DEFENDER_INFLUENCE = 24;
    final int MIN_DEFENDER_INFLUENCE = 12;

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


    int getEcPassiveIncome(final int round_num) {
        return (int)(
            GameConstants.PASSIVE_INFLUENCE_RATIO_ENLIGHTENMENT_CENTER
            * Math.sqrt(round_num)
        );
    }


    public double recipDecay(final double x, final double half_life) {
        return half_life / (x + half_life);
    }
    public double uniformRandom(final double min, final double max) {
        return (Math.random() * (max - min)) + min;
    }
    public int randInt(final int min, final int max) {
        return (int)uniformRandom(min, max + 1);
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
    void standardFlagReset() throws GameActionException {
        if(5 < rc.getRoundNum() - round_when_i_last_set_my_flag) {
            // Because trySetFlag sets round_when_i_last_set_my_flag, this runs once per 20 rounds
            if(trySetFlag(0)) {
                System.out.println("set my flag to 0");
            }
        }
    }

    final int NEUTRAL_EC = 1;
    final int ENEMY_ROBOT = 2;
    int getValueForFlag(
        final int what_the_flag_represents,
        MapLocation loc
    ) {
        // where_i_spawned will be close to, but not exactly, the same for units from the same EC.
        // So there will be known error because of where_i_spawned.

        // These two variables need to be int type.
        // If they are byte type, if they are negative they will be incorrectly casted to a negative int.
        int x = 0b11111111 & (loc.x - where_i_spawned.x);
        int y = 0b11111111 & (loc.y - where_i_spawned.y);
        int result = what_the_flag_represents;
        result <<= 8;
        result |= x;
        result <<= 8;
        result |= y;

        return result;
    }
    MapLocation getMapLocationFromFlagValue(final int flag_value) {
        // the flag must be from a robot from the same EC as us
        int dx = (int)((byte)((flag_value >> 8) & 0b11111111));
        int dy = (int)((byte)(flag_value & 0b11111111));
        return where_i_spawned.translate(dx, dy);
    }


    boolean flagNeutralECs() throws GameActionException {
        standardFlagReset();

        boolean did_set_flag = false;
        int neutral_ec_id = -1;
        RobotInfo neutral_ec = null;
        for(RobotInfo rbt : rc.senseNearbyRobots(
            rc.getType().sensorRadiusSquared,
            Team.NEUTRAL
        )) {
            neutral_ec = rbt;
        }
        if(neutral_ec != null) {
            int value_for_flag = getValueForFlag(
                NEUTRAL_EC,
                neutral_ec.location
            );
            if(trySetFlag(value_for_flag)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
    }

    boolean flagEnemies() throws GameActionException {
        standardFlagReset();

        boolean did_set_flag = false;
        int x_sum = 0;
        int y_sum = 0;
        int count = 0;
        for(RobotInfo rbt : rc.senseNearbyRobots(
            rc.getType().sensorRadiusSquared,
            rc.getTeam().opponent()
        )) {
            x_sum += rbt.location.x;
            y_sum += rbt.location.y;
            count++;
        }
        if(count > 0) {
            MapLocation enemy_centroid = new MapLocation(x_sum / count, y_sum / count);
            int flag_val = getValueForFlag(
                ENEMY_ROBOT,
                enemy_centroid
            );
            if(trySetFlag(flag_val)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
    }

    RobotInfo nearestRobot(
        final MapLocation center, // null for rc.getLocation()
        final int radiusSquared, // -1 for sensorRadiusSquared
        final Team team, // null for either team
        final RobotType type // null for any type
    ) {
        MapLocation nonnull_center = (center == null ? rc.getLocation() : center);
        RobotInfo nearest = null;
        int dist2_to_nearest = 123456;
        for(RobotInfo rbt : rc.senseNearbyRobots(
            nonnull_center,
            radiusSquared,
            team
        )) {
            if(type == null || type.equals(rbt.type)) {
                int dist2 = nonnull_center.distanceSquaredTo(rbt.location);
                if(dist2 < dist2_to_nearest) {
                    nearest = rbt;
                    dist2_to_nearest = dist2;
                }
            }
        }
        return nearest;
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
