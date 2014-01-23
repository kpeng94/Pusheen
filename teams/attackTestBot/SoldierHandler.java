package attackTestBot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};

	static Robot[] nearbyEnemies;
	static Robot[] attackableEnemies;

	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
	}

	public void execute() throws GameActionException{
		if (rc.isActive()) {
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
			MapLocation prioritizedLocation = prioritizeTarget(enemyRobots);
			if (prioritizedLocation.distanceSquaredTo(rc.senseEnemyHQLocation()) > 25) {
				rc.attackSquare(prioritizedLocation);
			}
		}
	}
	
	public MapLocation prioritizeTarget(Robot[] nearbyEnemies) throws GameActionException {
		double health = 110;
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
		
}