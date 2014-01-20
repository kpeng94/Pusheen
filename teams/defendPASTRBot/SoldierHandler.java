package defendPASTRBot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};

	static Robot[] nearbyEnemies;
	static Robot[] attackableEnemies;
	
	static MapLocation myLocation;
	public SoldierHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		myLocation = rc.getLocation();
		rc.setIndicatorString(0, "" + id);
		MapLocation pastrLocation = rc.senseHQLocation().add(Direction.EAST, 1);
		defendPASTR(0, pastrLocation, Direction.EAST);
		buildPASTR(0, pastrLocation);
		attackAtEnemies(0);
		denyPASTR();
	}
	
	public void defendPASTR(int pastrNum, MapLocation loc, Direction dir) throws GameActionException {
		positionForDefendingPASTR(pastrNum, loc, dir);
	}

	public void buildPASTR(int pastrNum, MapLocation loc) throws GameActionException {
		if (id == pastrNum * 7 + 7 && myLocation.distanceSquaredTo(loc) == 0) {
			rc.construct(RobotType.PASTR);
		}
	}
	
	public void attackAtEnemies(int pastrNum) throws GameActionException {
		if (id != pastrNum * 7 + 7 && id != pastrNum * 7 + 4) {
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
			MapLocation targetLocation = prioritizeTarget(enemyRobots);
			if(rc.canAttackSquare(targetLocation) && rc.getActionDelay() <= 1) {
				rc.attackSquare(targetLocation);
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
	
	public void denyPASTR() throws GameActionException {
		MapLocation[] myPastrLocations = rc.sensePastrLocations(rc.getTeam());
		if (myPastrLocations.length > 0) {
			Robot[] pastrs = rc.senseNearbyGameObjects(Robot.class, myPastrLocations[0], 0, rc.getTeam());
			RobotInfo info = rc.senseRobotInfo(pastrs[0]);
			if (info.health <= 10) {
				rc.attackSquare(info.location);
			}
		}
	}
	
	public void positionForDefendingPASTR(int pastrNum, MapLocation loc, Direction dir) throws GameActionException {
		MapLocation newLocation = loc.add(dir, 2);
		if (id == pastrNum * 7 + 1) {
			goToLocation(newLocation.add(dir.rotateLeft().rotateLeft(), 2));
		} else if (id == pastrNum * 7 + 2) {
			goToLocation(newLocation.add(dir.rotateLeft().rotateLeft(), 1));
		} else if (id == pastrNum * 7 + 3) {
			goToLocation(newLocation.add(dir.rotateRight().rotateRight(), 2));
		} else if (id == pastrNum * 7 + 4) {
			goToLocation(loc.add(dir, -3));			
		} else if (id == pastrNum * 7 + 5) {
			goToLocation(newLocation.add(dir.rotateRight().rotateRight(), 1));			
		} else if (id == pastrNum * 7 + 6) {
			goToLocation(newLocation);
		} else if (id == pastrNum * 7 + 7) {
			goToLocation(loc);
		}
		
	}

	public void goToLocation(MapLocation location) throws GameActionException {
		Direction moveDir = myLocation.directionTo(location);
		if (moveDir != Direction.OMNI && moveDir != Direction.NONE && rc.canMove(moveDir)) {
			BasicPathing.tryToMove(moveDir, true, rc, directionalLooks, dir);
		}
	}

}
