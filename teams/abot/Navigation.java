package abot;

import battlecode.common.*;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
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
	public static void swarmMove(int radius) {
		if (mapDone) {
			complexMove(radius);
		}
		else {
			simpleMove(radius);
		}
	}
	
	/* Attempts to move towards destination */
	public static void moveTowards() {
		swarmMove(1);
	}
	
	/* Simple movement */
	public static void simpleMove(int radius) {

	}
	
	/* Complex movement */
	public static void complexMove(int radius) {
		
	}
	
}
