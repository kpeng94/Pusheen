package attackBotv3;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};

	static Robot[] nearbyEnemies;
	static Robot[] attackableEnemies;
	static MapLocation enemyHQ; // Location of enemy HQ
	static MapLocation teamHQ; // Location of team HQ
	static MapLocation swarmLoc;

	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot

	static int temp;
	static int divider = 3;
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		enemyHQ = rc.senseEnemyHQLocation();
		teamHQ = rc.senseHQLocation();
		int swarmX = teamHQ.x + (enemyHQ.x-teamHQ.x) / divider;
		int swarmY = teamHQ.y + (enemyHQ.y-teamHQ.y) / divider;
		swarmLoc = new MapLocation(swarmX, swarmY);

		if (rc.isActive()) {
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());

			MapLocation prioritizedLocation = prioritizeTarget(enemyRobots);
			if (prioritizedLocation.distanceSquaredTo(rc.senseEnemyHQLocation()) > 25) {
				rc.attackSquare(prioritizedLocation);
			}
			
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam().opponent());
			Direction moveDir;
			int switchCase = rand.nextAnd(15);
			switch(switchCase){
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				moveDir = curLoc.directionTo(swarmLoc);
				// Only attack PASTRs that are not within the striking range of an HQ.
				if(pastrs.length > 0){
					for(int i = pastrs.length; i-->0;){
						if(pastrs[i].distanceSquaredTo(enemyHQ) > 25){
							moveDir = curLoc.directionTo(pastrs[0]);
							break;
						}
					}
				}
				break;
			default:
				moveDir = dir[switchCase-8];
				break;
			}
			if (moveDir != Direction.OMNI && moveDir != Direction.NONE && rc.canMove(moveDir))
				BasicPathing.tryToMove(moveDir, true, rc, directionalLooks, dir);
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