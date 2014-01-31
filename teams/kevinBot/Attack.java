package kevinBot;

import java.util.Map;

import battlecode.common.*;

public class Attack {
	private static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, 
												   Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static Robot[] closeEnemies;
	private static Robot[] detectableEnemies;
	private static RobotController rc;
	private static MapLocation myHQLocation;
	private static MapLocation enemyHQLocation;
	private static int squadronNumber = 0;
	private static boolean surrounding = false;
	private static MapLocation mySurroundDestination;
	private static MapLocation myLocation;
	private static MapLocation newLocation;
	private static int defaultOff = 14;
	private static int id;
	private static int offTarget = 14;
	private static boolean retreatMode = false;
	private static int numberOfUnitsWeHave;
	private static int numberOfUnitsTheyHave;
	private static double healthLastRound = 100;
	private static double healthThisRound = 100;
	
	/**
	 * INITIALIZATION
	 */
	
	/**
	 * Initialize the robot controller so that these methods can be used.
	 * @param rcin
	 */
	public static void init(RobotController rcin, int robotID) {
		rc = rcin;
		id = robotID;
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
	}
	
	
	/**
	 * STRATEGY (v1):
	 * 
	 * Don't fight unless you have a clear advantage. Having an advantage concretely means:
	 * 	1. You have a numbers advantage.
	 *  2. You have a positioning advantage. In a one-on-one combat, this means that you have checked 
	 *  	that the enemy is the one moving into the fight, and not you.
	 * 
	 * Self-destruct mechanic:
	 *  1. In combat, broadcast who will be doing the suicide.
	 * 
	 * 
	 * Healing calculations:
	 * 0 - 30 turns: heal nothing,
	 * 130 turns: heal 50 health.
	 * 
	 */
	
	/**
	 * Need to consider when there are walls in the way, etc.
	 * Probably should rewrite to use broadcast system.
	 * Our bots will surround the enemy HQ.
	 *
	 * This should only set the surroundDestination appropriately, 
	 * but should not change anything else or take any other action.
	 *  
	 * @throws GameActionException 
	 */
	public static boolean surround(boolean clockwise) throws GameActionException {
		int off = defaultOff;
		myLocation = rc.getLocation();
		if (mySurroundDestination == null) {
			int mlin2 = rc.readBroadcast(26014);
			mySurroundDestination = new MapLocation(mlin2 / 100, mlin2 % 100);			
		}
		if (myLocation.x != mySurroundDestination.x || myLocation.y != mySurroundDestination.y) {
			int distanceToDest = myLocation.distanceSquaredTo(mySurroundDestination);
			if (!surrounding && distanceToDest > 36) {
				Navigation.setDest(mySurroundDestination, 25);
				surrounding = true;
			} 
			if (isOccupied(off)) {
				rc.setIndicatorString(2, "2: " + rc.readBroadcast(26014) + " huh " + rc.readBroadcast(26013) + " next: " + rc.readBroadcast(26012));
				while (off > 0 && isOccupied(off)) {
					off--;
					int mlint = rc.readBroadcast(26000 + off);
					mySurroundDestination = new MapLocation(mlint / 100, mlint % 100);
				}					
				rc.setIndicatorString(0, "0: " + mySurroundDestination + " what the L: " + off + " wow: " + isOccupied(off));
				Navigation.setDest(mySurroundDestination);
				offTarget = off;
			}
			return false;
		}
		rc.setIndicatorString(1, "1: I am at the point now.");
		rc.broadcast(27000 + offTarget, id);
		return true;
	}
	
	private static boolean isOccupied(int locNum) throws GameActionException {
		return rc.readBroadcast(27000 + locNum) != 0;
	}
	
	public static void attackMicro() throws GameActionException {
		storeEnemiesWithinRange();
		// TODO: Make HQ actually produce this 
		numberOfUnitsWeHave = rc.readBroadcast(37000);
		numberOfUnitsTheyHave = rc.readBroadcast(37001);
		healthLastRound = healthThisRound;
		healthThisRound = rc.getHealth();

		// Let teammates know you are being hurt
		if (healthThisRound < healthLastRound) {
			// Let teammates know that you are being attacked by a bot
			rc.broadcast(39600 + id, 1);
			// We know we've been suicided on because the difference in health is not 0
			// TODO: We should account for some error here 
			if ((healthLastRound - healthThisRound) % 10 != 0) {
				rc.broadcast(39500 + id, 1);
			}
		}
		
		// Predict what your teammates are going to do and calculate and move as a result (if we can)
		if (healthThisRound <= 20) {
			goHeal();
		}
	}

	
	public static void goHeal() {
		Navigation.setDest(myHQLocation);
	}
	
	/**
	 * Considerations: running towards points of safety, but also points where we can herd our cows.
	 * Distracting the enemy from a PASTR.
	 * 
	 * Running alone or running as a group?
	 * Telling the other robots where you will hypothetically go.
	 * 
	 * @param considerWalls If we should take into account walls or not
	 */
	public static boolean retreat(boolean considerWalls, boolean groupEscape) throws GameActionException {
		myLocation = rc.getLocation();
		Robot[] nearby = rc.senseNearbyGameObjects(Robot.class, 20, rc.getTeam().opponent());
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		if (nearby.length >= 1) {
			retreatMode = true;
			// TODO: Change this later to calculate
			MapLocation closestEnemyLoc = rc.senseRobotInfo(nearbyRobots[0]).location; 
			Direction enemyDir = rc.getLocation().directionTo(closestEnemyLoc);
			if (retreatMode == true) {
				Direction oppositeDir = directions[(enemyDir.ordinal() + 6) % 8];
				int maxDistance = rc.getLocation().distanceSquaredTo(closestEnemyLoc);
				for (int i = 4; i-- > 0;) {
					Direction dirAdd = directions[(enemyDir.ordinal() + i + 2) % 8];
					int newDistance = rc.getLocation().add(dirAdd).distanceSquaredTo(closestEnemyLoc);
					if (newDistance > maxDistance) {
						oppositeDir = directions[(enemyDir.ordinal() + i + 2) % 8];
						maxDistance = newDistance;
					}
				}
				Direction toMyHQ = myLocation.directionTo(myHQLocation);
				int hqCloseness = myLocation.distanceSquaredTo(myHQLocation);
				rc.setIndicatorString(1, "HQ ordinal: " + toMyHQ.ordinal() + " ord ordinal: " + oppositeDir.ordinal());
				rc.setIndicatorString(2, "retreat mode  " + Clock.getRoundNum());
				takeStep(oppositeDir, toMyHQ, hqCloseness, 100);
				return true;
			}
			
		}
		return false;
	}

	private void decideFightingMechanic() {
		if (numberOfUnitsWeHave - numberOfUnitsTheyHave >= 5 && numberOfUnitsWeHave >= numberOfUnitsTheyHave * 2) {
			attackAndRetreat();
		} else if (numberOfUnitsWeHave - numberOfUnitsTheyHave <= -2) {
			justRetreat();
		} else {
			suicideAttackAndRetreat();
		}
	}
	
	private void attackAndRetreat() {
		
	}
	
	private void justRetreat() {
		
	}
	
	private void suicideAttackAndRetreat() {
		
	}
	
	private void suicide() {
		double myHealth = rc.getHealth();
//		Navigation.setDest(destination);
	}
	
	/**
	 * 
	 * @return
	 */
	private MapLocation calculateSuicideLocation() {
		return null;
	}
	
	private boolean shouldSuicide() {
		if (closeEnemies.length <= 1) {
			return false;
		}
//		} else if (myUnitsNumber >= theirUnitsNumber) {
//			return true;
//		}
//		double myHealth = rc.getHealth();
		return false;
	}
	
	/**
	 * Detects if the enemy is trying to use a suicide mechanic.
	 * @return true if there is a really close enemy (5 squared units away).
	 */
	private boolean detectSuicide() throws GameActionException {
		Robot[] closerEnemies = rc.senseNearbyGameObjects(Robot.class, 5, rc.getTeam().opponent());
		int closerEnemiesNum = closerEnemies.length;
		if (closeEnemies.length >= 1 && closerEnemiesNum >= 1) {
			rc.broadcast(39700 + id, closerEnemiesNum);
			return true;
		}
		return false;
	}
	
	/**
	 * Helper function for checking whether a potential suicide is imminent
	 * Should be done every turn to update the nearby enemies that can attack us.
	 * Bytecode cost: 100
	 */
	private static void storeEnemiesWithinRange() {
		closeEnemies = rc.senseNearbyGameObjects(Robot.class, 10, rc.getTeam().opponent());
		detectableEnemies = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
	}
	
	private int decideAction() {
		if (shouldSuicide()) {
			suicide();
		}
		return 0;
	}
	
	/**
	 * TODO: take into account move, action delay
	 * @param enemyLocations
	 * @param targetLocation
	 * @return
	 */
	private static boolean inEnemyRange(MapLocation[] enemyLocations, MapLocation targetLocation) {
		for (int i = enemyLocations.length; i-- > 0;) {
			if(targetLocation.distanceSquaredTo(enemyLocations[i]) <= 10) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Will help the robot choose who to attack.
	 */
	private static void chooseTargetToAttack() {
		
	}
	
	/**
	 * Takes a step in the direction 
	 *
	 */
	private static boolean takeStep(Direction dir) throws GameActionException {
		if (rc.isActive() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
	
	/**
	 * Takes a step in a weighted direction
	 * @param d1 direction 1
	 * @param d2 direction 2
	 * @param w1 weight of d1
	 * @param w2 weight of d2
	 * @throws GameActionException
	 */
	private static boolean takeStep(Direction d1, Direction d2, int w1, int w2) throws GameActionException {
		Direction dir = directions[((d1.ordinal() * w1 + d2.ordinal() * w2) / (w1 + w2) + 8) % 8];
		if (rc.isActive()) {
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			} 
			Direction dir2 = directions[(dir.ordinal() + 1) % 8];
			if (rc.canMove(dir2)) {
				rc.move(dir2);
				return true;
			} 
			Direction dir3 = directions[(dir.ordinal() + 7) % 8];
			if (rc.canMove(dir3)) {
				rc.move(dir3);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Takes a step in a weighted direction
	 * @param d1 direction 1
	 * @param d2 direction 2
	 * @param d3 direction 3
	 * @param w1 weight of d1
	 * @param w2 weight of d2
	 * @param w3 weight of d3
	 * @throws GameActionException
	 */
	private static boolean takeStep(Direction d1, Direction d2, Direction d3, int w1, int w2, int w3) throws GameActionException {
		Direction dir = directions[(d1.ordinal() * w1 + d2.ordinal() * w2 + d3.ordinal() * w3) / (w1 + w2 + w3)];
		if (rc.isActive()) {
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			} 
			Direction dir2 = directions[(dir.ordinal() + 1) % 8];
			if (rc.canMove(dir2)) {
				rc.move(dir2);
				return true;
			} 
			Direction dir3 = directions[(dir.ordinal() + 7) % 8];
			if (rc.canMove(dir3)) {
				rc.move(dir3);
				return true;
			}
		}
		return false;
	}
	
	private static void takeStep(MapLocation ml1, MapLocation ml2, int w1, int w2) {
		int avgX = (w1 * (myLocation.x - ml1.x) + w2 * (myLocation.y - ml1.y)) / (w1 + w2);
		int avgY = (w1 * (myLocation.y - ml1.y) * w2 * (myLocation.y - ml2.y)) / (w1 + w2);
		Math.atan2(avgY, avgX);
		if (avgY >= 0) {
			if (avgY >= 2.4 * avgX) {
//				if (rc.canMove(Direction))
			}
		}
		if (avgY > 0 && avgX > 0) {
			
		}
	}
	
	/**
	 * Aim for points with lots of cows in the enemy PASTR before aiming for the PASTR.
	 * 
	 * Consider: communication for micro in terms of which squares are getting attacked
	 */
	private static void aimForCows() {
//		if (closestEnemyPASTR)
		
	}
}
