package testplayer;

import battlecode.common.*;

public class RobotPlayer {
static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	static int width; // Map Width
	static int height; // Map Height
	static double[][] cowRates; // Spawn rate of cows
	static TerrainTile[][] map; // Terrain information of map (this will be built by the HQ)
	
	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	
	static boolean mapComplete; // Whether HQ is done scanning map
	static int curCheck; // Current sensed location
	
	static int temp;
	
	/* Main Method */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		cowRates = rc.senseCowGrowth();
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		map = new TerrainTile[((width/4) + 1) * 4][((height/4) + 1) * 4];
		
		while (true) {
			/* Save dynamic values */
			curLoc = rc.getLocation();
			
			/* Distributes actions to their respective functions */
			switch(rc.getType()) {
			case HQ:
				try {
					runHQ();
				}
				catch (Exception e) {
//					e.printStackTrace();
					System.out.println("HQ Exception");
				}
				break;
			case SOLDIER:
				try {
					runSoldier();
				}
				catch (Exception e) {
					System.out.println("Soldier Exception");
				}
				break;
			case PASTR:
				try {
					runPastr();
				}
				catch (Exception e) {
					System.out.println("Pastr Exception");
				}
				break;
			case NOISETOWER:
				try {
					runNoise();
				}
				catch (Exception e) {
					System.out.println("NoiseTower Exception");
				}
				break;
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	/* Runs code for HQ */
	private static void runHQ() throws GameActionException {
		// Spawn in a random direction if its able to spawn
//		if (rc.isActive() && rc.senseRobotCount() < 25) {
//			Direction spawnDir = dir[rand.nextAnd(0b111)];
//			if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null)
//				rc.spawn(spawnDir);
//		}
		if (rc.isActive()) {
			if (rc.senseRobotCount() == 0) {
				rc.spawn(rc.getTeam() == Team.A ? Direction.EAST : Direction.WEST);
			}
		}
		// Otherwise, spend all most remaining bytecodes left on calculations
		calculateHQ();
	}

	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			if (rc.getTeam() == Team.B) {
				rc.construct(RobotType.PASTR);
			}
			else {
				if (curLoc.x == 23) {
					Direction x = Direction.EAST;
					Direction y = Direction.NORTH;
					if (rand.nextAnd(0b1) == 1) {
						rc.attackSquare(curLoc.add(x,2).add(y,2));
					}
					else {
						rc.attackSquare(curLoc.add(x,2).add(y,-2));
					}
				}
				else {
					rc.move(Direction.EAST);
				}
			}
//			if (curLoc.x == 14) {
//				rc.construct(RobotType.NOISETOWER);
//			}
//			else {
//				rc.move(Direction.EAST);
//			}

//			Direction moveDir = dir[rand.nextAnd(0b111)];
//			if (rc.canMove(moveDir))
//				rc.move(moveDir);
		}
	}
	
	/* Runs code for PASTRS */
	private static void runPastr() throws GameActionException {
		
	}

	/* Runs code for Noise Towers */
	private static void runNoise() throws GameActionException {
		if (rc.getActionDelay() > 0) {
			System.out.println(rc.getActionDelay());
		}
		if (rc.isActive()) {
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam().opponent());
			if (pastrs.length == 0)
				return;
			int start = temp;
			while (!rc.canAttackSquare(pastrs[temp])) {
				temp = (temp + 1) % pastrs.length;
				if (start == temp)
					return;
			}
			rc.attackSquare(pastrs[temp]);
			temp = (temp + 1) % pastrs.length;
		}
	}
	
	/* Does calculations using the HQ */
	private static void calculateHQ() throws GameActionException {
		if (!mapComplete) {
			while(Clock.getBytecodesLeft() > 1000) {
				int xBase = 4 * (curCheck % (width/4 + 1));
				int yBase = 4 * (curCheck / (height/4 + 1));
				int data = 0;
				for (int i = 16; i-- > 0;) {
					int x = xBase + (i % 4);
					int y = yBase + (i / 4);
					map[x][y] = rc.senseTerrainTile(new MapLocation(x, y));
					data = (data << 2) & tileToInt(map[x][y]);
				}
				rc.broadcast(1000 + curCheck, data);
				curCheck += 1;
				System.out.println(Clock.getBytecodesLeft());
				if (curCheck >= (width/4 + 1) * (height/4 + 1)) {
					mapComplete = true;
					break;
				}
			}
			System.out.println("BROKE OUT BRO");
		}
	}

	private static int tileToInt(TerrainTile terrainTile) {
		switch(terrainTile) {
		case NORMAL:
			return 0;
		case ROAD:
			return 1;
		case VOID:
			return 2;
		case OFF_MAP:
			return 3;
		default:
			return 0;
		}
	}
	
}