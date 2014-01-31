package kevinBot;

import java.util.Map;

import battlecode.common.*;

public class Attack {
	private static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, 
												   Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static final int[] offsetsX = {0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -5, -5, -5, -5, -5, -5, -4, -3, -2, -1};
	private static final int[] offsetsY = {-5, -5, -5, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -5, -5};
	private static Robot[] closeEnemies;
	private static RobotController rc;
	private static MapLocation enemyHQLocation;
	private static int squadronNumber = 0;
	private static boolean surround = false;
	private static MapLocation mySurroundDestination;
	private static MapLocation myLocation;
	private static MapLocation newLocation;
	private static int defaultOff = 14;
	private static int id;
	private static int offTarget = 14;
	
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
			if (!surround && distanceToDest > 36) {
				Navigation.setDest(mySurroundDestination, 25);
				surround = true;
			} 
			if (isOccupied(off)) {
				rc.setIndicatorString(2, "2: " + rc.readBroadcast(26014) + " huh " + rc.readBroadcast(26013) + " next: " + rc.readBroadcast(26012));
				while (off >= 0 && isOccupied(off)) {
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
	
	private static boolean isInBounds(MapLocation ml) {
		return (ml.x < rc.getMapWidth() && ml.x >= 0 && ml.y < rc.getMapHeight() && ml.y >= 0);
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
	private static void retreat(boolean considerWalls, boolean groupEscape, Direction enemyDir) {
		if (!groupEscape && !considerWalls) {
			Direction oppositeDir = directions[(enemyDir.ordinal() + 4) % 8];
		}
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
		double myHealth = rc.getHealth();
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
	private void storeEnemiesWithinRange() {
		closeEnemies = rc.senseNearbyGameObjects(Robot.class, 10, rc.getTeam().opponent());
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
	private static void takeStep(Direction dir) throws GameActionException {
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
