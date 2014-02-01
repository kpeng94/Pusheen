package seedingTourneyBotv4;

import battlecode.common.*;

public class SimpleNav {
	static final int checkDist = 3;
	
	public static RobotController rc;

	public static boolean nearHQ; // Destination is near the enemy HQ

	public static boolean pathDone; // Path generation complete
	public static MapLocation dest; // Destination location
	public static int radius; // Radius to swarm around destination
	public static int[][] mapInfo; // Explored map info
	
	public static MapLocation[] path;
	public static int curPathPos;
	
	public static MapLocation curCheck;
	public static int checkNum;
	public static boolean isRoad;
	
	/* Initializes navigation to know the width and height */
	public static void init(RobotController rcin) throws GameActionException {
		rc = rcin;
	}
	
	/* Sets the destination */
	public static void setDest(MapLocation destination, int rad) throws GameActionException {
		if (dest == null || (dest.x != destination.x && dest.y != destination.y)) {
			pathDone = false;
			dest = destination;
			mapInfo = new int[Navigation.width][Navigation.height];
			curCheck = rc.getLocation();
			checkNum = 0;
			isRoad = Map.getTile(curCheck) == 2;
			if (dest.distanceSquaredTo(Navigation.enemyHQ) <= 25) {
				nearHQ = true;
			}
		}
		radius = rad;
	}
	
	/* Simple movement */
	public static void move() throws GameActionException {
		if (!pathDone) {
			calculate(5000);
			backTrace(rc.getLocation());
		}
		else if (curPathPos >= 0) {
			pathMove();
		}
		else {
			Navigation.trivialMove(dest, Navigation.reversedForward);
		}
	}
	
	/* Calculates paths during idle time */
	public static void calculate(int bytelimit) throws GameActionException {
		while (Clock.getBytecodesLeft() > bytelimit) {
			if (curCheck.distanceSquaredTo(dest) <= radius) {
				return;
			}
			int toDest = curCheck.directionTo(dest).ordinal();
			
			int minRound = Integer.MAX_VALUE;
			MapLocation minNext = null;
			boolean minRoad = false;
			
			for (int i = 8; i-- > 0;) {
				MapLocation next = curCheck.add(Navigation.dir[(toDest + Navigation.reversedAll[i]) % 8]);
				if (next.x < 0 || next.y < 0 || next.x >= Navigation.width || next.y >= Navigation.height) {
					continue;
				}
				int roundNum = (mapInfo[next.x][next.y] % 90000) / 9;
				int tile = Map.getTile(next);
				
				if (nearHQ && (tile == 3 || tile == 4)) {
					continue;
				}
				else if (!nearHQ && tile > 2) {
					continue;
				}
				
				if (roundNum == 0) {
					updateAdjacent(curCheck, isRoad ? 1 : 2);
					curCheck = next;
					isRoad = tile == 2;
					mapInfo[curCheck.x][curCheck.y] += checkNum * 9;
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
				updateAdjacent(curCheck, isRoad ? 1 : 2);
				curCheck = minNext;
				isRoad = minRoad;
				mapInfo[curCheck.x][curCheck.y] += checkNum * 9;
				checkNum++;
			}
		}
	}
	
	private static void updateAdjacent(MapLocation loc, int amount) {
		int dist = (mapInfo[loc.x][loc.y] / 90000) + amount;
		for (int i = 8; i-- > 0;) {
			MapLocation next = loc.add(Navigation.dir[i]);
			if (next.x < 0 || next.y < 0 || next.x >= Navigation.width || next.y >= Navigation.height) {
				continue;
			}
			int oldDist = mapInfo[next.x][next.y] / 90000;
			if (oldDist == 0 || oldDist > dist) {
				mapInfo[next.x][next.y] = (dist * 90000) + ((i + 4) % 8) + 1;
			}
		}
	}
	
	private static void backTrace(MapLocation start) throws GameActionException {
		if (curCheck.distanceSquaredTo(dest) <= radius) {
			pathDone = true;
		}
		MapLocation end = curCheck;
		curPathPos = 0;
		path = new MapLocation[Navigation.width + Navigation.height];
		path[curPathPos] = end;
		
		while (start.distanceSquaredTo(end) > checkDist && curPathPos + 1 < path.length) {
			end = end.add(Navigation.dir[(mapInfo[end.x][end.y] % 9) - 1]);
			if (start.distanceSquaredTo(end) > checkDist) {
				end = end.add(Navigation.dir[(mapInfo[end.x][end.y] % 9) - 1]);
			}
			curPathPos++;
			path[curPathPos] = end;
		}
		
		pathMove();

	}
	
	private static void pathMove() throws GameActionException {
		MapLocation pos = path[curPathPos];
		if (rc.getLocation().distanceSquaredTo(pos) < checkDist + 1) {
			curPathPos--;
		}
		Navigation.trivialMove(pos, Navigation.reversedAll);
	}
	
}
