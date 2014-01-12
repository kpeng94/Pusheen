package cow_growth_farmer;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	static MapLocation[] potentialPastrLocNearMe; //Potential PASTR locations close to me.
	static double[][] allCowGrowths; // cow growths
	static Direction allDirections[] = Direction.values();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};	
	static boolean isAssignedPASTR = false;
	static final int NUMBER_OF_PASTURES_TO_BUILD = 5;
	static int width;
	static int height;

	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	static ArrayList<MapLocation> pastrLocations = new ArrayList<MapLocation>();
	static int pastrNumber;
	static int pastrLoc;
	
	/**
	 * @author kpeng94
	 * Overall strategy: Farm best locations based on the cow spawn rates.
	 * 
	 * @param rcin
	 */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		allCowGrowths = rc.senseCowGrowth();
		
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
		if (pastrLocations.size() < NUMBER_OF_PASTURES_TO_BUILD) {
			generateBestPastrLocNearMeBasedOnCowGrowthRate(10);
		}
		System.out.println(Clock.getRoundNum());
		if (rc.isActive()) {
			Direction spawnDir = dir[rand.nextAnd(7)];
			if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null) {
				rc.spawn(spawnDir);
			}
			int robotNum = rc.readBroadcast(0);
			if (robotNum < NUMBER_OF_PASTURES_TO_BUILD) {
				rc.broadcast(1, pastrLocations.get(robotNum).x * 100 + pastrLocations.get(robotNum).y);
			}
		}
		
//		if (Clock.getRoundNum() == 200) {
//			for(int i = 0; i < pastrLocations.size(); i++) {
//				System.out.println(pastrLocations.get(i).x + " " + pastrLocations.get(i).y);
//			}
//		}
	}
	
	/**
	 * Gets best PASTR location near the robot that isn't already a PASTR location,
	 * based on cow growth.
	 *
	 * This method should only be called by the HQ.
	 */
	private static void generateBestPastrLocNearMeBasedOnCowGrowthRate(int radius) {
		MapLocation bestLocation = new MapLocation(curLoc.x, curLoc.y);

		while (pastrLocations.size() < NUMBER_OF_PASTURES_TO_BUILD) {
			double cowGrowthAmount = 0;
			checkAllSquares:
			for (int i = radius * 2; i-- > 0;) {
				checkRows:
				for (int j = radius * 2; j-- > 0;) {
					// Check that it's in bounds
					MapLocation mapLoc = new MapLocation(curLoc.x - i + radius, curLoc.y - j + radius);
					TerrainTile tile = rc.senseTerrainTile(mapLoc);
					if (tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD) {
						for (int k = pastrLocations.size(); k-- > 0;) {
							if (pastrLocations.get(k).distanceSquaredTo(mapLoc) < 10) {
								continue checkRows;
							}
						}
						double currentLocGrowth = sumCowGrowthsInPotentialPASTRAroundPoint(mapLoc);
//						System.out.println(mapLoc.x + " " + mapLoc.y + " " + i + " " + j + " " + cowGrowthAmount);
						if (currentLocGrowth > cowGrowthAmount) {
							cowGrowthAmount = currentLocGrowth;
							bestLocation = mapLoc;
						}
					}
				}
			}
			pastrLocations.add(bestLocation);
		}
	}

	/**
	 * Precondition: allCowGrowths is a double[][] that has all the cow growths on the map
	 * @param ml center point
	 * @return sum of cow growths that would be contained in a PASTR built on this point
	 */
	private static double sumCowGrowthsInPotentialPASTRAroundPoint(MapLocation ml) {
		double sum = 0;	
		// Checking the top and bottom rows
		for (int i = 3; i-- > 0;) {
			if (ml.x - i + 1 >= 0 && ml.x - i + 1 < rc.getMapWidth()) {
				// Top row
				if (ml.y - 2 >= 0) {
					sum += allCowGrowths[ml.x - i + 1][ml.y - 2];
				}
				if (ml.y + 2 < rc.getMapHeight()) {
					sum += allCowGrowths[ml.x - i + 1][ml.y + 2];
				}
			}
		}		
		// Checking the three horizontal rows
		for (int i = 5; i-- > 0;) {
			for (int j = 3; j-- > 0;) {
				TerrainTile tile = rc.senseTerrainTile(new MapLocation(ml.x - i + 2, ml.y - j + 1));
				if (tile == TerrainTile.NORMAL || tile == TerrainTile.ROAD) {
					sum += allCowGrowths[ml.x - i + 2][ml.y - j + 1];
				}
			}
		}
		return sum;
	}

	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			if (!isAssignedPASTR) {
				pastrNumber = rc.readBroadcast(0);
				pastrLoc = rc.readBroadcast(1);
				rc.broadcast(0, pastrNumber + 1);
				isAssignedPASTR = true;
			} 			
			if (curLoc.equals(new MapLocation(pastrLoc / 100, pastrLoc % 100))) {
				rc.construct(RobotType.PASTR);
			} else {
				simpleMove(curLoc.directionTo(new MapLocation(pastrLoc / 100, pastrLoc % 100)));
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