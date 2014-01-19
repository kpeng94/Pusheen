package suicideMicroBot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};

	static Robot[] nearbyEnemies;
	static Robot[] attackableEnemies;
	static MapLocation myLoc; 
	public SoldierHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		myLoc = rc.getLocation();
//		if (Clock.getRoundNum() % 2 == 0) {
		nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		attackableEnemies = rc.senseNearbyGameObjects(Robot.class, 10, rc.getTeam().opponent());
//		}
//		int lowestHealth = 1000;

		if (rc.isActive()) {
			if (makeDecision() == 0) {
				MapLocation closestEnemy = getClosestEnemy(nearbyEnemies);
				Direction moveDirection = myLoc.directionTo(closestEnemy);
				if(closestEnemy.distanceSquaredTo(myLoc) <= 2) {
					rc.selfDestruct();
				} else {
					BasicPathing.tryToMove(moveDirection, true, rc, directionalLooks, dir);					
				}
			}
		}
	}

	/**
	 * This method decides whether the robot should suicide or not 
	 * depending on whether there are a lot of robots.
	 *
	 * 0: suicide
	 * 1: normal attack
	 */
	public int makeDecision() {
		return 0;
	}

	/**
	 * 
	 * @param nearbyEnemies
	 * @return
	 */
	public MapLocation getClosestEnemy(Robot[] nearbyEnemies) throws GameActionException {
		MapLocation closestEnemy = rc.senseEnemyHQLocation();
		int distanceSquared = myLoc.distanceSquaredTo(closestEnemy);
//		int instead for bytecode saving?
		
		for (int i = nearbyEnemies.length; i-- > 0;) {
			RobotInfo info = rc.senseRobotInfo(nearbyEnemies[i]);
			MapLocation location = info.location;
			int newDistanceSquared = location.distanceSquaredTo(myLoc);
			if (newDistanceSquared < distanceSquared) {
				closestEnemy = location;
				distanceSquared = newDistanceSquared;
			}
		}
		return closestEnemy;
	}
		
}
