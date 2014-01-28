package mapBot;

import battlecode.common.*;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static final int[] reversedAll = new int[] {4, -3, 3, -2, 2, -1, 1, 0};
	static final int[] reversedForward = new int[] {-1, 1, 0};
	static final int checkDist = 4;
	
	public static int[] intdirs;
	public static RobotController rc;
	public static int width;
	public static int height;
	public static MapLocation ourHQ;
	public static MapLocation enemyHQ;

	public static boolean nearHQ; // Destination is near the enemy HQ
	
	public static boolean mapDone; // Able to use complex movement with HQ map

	public static boolean pathDone; // Path generation complete
	public static MapLocation dest; // Destination location
	public static int radius; // Radius to swarm around destination
	public static int[] mapinfo; // Explored map info
	
	public static int[] path;
	public static int curPathPos;
	
	public static MapLocation curCheck;
	public static int checkNum;
	public static boolean isRoad;
	
	/* Initializes navigation to know the width and height */
	public static void init(RobotController rcin) throws GameActionException {
		init(rcin, null, 1);
	}
	
	public static void init(RobotController rcin, MapLocation defaultLoc, int radius) throws GameActionException {
		mapDone = false;
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		intdirs = new int[] {-1, height - 1, height, height + 1, 1, 1 - height, -height, -1 - height};
		
		ourHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		
		if (defaultLoc != null) {
			setDest(defaultLoc, radius);
		}
		else {
			setDest(new MapLocation(width/2, height/2), radius);
		}
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination) throws GameActionException {
		setDest(destination, 1);
	}
	
	public static void setDest(MapLocation destination, int rad) throws GameActionException {
		destination = getNearestOpenDest(destination);
		rc.setIndicatorString(0, "" + destination);
		if (dest == null || (dest.x != destination.x && dest.y != destination.y)) {
			pathDone = false;
			dest = destination;
			mapinfo = new int[width * height];
			curCheck = rc.getLocation();
			checkNum = 0;
			isRoad = Map.getTile(curCheck) == 2;
			if (dest.distanceSquaredTo(enemyHQ) <= 25) {
				nearHQ = true;
			}
		}
		radius = rad;
	}
	
	/* Returns nearest movable square */
	private static MapLocation getNearestOpenDest(MapLocation destination) throws GameActionException {
		MapLocation newDest = destination;
		int tile = Map.getTile(newDest);
		int rad = 1;
		int offset = 7;
		while (tile == 3 || tile == 4) {
			newDest = destination.add(dir[offset], rad);
			tile = Map.getTile(newDest);
			if (offset-- <= 0) {
				offset = 7;
				rad++;
			}
		}
		return newDest;
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
	public static void trivialMove(MapLocation mapLoc, int[] looks) throws GameActionException {
		trivialMove(mapLoc, looks, false);
	}
	
	public static void trivialMove(MapLocation mapLoc, int[] looks, boolean tryDanger) throws GameActionException {
		MapLocation curLoc = rc.getLocation();
		int toDest = curLoc.directionTo(mapLoc).ordinal();
		for (int i = looks.length; i-- > 0;) {
			Direction moveDir = dir[(toDest + looks[i] + 8) % 8];
			if (rc.canMove(moveDir) && (tryDanger || Map.getTile(curLoc.add(moveDir)) != 5)) {
				rc.move(moveDir);
				return;
			}
		}
	}
	
	/* Simple movement */
	public static void simpleMove() throws GameActionException {
		if (!pathDone) {
			simpleCalculate(5000);
			backTrace(rc.getLocation());
		}
		else if (curPathPos >= 0) {
			pathMove();
		}
		else {
			trivialMove(dest, reversedAll);
		}
	}
	
	/* Complex movement */
	public static void complexMove() throws GameActionException {
		
	}
	
	/* Calculates paths during idle time */
	public static void calculate() throws GameActionException {
		if (mapDone) {
			complexCalculate();
		}
		else {
			simpleCalculate();
		}
	}
	
	public static void simpleCalculate() throws GameActionException {
		simpleCalculate(2500);
	}
	
	public static void simpleCalculate(int bytelimit) throws GameActionException {
		while (Clock.getBytecodesLeft() > bytelimit) {
			if (curCheck.distanceSquaredTo(dest) <= radius) {
				return;
			}
			int toDest = curCheck.directionTo(dest).ordinal();
			
			int minRound = Integer.MAX_VALUE;
			MapLocation minNext = null;
			boolean minRoad = false;
			
			for (int i = 8; i-- > 0;) {
				MapLocation next = curCheck.add(dir[(toDest + reversedAll[i] + 8) % 8]);
				int intloc = toInt(next);
				if (intloc < 0 || intloc > width * height) {
					continue;
				}
				int roundNum = (mapinfo[intloc] % 90000) / 9;
				int tile = Map.getTile(next);
				
				if (nearHQ && (tile == 3 || tile == 4)) {
					continue;
				}
				if (!nearHQ && tile > 2) {
					continue;
				}
				
				if (roundNum == 0) {
					updateAdjacent(toInt(curCheck), isRoad ? 1 : 2);
					curCheck = next;
					isRoad = tile == 2;
					mapinfo[toInt(curCheck)] += checkNum * 9;
					checkNum++;
					break;
				}
				else {
					if (roundNum < minRound) {
						minRound = roundNum;
						minNext = next;
						minRoad = tile == 2;
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
		if (curCheck.distanceSquaredTo(dest) <= radius) {
			pathDone = true;
		}
		int start = toInt(startPos);
		int end = toInt(curCheck);
		curPathPos = 0;
		path = new int[width + height];
		path[curPathPos] = end;
		
		while (intDist(start, end) > checkDist && curPathPos + 1 < path.length) {
			end += intdirs[(mapinfo[end] % 9) - 1];
			if (intDist(start, end) > checkDist) {
				end += intdirs[(mapinfo[end] % 9) - 1];
			}
			curPathPos++;
			path[curPathPos] = end;
		}
		
		pathMove();
		
//		Direction moveDir = dir[(backDir + 4) % 8];
//		
//		if (rc.canMove(moveDir)) {
//			mapinfo[start] = 90000 + (checkNum * 9) + (backDir + 4) % 8 + 1;
//			checkNum++;
//			rc.move(moveDir);
//		}
	}
	
	private static void pathMove() throws GameActionException {
		MapLocation pos = fromInt(path[curPathPos]);
		trivialMove(pos, reversedAll);
		if (rc.getLocation().distanceSquaredTo(pos) < checkDist + 1) {
			curPathPos--;
		}
	}
	
	public static void complexCalculate() {
		
	}
	
	public static int intDist(int pos1, int pos2) {
		int xdiff = (pos1 / height) - (pos2 / height);
		int ydiff = (pos1 % height) - (pos2 % height);
		return xdiff * xdiff + ydiff * ydiff;
	}
	
	public static MapLocation fromInt(int loc) {
		return new MapLocation(loc / height, loc % height);
	}
	
	public static int toInt(MapLocation loc) {
		return loc.x * height + loc.y;
	}
	
	public static int toInt(int x, int y) {
		return x * height + y;
	}
	
}
