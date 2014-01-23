package seedingTourneyBotv1;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public static MapLocation enemyHQLocation;
	public static MapLocation myHQLocation;
	public static MapLocation prioritizedEnemy; 
	public static MapLocation targetLocation = new MapLocation(-1, -1);
	public static boolean readCowMap = false;
	public static int targetLoc = 0;
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
		MapLocation targetLoc = new MapLocation((2 * myHQLocation.x + enemyHQLocation.x) / 3, 
													 (2 * myHQLocation.y + enemyHQLocation.y) / 3);
		Navigation.init(rc, targetLoc, 25);
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
		if (!Navigation.mapDone) {
			if (rc.readBroadcast(1) == 1)
				Navigation.mapDone = true;
		}

		if (CowMap.mapDone) {
			System.out.println("blah blah: " + targetLocation);
			if (!readCowMap) {
				targetLoc = rc.readBroadcast(10);
				readCowMap = true;
			}
			if (targetLoc != 0) {
				MapLocation pastrLocation = new MapLocation(targetLoc / 100, targetLoc % 100);
				Direction dir = pastrLocation.directionTo(enemyHQLocation);
				if (dir == Direction.OMNI || dir == Direction.NONE) {
					dir = myHQLocation.directionTo(enemyHQLocation);
				}			
				if (dir == Direction.NORTH_EAST || dir == Direction.NORTH_WEST ||
					dir == Direction.SOUTH_EAST || dir == Direction.SOUTH_WEST) {
					dir = dir.rotateLeft();
				} 
				buildPASTR(pastrLocation);
				goToPASTR(pastrLocation, dir);
			}			
		}
		// There is a location for the PASTR we want to build.
		
		if (rc.isActive()) {
			if (shouldAttack()) {
				tryAttack();
			}
			else {
//				add destination check
				if (targetLocation.x != -1 && rc.getLocation() != targetLocation) {
					tryMove();					
				}
			}
		}
		
		calculate();
	}

	
	/* Determines whether the robot should attack this turn */
	private boolean shouldAttack() throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		MapLocation targetLoc = prioritizeTarget(enemyRobots);
		if (targetLoc != enemyHQLocation) {
			prioritizedEnemy = targetLoc;
			return true;
		}
		return false; 
	}
	
	/* Attempts to attack */
	private void tryAttack() throws GameActionException {
		if(rc.canAttackSquare(prioritizedEnemy) && rc.getActionDelay() <= 1) {
			rc.attackSquare(prioritizedEnemy);
		}			
	}
	
	/* Attempts to move */
	private void tryMove() throws GameActionException {
		Navigation.swarmMove();
	}
	
	/* Does calculations */
	private void calculate() {
		Navigation.calculate();
	}

	/* Code for the robot that will build the pastr */
	private void buildPASTR(MapLocation destination) throws GameActionException {
		// Check if 3 / 5 of the robots on our side are in decent defense position
		if (rc.readBroadcast(30000) == 0) {
			Navigation.setDest(destination);
			targetLocation = destination;
			rc.broadcast(30000, id);
		} else if (rc.readBroadcast(30000) == id) {
			// Change this later if they are not going in order
			if (rc.readBroadcast(20002) != 0 && destination == rc.getLocation()) {
				rc.construct(RobotType.PASTR);
			}
		}		
	}
		
	private void goToPASTR(MapLocation pastrLoc, Direction dir) throws GameActionException {
		// Locations to go to:
		// pastrLoc + dir * 3 + dirRotated * -2, -1, 0, 1, 2
		if (rc.readBroadcast(30000) != id) {
			for (int i = 4; i-- > 0;) {
				if (rc.readBroadcast(20000 + i) == 0) {				
					targetLocation = pastrLoc.add(dir, 3).add(dir.rotateLeft().rotateLeft(), i - 2);
					Navigation.setDest(targetLocation); 
					break;
				}
			}
			// If we reached the target location, broadcast to the channel		
			if (targetLocation.x != -1 && rc.getLocation() == targetLocation) {
				if (dir == Direction.EAST) {
					rc.broadcast(20000 + targetLocation.y - pastrLoc.y + 2, id);
				} else if (dir == Direction.WEST) {
					rc.broadcast(20000 + pastrLoc.y - targetLocation.y + 2, id);
				} else if (dir == Direction.NORTH) {
					rc.broadcast(20000 + pastrLoc.x - targetLocation.x + 2, id);
				} else if (dir == Direction.SOUTH) {
					rc.broadcast(20000 + targetLocation.x - pastrLoc.x + 2, id);				
				}
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
	
	private void attackEnemyPASTR() {
		// Attack should be only if enemy has more pastrs than we do.
		// (2+ enemy pastrs, when we only have 1)
		if (rc.sensePastrLocations(rc.getTeam().opponent()).length >= 2) {
			
		}
	}
	
}
