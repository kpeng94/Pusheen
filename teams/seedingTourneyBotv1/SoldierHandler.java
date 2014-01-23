package seedingTourneyBotv1;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public static MapLocation enemyHQLocation;
	public static MapLocation myHQLocation;
	public static MapLocation prioritizedEnemy; 
	public static MapLocation targetLocation = new MapLocation(-1, -1);
	public static boolean readCowMap = false;
	public static int targetLoc = 0;
	public static boolean reachedDestination = false;
	public static boolean shouldRushEnemyPASTR = false;
	
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

		targetLoc = rc.readBroadcast(10);
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
			if (shouldBuildPASTR()) {
				buildPASTR(pastrLocation);				
			} else {
				if (!reachedDestination) {
					goToPASTR(pastrLocation, dir);
				}
				checkIfShouldRushEnemyPASTR();
			}
		}			
		// There is a location for the PASTR we want to build.
		
		if (rc.isActive()) {
			if (shouldAttack()) {
				tryAttack();
			} else {
//				add destination check
				if (targetLocation.x != -1 && (rc.getLocation().x != targetLocation.x || 
											   rc.getLocation().y != targetLocation.y)) {
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
		if (targetLoc.x != enemyHQLocation.x || targetLoc.y != enemyHQLocation.y) {
			prioritizedEnemy = targetLoc;
			return true;
		}
		return false; 
	}
	
	/* Attempts to attack */
	private void tryAttack() throws GameActionException {
		if (shouldRushEnemyPASTR) {
			attackEnemyPASTRMicro();
		}
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
			if (rc.readBroadcast(20002) != 0 && destination.x == rc.getLocation().x &&
				destination.y == rc.getLocation().y && rc.isActive()) {
				rc.construct(RobotType.PASTR);
			}
		}		
	}

	/* used to check if this robot should be the one to build the pastr */
	private boolean shouldBuildPASTR() throws GameActionException {
		int bc = rc.readBroadcast(30000);
		return bc == 0 || bc == id;
	}
	
	private void goToPASTR(MapLocation pastrLoc, Direction dir) throws GameActionException {
		// Locations to go to:
		// pastrLoc + dir * 3 + dirRotated * -2, -1, 0, 1, 2
		if (rc.readBroadcast(30000) != id) {
			for (int i = 5; i-- > 0;) {
				if (rc.readBroadcast(20000 + i) == 0) {				
					targetLocation = pastrLoc.add(dir, 3).add(dir.rotateLeft().rotateLeft(), i - 2);
					Navigation.setDest(targetLocation); 
					break;
				}
			}
			// If we reached the target location, broadcast to the channel		
			if (targetLocation.x != -1 && (rc.getLocation().x == targetLocation.x &&
										   rc.getLocation().y == targetLocation.y)) {
				reachedDestination = true;
				if (dir == Direction.EAST) {
					rc.broadcast(20000 + pastrLoc.y - targetLocation.y + 2, id);
				} else if (dir == Direction.WEST) {
					rc.broadcast(20000 + targetLocation.y - pastrLoc.y + 2, id);
				} else if (dir == Direction.NORTH) {
					rc.broadcast(20000 + pastrLoc.x - targetLocation.x + 2, id);
				} else if (dir == Direction.SOUTH) {
					rc.broadcast(20000 + targetLocation.x - pastrLoc.x + 2, id);				
				}
			}			
		}
	}
	
	public MapLocation prioritizeTarget(Robot[] nearbyEnemies) throws GameActionException {
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

	private void checkIfShouldRushEnemyPASTR() {
		// Attack should be only if enemy has more pastrs than we do.
		// (2+ enemy pastrs, when we only have 1)
		if (!shouldRushEnemyPASTR) {
			MapLocation[] enemyPASTRs = rc.sensePastrLocations(rc.getTeam().opponent());
			if (enemyPASTRs.length >= 2) {
				shouldRushEnemyPASTR = true;
				int farthestDist = enemyPASTRs[enemyPASTRs.length - 1].distanceSquaredTo(enemyHQLocation);
				MapLocation farthestPASTRFromTheirHQ = enemyPASTRs[enemyPASTRs.length - 1];
				for (int i = enemyPASTRs.length - 1; i-- > 0;) {
					int newDist = enemyPASTRs[i].distanceSquaredTo(enemyHQLocation);
					if (newDist > farthestDist) {
						farthestDist = newDist;
						farthestPASTRFromTheirHQ = enemyPASTRs[i];
					}
				}
//				System.out.println("omgomgomg");
//				System.out.println("their farthest: " + farthestPASTRFromTheirHQ);
//				System.out.println(reachedDestination);
				targetLocation = farthestPASTRFromTheirHQ;
				Navigation.setDest(farthestPASTRFromTheirHQ, 9);
			} else if (Clock.getRoundNum() % 10 == 0 && rc.senseTeamMilkQuantity(rc.getTeam().opponent()) > 
												 rc.senseTeamMilkQuantity(rc.getTeam())) {
				shouldRushEnemyPASTR = true;
				if (enemyPASTRs.length >= 1) {
					Navigation.setDest(enemyPASTRs[0], 9);
					targetLocation = enemyPASTRs[0];
				}
			}
		}
	}
	
	private void attackEnemyPASTRMicro() throws GameActionException {	
		// Prioritize points in enemy PASTR with a lot of cows
		MapLocation[] enemyPASTRLocations = rc.sensePastrLocations(rc.getTeam().opponent());
		for (int i = enemyPASTRLocations.length; i-- > 0;) {
			MapLocation location = enemyPASTRLocations[i];
			if (location.distanceSquaredTo(rc.getLocation()) > 35) {
				continue;
			} else {
				for (int j = 5; j-- > 0;) {
					for (int k = 5; k-- > 0;) {
						MapLocation locationInEnemyPASTR = new MapLocation(location.x - j + 2, location.y - k + 2);
						if (rc.canSenseSquare(locationInEnemyPASTR)) {
							double cowsNearEnemy = rc.senseCowsAtLocation(locationInEnemyPASTR);
							if (cowsNearEnemy >= 2000 && rc.canAttackSquare(locationInEnemyPASTR)) {
								rc.attackSquare(locationInEnemyPASTR);
							}
						}
					}
				}
			}
		// Prioritize attacking enemies first, because if we don't do that, the enemy can deny the PASTR more easily.		
		}		
	}

	private void checkToFillSpots() throws GameActionException {
		if (rc.readBroadcast(30000) == id) {
		    rc.broadcast(15000, Clock.getRoundNum());
    	} else if (rc.readBroadcast(20000) == id) {
		    rc.broadcast(15001, Clock.getRoundNum());
		} else if (rc.readBroadcast(20001) == id) {
		    rc.broadcast(15002, Clock.getRoundNum());
		} else if (rc.readBroadcast(20002) == id) {
		    rc.broadcast(15003, Clock.getRoundNum());
		} else if (rc.readBroadcast(20003) == id) {
		    rc.broadcast(15004, Clock.getRoundNum());
		} else if (rc.readBroadcast(20004) == id) {
		    rc.broadcast(15005, Clock.getRoundNum());
		}
	}
}
