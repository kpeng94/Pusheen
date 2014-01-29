package mapBot;

import battlecode.common.*;

public class Map {
	public static int mapChannels;
	
	public static RobotController rc;
	public static int width;
	public static int height; 
	
	public static int curCheck;
	public static int curX;
	public static boolean mapDone;

	/* Initializes the map */
	public static void init(RobotController rcin) {
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		
		mapChannels = GameConstants.BROADCAST_MAX_CHANNELS - width * height + 1;
	}
	
	/* Initializes main locations with the HQ */
	public static void HQinit() throws GameActionException {
		MapLocation ourHQ = rc.senseHQLocation();
		MapLocation enemyHQ = rc.senseEnemyHQLocation();

		MapLocation[] nearEnemyHQ = MapLocation.getAllMapLocationsWithinRadiusSq(enemyHQ, 25);
		for (int i = nearEnemyHQ.length; i-- > 0;) {
			MapLocation danger = nearEnemyHQ[i];
			if (getTile(danger) < 3) {
				rc.broadcast(mapChannels + danger.x * height + danger.y, 5);
			}
		}
		rc.broadcast(mapChannels + ourHQ.x * height + ourHQ.y, 3);
		rc.broadcast(mapChannels + enemyHQ.x * height + enemyHQ.y, 3);
	}
	
	/**
	 * Gets the terrain tile represented as an integer:
	 * 0 : Unknown (this should never be returned from this function)
	 * 1 : Normal
	 * 2 : Road
	 * 3 : Void
	 * 4 : Off Map
	 * 5 : Danger (near enemy HQ)
	 */
	public static int getTile(MapLocation loc) throws GameActionException {
		if (loc.x < 0 || loc.y < 0 || loc.x >= width || loc.y >= height) {
			return 4;
		}
		int channel = mapChannels + loc.x * height + loc.y;
		int tile = rc.readBroadcast(channel);
		if (tile > 0) {
			return tile;
		}
		tile = rc.senseTerrainTile(loc).ordinal() + 1;
		rc.broadcast(channel, tile);
		return tile;
	}
	
	// Duplicated to save bytecodes
	public static int getTile(int x, int y) throws GameActionException {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return 4;
		}
		int channel = mapChannels + x * height + y;
		int tile = rc.readBroadcast(channel);
		if (tile > 0) {
			return tile;
		}
		tile = rc.senseTerrainTile(new MapLocation(x, y)).ordinal() + 1;
		rc.broadcast(channel, tile);
		return tile;
	}
	
	/* Continues calculating the map */
	public static void calculate(int bytelimit) throws GameActionException {
		if (mapDone) {
			return;
		}
		if (curCheck == 0) {
			curCheck = 1;
			curCheck = getNextCheck();
		}
		while (Clock.getBytecodesLeft() > bytelimit) {
			if (curX-- <= 0) {
				rc.broadcast(mapChannels - curCheck, 5000);
				curCheck = getNextCheck();
				if (mapDone) {
					return;
				}
			}
			int channel = mapChannels + curX * height + curCheck - 1;
			int tile = rc.readBroadcast(channel);
			if (tile == 0) {
				rc.broadcast(channel, rc.senseTerrainTile(new MapLocation(curX, curCheck - 1)).ordinal() + 1);
			}
		}
	}
	
	private static int getNextCheck() throws GameActionException {
		int curRound = Clock.getRoundNum() + 1;
		int start = curCheck;
		do {
			if (--curCheck <= 0) {
				curCheck = height;
			}
			int readRound = rc.readBroadcast(mapChannels - curCheck);
			if (readRound == 0 || curRound - readRound > 10) {
				rc.broadcast(mapChannels - curCheck, curRound);
				curX = width - 1;
				return curCheck;
			}
		} while (curCheck != (start % height) + 1);
		mapDone = true;
		return curCheck;
	}
	
}
