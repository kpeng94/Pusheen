package navBot;

import battlecode.common.*;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static final int[] reversedLooks = new int[] {4, -3, 3, -2, 2, -1, 1, 0};
	
	public static int[] intdirs;
	public static RobotController rc;
	public static int width;
	public static int height;

	public static boolean mapDone; // Able to use complex movement with HQ map

	public static boolean pathDone; // Path generation complete
	public static MapLocation dest; // Destination location
	public static int radius; // Radius to swarm around destination
	public static int[] mapinfo; // Explored map info
	
	public static MapLocation curCheck;
	public static int checkNum;
	public static boolean isRoad;
	
	/* Initializes navigation to know the width and height */
	public static void init(RobotController rcin) {
		init(rcin, null, 1);
	}
	
	public static void init(RobotController rcin, MapLocation defaultLoc, int radius) {
		mapDone = false;
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		intdirs = new int[] {-1, height - 1, height, height + 1, 1, 1 - height, -height, -1 - height};
		if (defaultLoc != null) {
			setDest(defaultLoc, radius);
		}
		else {
			setDest(new MapLocation(width/2, height/2), radius);
		}
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination) {
		setDest(destination, 1);
	}
	
	public static void setDest(MapLocation destination, int rad) {
		if (dest != destination) {
			pathDone = false;
			dest = destination;
			mapinfo = new int[width * height];
			curCheck = rc.getLocation();
			checkNum = 0;
			isRoad = rc.senseTerrainTile(curCheck) == TerrainTile.ROAD;
		}
		radius = rad;
	}
	
	/* Attempts to move to within a radius around destination */
	public static void swarmMove() throws GameActionException {
		if (mapDone) {
			complexMove();
		}
		else {
			simpleMove();
		}
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
	public static void simpleMove() throws GameActionException {
		if (!pathDone) {
			simpleCalculate();
		}
		backTrace(rc.getLocation());
	}
	
	/* Complex movement */
	public static void complexMove() throws GameActionException {
		
	}
	
	/* Calculates paths during idle time */
	public static void calculate() {
		if (mapDone) {
			complexCalculate();
		}
		else {
			simpleCalculate();
		}
	}
	
	public static void simpleCalculate() {
		while (Clock.getBytecodesLeft() > 2000) {
			if (curCheck.distanceSquaredTo(dest) <= radius) {
				pathDone = true;
				return;
			}
			int toDest = curCheck.directionTo(dest).ordinal();
			
			int minRound = Integer.MAX_VALUE;
			MapLocation minNext = null;
			boolean minRoad = false;
			
			for (int i = 8; i-- > 0;) {
				MapLocation next = curCheck.add(dir[(toDest + reversedLooks[i] + 8) % 8]);
				int roundNum = (mapinfo[toInt(next)] % 90000) / 9;
				TerrainTile tile = rc.senseTerrainTile(next);
				if (tile == TerrainTile.VOID || tile == TerrainTile.OFF_MAP) {
					continue;
				}
				if (roundNum == 0) {
					updateAdjacent(toInt(curCheck), isRoad ? 1 : 2);
					curCheck = next;
					isRoad = tile == TerrainTile.ROAD;
					mapinfo[toInt(curCheck)] += checkNum * 9;
					checkNum++;
					break;
				}
				else {
					if (roundNum < minRound) {
						minRound = roundNum;
						minNext = next;
						minRoad = tile == TerrainTile.ROAD;
					}
				}
			}
			if (minNext != null) {
				updateAdjacent(toInt(curCheck), isRoad ? 1 : 2);
				curCheck = minNext;
				isRoad = minRoad;
				mapinfo[toInt(curCheck)] += checkNum * 9;
				checkNum++;
			}
		}
	}
	
	private static void updateAdjacent(int loc, int amount) {
		int dist = (mapinfo[loc] / 90000) + amount;
		for (int i = 8; i-- > 0;) {
			int intloc = loc + intdirs[i];
			if (intloc >= 0 && intloc < width * height) {
				int oldDist = mapinfo[intloc] / 90000;
				if (oldDist == 0 || oldDist > dist) {
					mapinfo[intloc] = (dist * 90000) + ((i + 4) % 8) + 1;
				}
			}
		}
	}
	
	private static void backTrace(MapLocation startPos) throws GameActionException {
		int start = toInt(startPos);
		int end = toInt(curCheck);
		int backDir = 0;
		while (end != start) {
			backDir = (mapinfo[end] % 9) - 1;
			end += intdirs[backDir];
		}
		
		Direction moveDir = dir[(backDir + 4) % 8];
		if (rc.canMove(moveDir)) {
			mapinfo[start] = 90000 + (checkNum * 9) + (backDir + 4) % 8 + 1;
			checkNum++;
			rc.move(moveDir);
		}
	}
	
	public static void complexCalculate() {
		
	}
	
	public static int toInt(MapLocation loc) {
		return loc.x * height + loc.y;
	}
	
	public static int toInt(int x, int y) {
		return x * height + y;
	}
	
}
