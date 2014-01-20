package abot;

import battlecode.common.*;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static final int[] reversedLooks = new int[] {4, -3, 3, -2, 2, -1, 1, 0};
	
	public static int[] intdirs;
	public static RobotController rc;
	public static int width;
	public static int height;
	
	public static boolean mapDone;
	public static MapLocation dest;
	public static int[] mapinfo;
	
	/* Initializes navigation to know the width and height */
	public static void init(RobotController rcin) {
		init(rcin, null);
	}
	
	public static void init(RobotController rcin, MapLocation defaultLoc) {
		mapDone = false;
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		intdirs = new int[] {-1, height - 1, height, height + 1, 1, 1 - height, -height, -1 - height};
		if (defaultLoc != null) {
			setDest(defaultLoc);
		}
		else {
			setDest(new MapLocation(width/2, height/2));
		}
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination) {
		dest = destination;
		mapinfo = new int[width * height];
	}
	
	/* Attempts to move to within a radius around destination */
	public static void swarmMove(int radius) throws GameActionException {
		if (mapDone) {
			complexMove(radius);
		}
		else {
			simpleMove(radius);
		}
	}
	
	/* Attempts to move towards destination */
	public static void moveTo() throws GameActionException {
		swarmMove(1);
	}
	
	/* Trivial movement */
	public static void trivialMove(MapLocation mapLoc) throws GameActionException {
		int toDest = rc.getLocation().directionTo(mapLoc).ordinal();
		for (int i = 8; i-- > 0;) {
			Direction moveDir = dir[(toDest + reversedLooks[i] + 8) % 8];
			if (rc.canMove(moveDir)) {
				rc.move(moveDir);
				return;
			}
		}
	}
	
	/* Simple movement */
	public static void simpleMove(int radius) throws GameActionException {
		
	}
	
	/* Complex movement */
	public static void complexMove(int radius) throws GameActionException {
		
	}
	
	private static void updateAdjacent(int loc, int amount) {
		int dist = mapinfo[loc] + amount;
		for (int i = 8; i-- > 0;) {
			if (loc + intdirs[i] >= 0) {
				if ((mapinfo[loc + intdirs[i]] / 9) > dist) {
					mapinfo[loc + intdirs[i]] = (dist * 9) + ((i + 4) % 8) + 1;
				}
			}
		}
	}
	
}
