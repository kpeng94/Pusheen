package navBotv3;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public static RobotController rc;
	public static int width;
	public static int height;
	public static MapLocation ourHQ;
	public static MapLocation enemyHQ;
	
	public static MapLocation[] buffer;
	public static MapLocation startBuffer;
	public static int bufferIndex;
	public static boolean tracing;
	public static boolean looped;
	public static MapLocation cur;
	
	public static boolean nearHQ; // Destination is near the enemy HQ
	public static boolean done; // Found a path to the destination
	public static MapLocation dest;
	public static int[][] mapInfo;
	
	/* Initializes navigation */
	public static void init(RobotController rcin) throws GameActionException {
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		
		ourHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		setDest(new MapLocation(width/2, height/2));
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination) throws GameActionException {
		destination = getNearestOpenDest(destination);
		if (dest == null || (dest.x != destination.x && dest.y != destination.y)) {
			dest = destination;
			mapInfo = new int[width][height];
			done = false;
			
			cur = rc.getLocation();
			tracing = false;
			
			if (dest.distanceSquaredTo(enemyHQ) <= 25) {
				nearHQ = true;
			}
		}
	}
	
	/* Returns nearest movable square */
	public static MapLocation getNearestOpenDest(MapLocation destination) throws GameActionException {
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
	
	/* Moves along a tangent bug path */
	public static void move() throws GameActionException {
		
	}
	
	/* Calculate for tangent bug path */
	public static void calculate(int bytelimit) throws GameActionException {
		if (done) {
			return;
		}
		while (Clock.getBytecodesLeft() > bytelimit) {
			System.out.println(cur);
			if (cur.x == dest.x && cur.y == dest.y) {
				done = true;
				return;
			}
			if (!tracing) {
				Direction toDest = cur.directionTo(dest);
				if (!checkTile(toDest, false)) {
					Direction rotDir = toDest;
					for (int i = 4; i-- > 0;) {
						rotDir = rotDir.rotateRight();
						if (checkTile(rotDir, true)) {
							break;
						}
					}
				}
			}
			else {
				if (cur.x == startBuffer.x && cur.y == startBuffer.y) {
					tracing = false;
					int minDist = startBuffer.distanceSquaredTo(dest);
					MapLocation minLoc = startBuffer;
					int end = bufferIndex;
					if (looped) {
						end = width + height;
					}
					
					for (int i = end; i-- > 0;) {
						int newDist = buffer[i].distanceSquaredTo(dest);
						if (newDist < minDist) {
							minDist = newDist;
							minLoc = buffer[i];
						}
					}
					cur = minLoc;
					continue;
				}
				Direction forward = dir[((mapInfo[cur.x][cur.y] / 10) % 10) - 1].opposite().rotateLeft().rotateLeft();
				for (int i = 6; i-- > 0;) {
					if (checkTile(forward, false)) {
						break;
					}
					forward = forward.rotateRight();
				}
			}
		}
	}
	
	/* Checks a single tile */
	private static boolean checkTile(Direction toNext, boolean startTrace) throws GameActionException {
		MapLocation next = cur.add(toNext);
		int tile = Map.getTile(next);
		if (tile != 3 && tile != 4) {
			int curInfo = mapInfo[cur.x][cur.y];
			int nextInfo = mapInfo[next.x][next.y];
			int dist = (curInfo / 100) +  1;
			mapInfo[cur.x][cur.y] = (curInfo / 10) * 10 + toNext.ordinal() + 1;
			if (nextInfo / 100 == 0 || nextInfo / 100 > dist) {
				mapInfo[next.x][next.y] = dist * 100 + (toNext.opposite().ordinal() + 1) * 10;
			}
			
			if (startTrace) {
				tracing = true;
				buffer = new MapLocation[width + height];
				bufferIndex = 0;
				startBuffer = cur;
				looped = false;
			}
			if (tracing) {
				buffer[bufferIndex++] = next;
				if (bufferIndex == width + height) {
					bufferIndex = 0;
					looped = true;
				}
			}
			cur = next;
			return true;
		}
		return false;
	}
}
