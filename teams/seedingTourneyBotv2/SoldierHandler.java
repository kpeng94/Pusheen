package seedingTourneyBotv2;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public static MapLocation enemyHQLocation;
	public static MapLocation myHQLocation;
	public static MapLocation prioritizedEnemy; 
	public static MapLocation targetLocation = new MapLocation(-1, -1);
	public static boolean readCowMap = false;
	public static int bestCGRLoc = 0;
	public static boolean reachedDestination = false;
	public static boolean shouldRushEnemyPASTR = false;
	public static MapLocation pastrLocation; // where we want to build the pastr
	public static MapLocation[] enemyPASTRs; 
	public static int channelClaimed = 0;
	public static MapLocation closeToMe; 
	public static boolean bumRushing;
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
		closeToMe = new MapLocation((2 * myHQLocation.x + enemyHQLocation.x) / 3, 
													 (2 * myHQLocation.y + enemyHQLocation.y) / 3);
		Navigation.init(rc, closeToMe, 25);
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();

		checkToFillSpots();
		
		// Navigation for each soldier
		if (!Navigation.mapDone) {
			if (rc.readBroadcast(1) == 1)
				Navigation.mapDone = true;
		}
		
		// Keep checking for the best cow growth rate location until the HQ broadcasts it.
		bestCGRLoc = rc.readBroadcast(10);
		if (bestCGRLoc != 0) {
			pastrLocation = new MapLocation(bestCGRLoc / 100, bestCGRLoc % 100);
			int obj = decideObjective();
			if (obj == 1) {
				buildPASTR(pastrLocation);				
			} else if (obj == 2) {
				if (!reachedDestination) {
					// If we have reached the destination (which can only be set true by the defendPASTR
					// method, we don't need to move, we only need to stay put and attack when necessary
					// for now).
					defendPASTR();					
				}
			} else if (obj == 3) {
				bumRush();
			} else if (obj == 4) {
				buildNoiseTower(pastrLocation);
			} else {
				tryToBeUseful();				
			}
		}			
		// There is a location for the PASTR we want to build.
		
		if (rc.isActive()) {
			if (shouldAttack()) {
				tryAttack();
			} else {
				if (targetLocation.x != -1 && (rc.getLocation().x != targetLocation.x || 
											   rc.getLocation().y != targetLocation.y)) {
					tryMove();					
				}
			}
		}
		
		calculate();
	}

	private void buildNoiseTower(MapLocation destination) throws GameActionException {
			// Check if 3 / 5 of the robots on our side are in decent defense position
			if (rc.readBroadcast(30001) == 0) {
				Navigation.setDest(destination);
				targetLocation = destination;
				rc.broadcast(30001, id);
			} else if (rc.readBroadcast(30001) == id) {
				// Change this later if they are not going in order
				int count = 0;
				for (int i = 5; i-- > 0;) {
					if (rc.readBroadcast(23000 + i) != 0) {
						count++;
					}
				}
				if (count >= 4 && rc.getLocation().distanceSquaredTo(pastrLocation) <= 1 && rc.isActive()) {
					rc.construct(RobotType.NOISETOWER);
				}
			}		
	}

	/**
	 * If we really don't know what to do right now, we can try to be useful.
	 */
	private void tryToBeUseful() {
		targetLocation = closeToMe;
		Navigation.setDest(targetLocation, 25);		
	}

	/**
	 * Determines what this robot should do.
	 * 0 : dawdle, do nothing
	 * 1 : build a PASTR at optimal PASTR location
	 * 2 : defend the PASTR
	 * 3 : bum rush enemy
	 * 4 : build noise tower
	 * 
	 * So here, 1 > 4 > 3 > 2 > 
	 * 
	 * @return 
	 */
	private int decideObjective() throws GameActionException {
		if (shouldBuildPASTR()) {
			return 1;
		}
		if (shouldBuildNoiseTower()) {
			return 4;
		}		
		if (shouldBumRush()) {
			return 3;
		}
		if (shouldDefendPASTR()) {
			return 2;
		} 
		return 0;
	}
	
	/**
	 * Checks if no one has built a noise tower yet or if 
	 * @return
	 * @throws GameActionException
	 */
	private boolean shouldBuildNoiseTower() throws GameActionException {
		int bc = rc.readBroadcast(30001);
		return bc == 0 || bc == id;		
	}
		
	private boolean shouldBumRush() {
		updateBumRushInfo();
		if (bumRushing) {
			if (enemyPASTRs.length >= 1) {
				targetLocation = getMostVulnerableEnemyLocation();
			}
			return true;
		}
		if (enemyPASTRs.length == 0) {
			bumRushing = false;
		}
		if (enemyPASTRs.length >= 2 || (Clock.getRoundNum() % 10 == 0 && rc.senseTeamMilkQuantity(rc.getTeam().opponent()) > 
		 rc.senseTeamMilkQuantity(rc.getTeam()) && enemyPASTRs.length >= 1)) {	
			// Attack the PASTR that is farthest from the enemy.
			// Perhaps write some code later that will avoid HQs altogether.
			targetLocation = getMostVulnerableEnemyLocation();
			bumRushing = true;
			reachedDestination = false;
			return true; 
		}
		return false;
	}

	private void updateBumRushInfo() {
		enemyPASTRs = rc.sensePastrLocations(rc.getTeam().opponent());		
	}

	/**
	 * Set the navigation to the target location and reset whatever 
	 * channels it previously promised that it was operating for.
	 * @throws GameActionException
	 */
	private void bumRush() throws GameActionException {
		Navigation.setDest(targetLocation, 10);
	}

	private MapLocation getMostVulnerableEnemyLocation() {
		int farthestDist = enemyPASTRs[enemyPASTRs.length - 1].distanceSquaredTo(enemyHQLocation);
		MapLocation farthestPASTRFromTheirHQ = enemyPASTRs[enemyPASTRs.length - 1];
		for (int i = enemyPASTRs.length - 1; i-- > 0;) {
			int newDist = enemyPASTRs[i].distanceSquaredTo(enemyHQLocation);
			if (newDist > farthestDist) {
				farthestDist = newDist;
				farthestPASTRFromTheirHQ = enemyPASTRs[i];
			}
		}
		return farthestPASTRFromTheirHQ;
	}
	/**
	 * Resets channels that this robot was responsible for before.
	 * This should only be called if the robot is changing its action 
	 * (for example, from defending a pastr to bum rushing).
	 * @throws GameActionException
	 */
	private void resetChannels() throws GameActionException {
		for (int i = 5; i-- > 0;) {
			if (rc.readBroadcast(20000 + i) == id) {
				rc.broadcast(20000 + i, 0);
			}
			if (rc.readBroadcast(21000 + i) == id) {
				rc.broadcast(21000 + i, 0);
			}
		}		
	}

	/**
	 * Channels: 
	 * 20000 - 20004: information about whether or not a robot has defended a position.
	 * 21000 - 21004: information about whether or not a robot has claimed a position to defend.
	 * 22000 - 22004: locations that will be defended by each of these bots respectively
	 * 23000 - 23004: information about whether or not a robot is close by to the position.
	 * 
	 * If all channels have been claimed by an ID, there's no need to defend it.
	 * @return
	 * @throws GameActionException
	 */
	private boolean shouldDefendPASTR() throws GameActionException {
		// We should defend the pastr in the early stages. We can check this by checking broadcast channels.
		for (int i = 5; i-- > 0;) {
			if (channelClaimed == 0 && rc.readBroadcast(21000 + i) == 0) {
				int bc = rc.readBroadcast(22000 + i);
				if (bc != 0) {
					targetLocation = new MapLocation(bc / 100, bc % 100);
					rc.broadcast(21000 + i, id);
					rc.broadcast(15001 + i, Clock.getRoundNum());
					channelClaimed = 21000 + i;
					return true;
				}
			} else if (channelClaimed != 0) {
				return true;
			}
		}
		return false;
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
			rc.broadcast(15000, Clock.getRoundNum());
		} else if (rc.readBroadcast(30000) == id) {
			// Change this later if they are not going in order
			int count = 0;
			for (int i = 5; i-- > 0;) {
				if (rc.readBroadcast(23000 + i) != 0) {
					count++;
				}
			}
			if (count >= 3 && destination.x == rc.getLocation().x &&
				destination.y == rc.getLocation().y && rc.isActive()) {
				rc.construct(RobotType.PASTR);
			}
		}		
	}

	/* used to check if this robot should be the one to build the pastr 
	 * The robot should basically build a pastr whenever one doesn't exist (ours got destroyed,
	 * or beginning).
	 */	
	private boolean shouldBuildPASTR() throws GameActionException {
		int bc = rc.readBroadcast(30000);
		return bc == 0 || bc == id;
	}
	
	private void defendPASTR() throws GameActionException {
		// Locations to go to:
		// pastrLoc + dir * 3 + dirRotated * -2, -1, 0, 1, 2
		if (rc.readBroadcast(30000) != id) {
			Navigation.setDest(targetLocation);
			// If we reached the target location, broadcast to the channel		
			MapLocation ml = rc.getLocation();
			if (targetLocation.x != -1) { 
				if (ml.x == targetLocation.x && ml.y == targetLocation.y) {
					reachedDestination = true;
					rc.broadcast(channelClaimed - 1000, id);
				} else if (ml.distanceSquaredTo(targetLocation) <= 2){
					rc.broadcast(channelClaimed + 2000, id);
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
			rc.setIndicatorString(0, ""+id);
			rc.setIndicatorString(1, "pastr");
		    rc.broadcast(15000, Clock.getRoundNum());
    	} else if (rc.readBroadcast(21000) == id) {
    		rc.setIndicatorString(0, ""+id);
    		rc.setIndicatorString(1, "defend");
    		rc.broadcast(15001, Clock.getRoundNum());
		} else if (rc.readBroadcast(21001) == id) {
    		rc.setIndicatorString(0, ""+id);
    		rc.setIndicatorString(1, "defend");
		    rc.broadcast(15002, Clock.getRoundNum());
		} else if (rc.readBroadcast(21002) == id) {
    		rc.setIndicatorString(0, ""+id);
    		rc.setIndicatorString(1, "defend");
		    rc.broadcast(15003, Clock.getRoundNum());
		} else if (rc.readBroadcast(21003) == id) {
    		rc.setIndicatorString(0, ""+id);
    		rc.setIndicatorString(1, "defend");
		    rc.broadcast(15004, Clock.getRoundNum());
		} else if (rc.readBroadcast(21004) == id) {
    		rc.setIndicatorString(0, ""+id);
    		rc.setIndicatorString(1, "defend");
		    rc.broadcast(15005, Clock.getRoundNum());
		}
	}
}
