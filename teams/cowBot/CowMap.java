package cowBot;

import battlecode.common.*;

public class CowMap {
	public static final Direction[] checkDirs = {Direction.SOUTH_EAST, Direction.SOUTH, Direction.EAST, Direction.NONE};
	
	public static RobotController rc;
	public static int width;
	public static int height;
	public static int halfwidth;
	public static int halfheight;
	public static MapLocation ourHQ;
	
	public static boolean mapDone;
	public static boolean bestDone;
	
	public static double[][] cowBase;
	public static double[][] cowMap;
	public static int curPos;
	
	public static MapLocation bestLoc;
	public static double bestCows;
	public static int bestDist;
	
	/* Initializes the cow map */
	public static void init(RobotController rcin) {
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		halfwidth = (width + 1) / 2;
		halfheight = (height + 1) / 2;
		ourHQ = rc.senseHQLocation();
		
		cowBase = rc.senseCowGrowth();
		cowMap = new double[halfwidth][halfheight];
		mapDone = false;
		curPos = width * height - 1;
		bestCows = -1;
	}
	
	/* Does calculation to populate the map */
	public static void calculate() throws GameActionException {
		if (!mapDone) {
			calculateCows();
		}
		else if (!bestDone) {
			calculateBest();
//			System.out.println(Clock.getBytecodesLeft());
		}
//		else {
//			System.out.println("Best: " + bestLoc.x + " " + bestLoc.y + ": " + bestCows);
//		}
	}
	
	/* Calculates the coarse map of cows */
	private static void calculateCows() {
		while (Clock.getBytecodesLeft() > 1000) {
			if (curPos < 0) {
				curPos = (halfwidth - 1) * (halfheight - 1) - 1;
				mapDone = true;
				return;
			}
			int curx = curPos / height;
			int cury = curPos % height;
			cowMap[curx / 2][cury / 2] += cowBase[curx][cury];
			curPos--;
		}
	}
	
	/* Gets the best cow spawn map locations */
	private static void calculateBest() throws GameActionException {
		while (Clock.getBytecodesLeft() > 1000) {
			if (curPos < 0) {
				rc.broadcast(10, bestLoc.x * 100 + bestLoc.y);
				bestDone = true;
				return;
			}
			int curx = curPos / (halfheight - 1);
			int cury = curPos % (halfheight - 1);
			double cows = cowMap[curx][cury] + cowMap[curx+1][cury] + cowMap[curx][cury+1] + cowMap[curx+1][cury+1];
			if (cows > bestCows) {
				checkBest(cows, new MapLocation(2*curx + 1, 2*cury + 1));
			}
			else if (cows == bestCows) {
				MapLocation loc = new MapLocation(2*curx + 1, 2*cury + 1);
				if (ourHQ.distanceSquaredTo(loc) < bestDist) {
					checkBest(cows, loc);
				}
			}
			curPos--;
		}
	}
	
	/* Checks a 2x2 region to make sure that its not void */
	private static void checkBest(double cows, MapLocation loc) {
		for (int i = checkDirs.length; i-- > 0;) {
			MapLocation newLoc = loc.add(checkDirs[i]);
			if (newLoc.x == ourHQ.x && newLoc.y == ourHQ.y) {
				continue;
			}
			if (rc.senseTerrainTile(newLoc) != TerrainTile.VOID) {
				bestCows = cows;
				bestLoc = newLoc;
				bestDist = ourHQ.distanceSquaredTo(bestLoc);
			}
		}
	}
	
}
