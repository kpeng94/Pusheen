package navBotv2;

import battlecode.common.*;

public class Navigationv2 {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static final int[] reversedAll = new int[] {4, 5, 3, 6, 2, 7, 1, 0};
	
	public static int[] intdirs;	
	public static RobotController rc;
	public static int width;
	public static int height;
	public static MapLocation ourHQ;
	public static MapLocation enemyHQ;
	public static int beamWidth;
	
	public static boolean nearHQ;
	
	public static boolean pathDone;
	public static MapLocation dest;
	public static int start;
	public static int[] mapInfo;
	
	public static int[] path;
	public static int curPath;
	
	public static int depth;
	public static int[] beamList; // Current beam list
	public static int[] nextBeam; // Next beam list
	public static int beamStart; // Has not checked yet
	public static int beamLength; // Length of current beam
	public static int beamEnd; // Total number of elements in the next beam
	
	/* Initializes navigation */
	public static void init(RobotController rcin) throws GameActionException {
		init(rcin, null);
	}
	
	public static void init(RobotController rcin, MapLocation destination) throws GameActionException {
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		ourHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();

		intdirs = new int[] {-1, height - 1, height, height + 1, 1, 1 - height, -height, -1 - height};
		beamWidth = 100;
		
		if (destination == null) {
			destination = new MapLocation(width/2, height/2);
		}
		setDest(destination);
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination) throws GameActionException {
		destination = getNearestOpenDest(destination);
		if (dest == null || (dest.x != destination.x && dest.y != destination.y)) {
			pathDone = false;
			beamList = new int[beamWidth];
			nextBeam = new int[beamWidth];
			
			beamStart = 0;
			beamLength = 1;
			beamEnd = 0;

			dest = destination;
			start = toInt(rc.getLocation());
			beamList[0] = start + 1;
			mapInfo = new int[width * height];
			if (dest.distanceSquaredTo(enemyHQ) <= 25) {
				nearHQ = true;
			}
		}
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
	
	/* Trivial movement */
	public static void trivialMove(MapLocation mapLoc, int[] looks) throws GameActionException {
		trivialMove(mapLoc, looks, false);
	}
	
	public static void trivialMove(MapLocation mapLoc, int[] looks, boolean tryDanger) throws GameActionException {
		MapLocation curLoc = rc.getLocation();
		int toDest = curLoc.directionTo(mapLoc).ordinal();
		for (int i = looks.length; i-- > 0;) {
			Direction moveDir = dir[(toDest + looks[i]) % 8];
			if (rc.canMove(moveDir) && (tryDanger || Map.getTile(curLoc.add(moveDir)) != 5)) {
				rc.move(moveDir);
				return;
			}
		}
	}
	
	/* Moves towards destination while calculating */
	public static void move() throws GameActionException {
		calculate(2500);
		moveNext();
	}
	
	private static void moveNext() throws GameActionException {
		if (pathDone) {
			if (rc.getLocation().distanceSquaredTo(fromInt(path[curPath] - 1)) <= 1) {
				curPath++;
			}
			while (path[curPath] == 0) {
				curPath++;
			}
			MapLocation next = fromInt(path[curPath] - 1);
			rc.setIndicatorString(1, "" + next);
			trivialMove(next, reversedAll);
		}
		else {
			
		}
	}
	
	/* Calculates more of the map using beam search */
	public static void calculate(int bytelimit) throws GameActionException {
		if (pathDone) {
			return;
		}
		while (Clock.getBytecodesLeft() > bytelimit && beamLength > 0) {
			int cur = beamList[beamStart] - 1;
			if (dest.x == cur / height && dest.y == cur % height) {
				backTrace();
				pathDone = true;
				return;
			}
			int depth = mapInfo[cur] / 8;
			int toDest = fromInt(cur).directionTo(dest).ordinal();
			for (int i = 8; i-- > 0;) {
				if (beamEnd == beamWidth) {
					break;
				}
				int nextDir = (toDest + reversedAll[i]) % 8;
				int tile = Map.getTile(new MapLocation(cur / height, cur % height).add(dir[nextDir]));
				if (tile == 3 || tile == 4) {
					continue;
				}
				int next = cur + intdirs[nextDir];
				if (next == start || (mapInfo[next] != 0 && mapInfo[next] / 8 <= depth + 1)) {
					continue;
				}
				if (mapInfo[next] == 0) {
					nextBeam[beamEnd++] = next + 1;
				}
				mapInfo[next] = (depth + 1) * 8 + ((nextDir + 4) % 8);
			}
			if (++beamStart == beamLength || beamEnd == beamWidth) {
				beamList = nextBeam.clone();
				beamLength = beamEnd;
				beamStart = 0;
				beamEnd = 0;
			}
		}
	}
	
	/* Backtraces to find path */
	private static void backTrace() throws GameActionException {
		int end = toInt(dest);
		curPath = 0;
		path = new int[mapInfo[end] / 8];
		while (end != start) {
			path[(mapInfo[end] / 8) - 1] = end + 1;
			end += intdirs[mapInfo[end] % 8];
		}
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
