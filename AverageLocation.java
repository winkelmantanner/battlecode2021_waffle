package tannerplayer;
import battlecode.common.*;

public class AverageLocation {
    public double total_x = 0;
    public double total_y = 0;
    public double total_weight = 0;
    public boolean is_empty = true;
    public void add(final MapLocation loc, final double weight) {
        total_x += (loc.x * weight);
        total_y += (loc.y * weight);
        total_weight += weight;
        is_empty = false;
    }
    public void add(final MapLocation loc) {
        add(loc, 1);
    }
    public boolean is_empty() {
        return is_empty;
    }
    public MapLocation get() {
        return new MapLocation(
            (int)((total_x) / total_weight),
            (int)((total_y) / total_weight)
        );
    }
}
