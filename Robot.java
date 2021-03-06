
package tannerplayer;
import battlecode.common.*;
import java.lang.IllegalArgumentException;

abstract public strictfp class Robot {
    RobotController rc;
    int roundNumCreated = -1;
    int last_round_conviction = 0;
    MapLocation where_i_spawned = null;
    boolean preferRotateRight = false;
    double sensor_radius_nonsquared = -1;

    final int STANDARD_POLITICIAN_INFLUENCE = 50;
    final int MUCKRAKER_INFLUENCE = 1;

    final int UNKNOWN = -1;
    int map_max_x = UNKNOWN;
    int map_min_x = UNKNOWN;
    int map_max_y = UNKNOWN;
    int map_min_y = UNKNOWN;

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
        sensor_radius_nonsquared = Math.sqrt(rc.getType().sensorRadiusSquared);
    }

    public int roundNumAtStartOfRound = -1;
    public void beforeRunTurn() {
        roundNumAtStartOfRound = rc.getRoundNum();

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
        runTurnRobot();
        afterRunTurn();
    }
    abstract public void runTurnRobot() throws GameActionException;


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
            trySetFlag(0);
        }
    }

    final int NEUTRAL_EC = 1;
    final int ENEMY_ROBOT = 2;
    final int MAP_MAX_X = 3;
    final int MAP_MIN_X = 4;
    final int MAP_MAX_Y = 5;
    final int MAP_MIN_Y = 6;
    final int ENEMY_SLANDERER = 7;
    final int ENEMY_EC = 8;
    final int MAX_FLAG_MEANING_VALUE = ENEMY_EC; // Round num is modded by this
    final int ASSIGNING_DEFENDER = 15;
    public int getMasked(final int coord_unmasked) {
        return 0xFF & coord_unmasked;
    }
    int getValueForFlagMaskedLocation(
        final int what_the_flag_represents,
        MapLocation loc
    ) {
        int x = getMasked(loc.x);
        int y = getMasked(loc.y);
        return (((what_the_flag_represents << 8) | x) << 8) | y;
    }
    int getValueForFlagRaw(
        final int what_the_flag_represents,
        final short last_2_bytes
    ) {
        int result = what_the_flag_represents;
        result <<= 16;
        result |= ((int)last_2_bytes) & 0xFFFF;
        return result;
    }
    int getOriginalMapCoordFromMasked(
        final int masked_coord,
        final int my_coord_unmasked
    ) {
        int my_coord_masked = (my_coord_unmasked & 0xFF);

        int d1 = (masked_coord + 0x100) - my_coord_masked;
        int d2 = masked_coord - my_coord_masked;
        int d3 = masked_coord - (my_coord_masked + 0x100);

        int a1 = Math.abs(d1);
        int a2 = Math.abs(d2);
        int a3 = Math.abs(d3);

        int a_min = Math.min(a1, Math.min(a2, a3));

        int d_actual = 0;
        if(a_min == a1) {
            d_actual = d1;
        } else if(a_min == a2) {
            d_actual = d2;
        } else { // a_min == a3
            d_actual = d3;
        }

        return my_coord_unmasked + d_actual;
    }
    MapLocation getMapLocationFromMaskedFlagValue(final int flag_value) {
        MapLocation myLoc = rc.getLocation();
        int masked_x = (flag_value >> 8) & 0xFF;
        int masked_y = flag_value & 0xFF;
        int x = getOriginalMapCoordFromMasked(masked_x, myLoc.x);
        int y = getOriginalMapCoordFromMasked(masked_y, myLoc.y);
        return new MapLocation(x, y);
    }
    int getMeaningWithoutConv(final int flag_val) {
        return ((flag_val >> 16) & 0b1111);
    }

    // Powers of phi, length 16
    // Each entry is ceil(phi**index)
    final int [] CONV_VALS = {1, 2, 3, 5, 7, 12, 18, 30, 47, 76, 123, 199, 322, 521, 843, 1364};
    int getMeaningWithConv(final int meaning_wo_conv, final int conviction) {
        int idx = 0;
        for(int k = 0; k < CONV_VALS.length; k++) {
            if(conviction <= CONV_VALS[k]) {
                idx = k;
                break;
            }
        }
        return ((idx << 4) | meaning_wo_conv);
    }
    int getMaxConvFromFlagVal(final int flag_val_w_conv) {
        return CONV_VALS[(flag_val_w_conv >> 16) >> 4];
    }
    int getMinConvFromFlagVal(final int flag_val_w_conv) {
        int idx = (flag_val_w_conv >> 16) >> 4;
        if(idx <= 0) {
            return 0;
        } else {
            return 1 + CONV_VALS[idx - 1];
        }
    }


    boolean flagNeutralECs() throws GameActionException {
        standardFlagReset();

        boolean did_set_flag = false;
        int neutral_ec_id = -1;
        RobotInfo neutral_ec = nearestRobot(null, -1, Team.NEUTRAL, null);
        if(neutral_ec != null) {
            int value_for_flag = getValueForFlagMaskedLocation(
                getMeaningWithConv(
                    NEUTRAL_EC,
                    neutral_ec.conviction
                ),
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
        RobotInfo nearest_enemy = nearestRobot(null, -1, rc.getTeam().opponent(), null);
        if(nearest_enemy != null) {
            int flag_val = getValueForFlagMaskedLocation(
                getMeaningWithConv(ENEMY_ROBOT, nearest_enemy.conviction),
                nearest_enemy.location
            );
            if(trySetFlag(flag_val)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
    }

    boolean flagEnemyEcs() throws GameActionException {
        boolean did_set_flag = false;
        RobotInfo enemy_ec = nearestRobot(null, -1, rc.getTeam().opponent(), RobotType.ENLIGHTENMENT_CENTER);
        if(enemy_ec != null) {
            int flag_val = getValueForFlagMaskedLocation(
                ENEMY_EC,
                enemy_ec.location
            );
            if(trySetFlag(flag_val)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
    }

    boolean flagAndUpdateMapEdges() throws GameActionException {
        standardFlagReset();
        return flagAndUpdateSpecificMapEdge(MAP_MAX_X)
            || flagAndUpdateSpecificMapEdge(MAP_MIN_X)
            || flagAndUpdateSpecificMapEdge(MAP_MAX_Y)
            || flagAndUpdateSpecificMapEdge(MAP_MIN_Y);
    }
    boolean flagAndUpdateSpecificMapEdge(
        final int which_edge // must be MAP_MAX_X, MAP_MIN_X, MAP_MAX_Y, or MAP_MIN_Y
    ) throws GameActionException {
        int dx = 0;
        int dy = 0;
        boolean is_y = false; 
        switch(which_edge) {
            case MAP_MAX_X:  dx =  1;  is_y = false;  break;
            case MAP_MIN_X:  dx = -1;  is_y = false;  break;
            case MAP_MAX_Y:  dy =  1;  is_y = true;   break;
            case MAP_MIN_Y:  dy = -1;  is_y = true;   break;
            default:
                throw new IllegalArgumentException("flagMapEdge which_edge invalid value: " + String.valueOf(which_edge));
        }
        boolean did_set_flag = false;
        MapLocation myLoc = rc.getLocation();
        int extreme_value = UNKNOWN;
        boolean map_edge_detected = false;
        for(
            int k = (int)sensor_radius_nonsquared;
            k >= 0;
            k--
        ) {
            MapLocation l = myLoc.translate(dx*k, dy*k);
            if(rc.onTheMap(l)) {
                extreme_value = (is_y ? l.y : l.x);
                break;
            } else {
                map_edge_detected = true;
            }
        }
        if(map_edge_detected) {
            switch(which_edge) {
                case MAP_MAX_X:  map_max_x = extreme_value;  break;
                case MAP_MIN_X:  map_min_x = extreme_value;  break;
                case MAP_MAX_Y:  map_max_y = extreme_value;  break;
                case MAP_MIN_Y:  map_min_y = extreme_value;  break;
            }
            int value_for_flag = getValueForFlagRaw(which_edge, (short)extreme_value);
            if(trySetFlag(value_for_flag)) {
                did_set_flag = true;
            }
        }
        return did_set_flag;
    }

    boolean updateMapEdgesBasedOnFlagIfApplicable(final int flag_val) {
        boolean did = false;
        switch(getMeaningWithoutConv(flag_val)) {
            case MAP_MAX_X:  map_max_x = (flag_val & 0xFFFF);  did = true;  break;
            case MAP_MIN_X:  map_min_x = (flag_val & 0xFFFF);  did = true;  break;
            case MAP_MAX_Y:  map_max_y = (flag_val & 0xFFFF);  did = true;  break;
            case MAP_MIN_Y:  map_min_y = (flag_val & 0xFFFF);  did = true;  break;
            // default intentionally excluded
        }
        return did;
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


    double getEmpowerConvAvailable(final int conv) throws GameActionException {
        return (
            conv - GameConstants.EMPOWER_TAX
        ) * rc.getEmpowerFactor(rc.getTeam(), 0);
    }
    double getEmpowerConvAvailable() throws GameActionException {
        return getEmpowerConvAvailable(rc.getConviction());
    }
    boolean getConvertMode(RobotInfo enemy_ec) throws GameActionException {
        boolean convert_mode = false;
        if(enemy_ec != null) {
            double friendlyConvInArea = 0;
            RobotInfo [] friendlyRbts = rc.senseNearbyRobots(-1, rc.getTeam());
            for(RobotInfo rbt : friendlyRbts) {
                if(rbt.type.equals(RobotType.POLITICIAN)) {
                    friendlyConvInArea += getEmpowerConvAvailable(rbt.conviction);
                }
            }
            convert_mode = (friendlyConvInArea > 1.5 * enemy_ec.conviction);
        }
        return convert_mode;
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
