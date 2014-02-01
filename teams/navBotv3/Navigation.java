package navBotv3;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static final int[] reversedAll = new int[] {4, 5, 3, 6, 2, 7, 1, 0};
	static final int[] reversedForward = new int[] {7, 1, 0};
	
	public static RobotController rc;
	public static int width;
	public static int height;
	public static MapLocation enemyHQ;
	
	public static int waitTime;	
	public static int startRound;
	public static boolean failure;
	
	public static void init(RobotController rcin) throws GameActionException {
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();

		enemyHQ = rc.senseEnemyHQLocation();
		TangentBug.rc = rc;
		SimpleNav.rc = rc;
		SimpleNav.setDest(new MapLocation(width/2, height/2), 1);
		
		waitTime = width + height;
	}
	
	public static void setDest(MapLocation dest) throws GameActionException {
		setDest(dest, 1);
	}
	
	public static void setDest(MapLocation dest, int rad) throws GameActionException {
		dest = getNearestOpenDest(dest);

		SimpleNav.setDest(dest, rad);
		startRound = Clock.getRoundNum();
	}
	
	public static void move() throws GameActionException {
		if ((Clock.getRoundNum() - startRound > waitTime) && !SimpleNav.pathDone) {
			TangentBug.setDest(SimpleNav.dest);
			failure = true;
		}
		if (failure) {
			TangentBug.move();
		}
		else {
			SimpleNav.move();
		}
	}
	
	/* Returns nearest movable square */
	public static MapLocation getNearestOpenDest(MapLocation destination) throws GameActionException {
		MapLocation newDest = destination;
		int tile = Map.getTile(newDest);
		int rad = 1;
		int offset = 7;
		while (tile > 2) {
			newDest = destination.add(dir[offset], rad);
			tile = Map.getTile(newDest);
			if (offset-- <= 0) {
				offset = 7;
				rad++;
			}
		}
		return newDest;
	}
	
	public static void calculate(int bytelimit) throws GameActionException {
		if ((Clock.getRoundNum() - startRound > waitTime) && !SimpleNav.pathDone) {
			TangentBug.setDest(SimpleNav.dest);
			failure = true;
		}
		if (failure) {
			TangentBug.calculate(bytelimit);
		}
		else {
			SimpleNav.calculate(bytelimit);
		}
	}
	
	/* Trivial movement */
	public static void trivialMove(MapLocation mapLoc, int[] looks) throws GameActionException {
		trivialMove(mapLoc, looks, false);
	}
	
	public static void trivialMove(MapLocation mapLoc, int[] looks, boolean tryDanger) throws GameActionException {
		MapLocation curLoc = rc.getLocation();
		int toDest = curLoc.directionTo(mapLoc).ordinal();
		if (toDest > 7) {
			return;
		}
		for (int i = looks.length; i-- > 0;) {
			Direction moveDir = dir[(toDest + looks[i]) % 8];
			if (rc.canMove(moveDir) && (tryDanger || Map.getTile(curLoc.add(moveDir)) != 5)) {
				rc.move(moveDir);
				return;
			}
		}
	}
	
}
