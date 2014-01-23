package turtle;

import battlecode.common.*;

public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	static MapLocation[] potentialPastrLocNearMe; //Potential PASTR locations close to me.
	static double[][] allCowGrowths; // cow growths
	static Direction allDirections[] = Direction.values();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	
	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	static MapLocation[] pastrLocations = new MapLocation[3];
	
	/**
	 * @author kpeng94
	 * Overall strategy:
	 * The purpose of this robot is to be a defensive bot. It is conjectured that it will benefit from 
	 * rushing the opponent if the opponent is building a lot of PASTRs in the beginning of the game.
	 * 
	 * The HQ will first find good PASTR locations near our base, and start getting robots to crowd 
	 * cows near those points if they are not too far away from our base (square distance <= X). 
	 * We'll build a PASTR once we think we can defend it properly (army size > Y). 
	 * If we get an even more sizable advantage (army size > Y and our army size - their army size > Z), 
	 * we will build another PASTR.
	 * 
	 * Somewhat arbitrary values now
	 * X: 50
	 * Y: 10
	 * Z: 3
	 * 
	 * For defensive mechanics, we will crowd around the PASTR in the direction away from the HQ 
	 * (with the assumption that the HQ can relatively protect the other side.
	 * 
	 * @param rcin
	 */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		
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
		if (Clock.getRoundNum() < 3) {			
			if (rc.isActive()) {
				Direction spawnDir = dir[rand.nextAnd(7)];
				if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null) {
					rc.spawn(spawnDir);
				}
				
			}
			if (pastrLocations[0] == null) {
				pastrLocations[0] = getBestPastrLocNearMeBasedOnCowGrowthRate();
				rc.broadcast(0, pastrLocations[0].x * 100 + pastrLocations[0].y);
			}
		}
		double d = rc.senseCowsAtLocation(new MapLocation(8,11));
		System.out.println(d);
	}
	
	/**
	 * Gets best PASTR location near the robot that isn't already a PASTR location,
	 * based on cow growth.
	 *
	 * This method should only be called by the HQ.
	 * 
 	 * TODO: optimize
	 */
	private static MapLocation getBestPastrLocNearMeBasedOnCowGrowthRate() {
		if (Clock.getRoundNum() < 1) {
			allCowGrowths = rc.senseCowGrowth();			
		}
		MapLocation bestLocation = new MapLocation(curLoc.x, curLoc.y);
		double cowGrowthAmount = 0;
		MapLocation enemyHQLocation = rc.senseEnemyHQLocation();
		double distanceToEnemyHQ = curLoc.distanceSquaredTo(enemyHQLocation);
		for (int i = 15; i-- > 0;) {
			for (int j = 15; j-- > 0;) {
				// Check that it's in bounds
				if (curLoc.x - i + 8 >= 0 && curLoc.x - i + 8 < rc.getMapWidth() &&
					curLoc.y - j + 8 >= 0 && curLoc.y - j + 8 < rc.getMapHeight()) {
					MapLocation mapLoc = new MapLocation(curLoc.x - i + 8, curLoc.y - j + 8);
					double currentLocGrowth = allCowGrowths[curLoc.x - i + 8][curLoc.y - j + 8];

					// Check that it's behind a perpendicular line and not void
					// and choose the best point.
					if (mapLoc.distanceSquaredTo(enemyHQLocation) < distanceToEnemyHQ &&
						currentLocGrowth > cowGrowthAmount && rc.senseTerrainTile(mapLoc) != TerrainTile.VOID) {
						cowGrowthAmount = currentLocGrowth;
						bestLocation = mapLoc;
					}
				}
			}
		}
		return bestLocation;
	}
	
	private static double sumCowGrowthsInPotentialPASTRAroundPoint(MapLocation ml) {
		double sum = 0;
		
		// Checking the top and bottom rows
		for (int i = 3; i-- > 0;) {
			if (ml.x - i + 2 >= 0 && ml.x - i + 2 < rc.getMapWidth()) {
				// Top row
				if (ml.y - 2 >= 0) {
					sum += allCowGrowths[ml.x - i + 2][ml.y - 2];
				}
				if (ml.y + 2 <= rc.getMapHeight()) {
					sum += allCowGrowths[ml.x - i + 2][ml.y - 2];
				}
			}
		}
		
		// Checking the three horizontal rows
		for (int i = 5; i-- > 0;) {
			for (int j = 3; j-- > 0;) {
				if (ml.x - i + 3 >= 0 && ml.x - i + 3 < rc.getMapWidth() &&
					ml.y - j + 2 >= 0 && ml.y - j + 2 < rc.getMapHeight() && 
					rc.senseTerrainTile(new MapLocation(ml.x - i + 3, ml.y - j + 2)) != TerrainTile.VOID) {
					sum += allCowGrowths[ml.x - i + 3][ml.y - j + 2];
				}
			}
		}
		return sum;
	}

	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
//			int mapLoc = rc.readBroadcast(0);
			int mapLoc = 1010;
			if (curLoc.equals(new MapLocation(mapLoc / 100, mapLoc % 100))) {
//				rc.construct(RobotType.PASTR);
			} else {
				simpleMove(curLoc.directionTo(new MapLocation(mapLoc / 100, mapLoc % 100)));
			}
		}
	}

	private static void simpleMove(Direction chosenDirection) throws GameActionException{
		for(int directionalOffset:directionalLooks){
			int forwardInt = chosenDirection.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(rc.canMove(trialDir)){
				rc.move(trialDir);
				break;
			}
		}
	}
	
	/* Runs code for PASTRS */
	private static void runPastr() throws GameActionException {
		
	}

	/* Runs code for Noise Towers */
	private static void runNoise() throws GameActionException {
		
	}
	
}