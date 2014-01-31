package kevinBot;

import java.util.Map;

import battlecode.common.*;

public class Attack {
	private static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, 
												   Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static Robot[] reallyCloseEnemies; // Within 5 squared distance
	private static Robot[] closeEnemies; // Within 10 squared distance
	private static Robot[] detectableEnemies; // Within 35 squared distance
	private static MapLocation[] detectableEnemyLocations;
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
	private static boolean beingAttacked = false;
	private static boolean beingSDed = false;
	private static int goal = 0;
	private static int numberOfNTs = 0;
	private static int numberOfPASTRs = 0;
	private static boolean healing = false;
	
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
				while (off > 0 && isOccupied(off)) {
					off--;
					int mlint = rc.readBroadcast(26000 + off);
					mySurroundDestination = new MapLocation(mlint / 100, mlint % 100);
				}					
				Navigation.setDest(mySurroundDestination);
				offTarget = off;
			}
			return false;
		}
		rc.broadcast(27000 + offTarget, id);
		return true;
	}
	
	private static boolean isOccupied(int locNum) throws GameActionException {
		return rc.readBroadcast(27000 + locNum) != 0;
	}
	
	/**
	 * Method for the attack micro. Flow:
	 * 1. We gather information about all enemies in range.
	 * 2. We gather information about our unit force compared to their unit force.
	 * 3. Consider: Need to take down their PASTR, or just fighting, or defending.
	 * 4. If just fighting or defending, we want to find defensively.
	 * 5. 
	 * @throws GameActionException
	 */
	public static void attackMicro() throws GameActionException {
		storeEnemiesWithinRange();
		// TODO: Make HQ actually produce this 
		myLocation = rc.getLocation();
		squadronNumber = rc.readBroadcast(39300 + id);
		numberOfUnitsWeHave = rc.readBroadcast(35020 + squadronNumber);
		numberOfUnitsTheyHave = rc.readBroadcast(37001);
		healthLastRound = healthThisRound;
		healthThisRound = rc.getHealth();
		numberOfPASTRs = rc.readBroadcast(31000);
		numberOfNTs = rc.readBroadcast(31001);
//		Add squadronLeader boolean (true / false)
		goal = rc.readBroadcast(39400 + id);
		// Let teammates know you are being hurt
		if (healthThisRound < healthLastRound) {
			// Let teammates know that you are being attacked by a bot
			rc.broadcast(39600 + id, 1);
			beingAttacked = true;
			// We know we've been suicided on because the difference in health is not 0
			// TODO: We should account for some error here 
			if ((healthLastRound - healthThisRound) % 10 != 0) {
				rc.broadcast(39500 + id, 1);
				beingSDed = true;
			}
		}
		
		if (goal == 1) {
			// mostly attack and retreat micro here
			decideFightingMechanic();
		} else if (goal == 2) {
			// mostly attack, no retreat
		} else if (goal == 3) {
			
		}
		
		// Predict what your teammates are going to do and calculate and move as a result (if we can)

		if (healthThisRound <= 20) {
			goHeal();
			healing = true;
		}
	}

	/**
	 * Computes the direction that I would most likely move in next move
	 * @return
	 */
	public static Direction highPDir() {
		return Direction.NONE;
	}
	
	public static 
	
	public static MapLocation findClosestEnemyLoc(Robot[] eInRange, boolean detect) throws GameActionException {
		MapLocation closestML = null;
		int distance = 100000;
		
		if (detect) {
			detectableEnemyLocations = new MapLocation[eInRange.length];
			for (int i = eInRange.length; i-- > 0;) {
				RobotInfo ri = rc.senseRobotInfo(eInRange[i]);
				MapLocation newLoc = ri.location;
				detectableEnemyLocations[i] = newLoc;
				int nd = myLocation.distanceSquaredTo(newLoc);
				if (nd < distance) {
					closestML = newLoc;
					distance = nd;
				}
			}
		} else {
			for (int i = eInRange.length; i-- > 0;) {
				RobotInfo ri = rc.senseRobotInfo(eInRange[i]);
				MapLocation newLoc = ri.location;
				int nd = myLocation.distanceSquaredTo(newLoc);
				if (nd < distance) {
					closestML = newLoc;
					distance = nd;
				}
			}			
		}
		return closestML;
	}

	/**
	 * Gets map location of bot with lowest HP
	 * @param nearbyEnemies
	 * @return
	 * @throws GameActionException
	 */
	public static MapLocation findLowestHP(Robot[] nearbyEnemies) throws GameActionException {
		double health = 1000;
		MapLocation lowestHealthTargetLocation = rc.senseEnemyHQLocation();
		for (int i = nearbyEnemies.length; i-- > 0;) {
			RobotInfo info = rc.senseRobotInfo(nearbyEnemies[i]);
			if (info.type != RobotType.HQ && rc.canAttackSquare(info.location) && info.health < health) {
				health = info.health;
				lowestHealthTargetLocation = info.location;				
			}
		}
		return lowestHealthTargetLocation;
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
	 * @param groupEscape
	 * @param enemyDir
	 * @param ml Assumed to be the closest enemy (TODO: tweak for multiple closest later)
	 * @return
	 * @throws GameActionException
	 */
	public static boolean retreat(boolean groupEscape, 
								  Direction enemyDir, MapLocation ml) throws GameActionException {
		Direction oppositeDir = directions[(enemyDir.ordinal() + 6) % 8];
		int maxDistance = rc.getLocation().distanceSquaredTo(ml);
		for (int i = 4; i-- > 0;) {
			Direction dirAdd = directions[(enemyDir.ordinal() + i + 2) % 8];
			int newDistance = rc.getLocation().add(dirAdd).distanceSquaredTo(ml);
			if (newDistance > maxDistance) {
				oppositeDir = directions[(enemyDir.ordinal() + i + 2) % 8];
				maxDistance = newDistance;
			}
		}
		Direction toMyHQ = myLocation.directionTo(myHQLocation);
		int hqCloseness = myLocation.distanceSquaredTo(myHQLocation);
		takeStep(oppositeDir, toMyHQ, hqCloseness, 100);
		return true;			
	}

	/**
	 * For attacking mode
	 */
	private static void decideFightingMechanic() throws GameActionException {
		if (numberOfUnitsWeHave - numberOfUnitsTheyHave >= 5 && numberOfUnitsWeHave >= numberOfUnitsTheyHave * 2) {
			attackAndRetreat();
		} else if (numberOfUnitsWeHave - numberOfUnitsTheyHave <= -2) {
			justRetreat();
		} else {
			suicideAttackAndRetreat();
		}
	}
	
	/**
	 * Attack and run away as necessary.
	 * @throws GameActionException
	 */
	private static void attackAndRetreat() throws GameActionException {
		if (reallyCloseEnemies.length >= 1) {
			MapLocation ml = findClosestEnemyLoc(reallyCloseEnemies, false);
			Direction dte = myLocation.directionTo(ml);
			retreat(false, dte, ml);
		} else if (closeEnemies.length >= 1) {
			MapLocation ml = findLowestHP(closeEnemies);
			if (rc.isActive() && rc.canAttackSquare(ml)) {
				rc.attackSquare(ml);
			}
		} else {
			MapLocation ml = findClosestEnemyLoc(detectableEnemies, true);
			Direction dir = myLocation.directionTo(ml);
			if (!inEnemyRange(detectableEnemyLocations, myLocation.add(dir))) {
				takeStep(dir);
			}
		}
	}
	
	private static void justRetreat() throws GameActionException {
		if (reallyCloseEnemies.length >= 1) {
			MapLocation ml = findClosestEnemyLoc(reallyCloseEnemies, false);
			Direction dte = myLocation.directionTo(ml);
			retreat(false, dte, ml);
		} else if (closeEnemies.length >= 1) {
			MapLocation ml = findClosestEnemyLoc(closeEnemies, false);
			Direction dte = myLocation.directionTo(ml);
			retreat(false, dte, ml);
		}
	}
	
	private static void suicideAttackAndRetreat() throws GameActionException {
		if (shouldSuicide()) {
			if (canSuicide()) {
				rc.selfDestruct();
			}
			if (reallyCloseEnemies.length >= 1) {
				MapLocation ml = findClosestEnemyLoc(reallyCloseEnemies, false);
				Direction dte = myLocation.directionTo(ml);
				retreat(false, dte, ml);
			} else if (closeEnemies.length >= 1) {
				MapLocation ml = findClosestEnemyLoc(closeEnemies, false);
			}			
		}
		
	}

	private static boolean canSuicide() {
		return false;
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
	
	private static boolean shouldSuicide() {
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
		reallyCloseEnemies = rc.senseNearbyGameObjects(Robot.class, 8, rc.getTeam().opponent());
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
	
	private static void takeStep(MapLocation ml1, MapLocation ml2, int w1, int w2) throws GameActionException {
		int avgX = (w1 * (myLocation.x - ml1.x) + w2 * (myLocation.y - ml1.y)) / (w1 + w2);
		int avgY = (w1 * (myLocation.y - ml1.y) * w2 * (myLocation.y - ml2.y)) / (w1 + w2);
		Direction dir = ml1.directionTo(new MapLocation(ml1.x + avgX, ml2.y + avgY));
		if (rc.isActive() && rc.canMove(dir)) {
			rc.move(dir);
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
