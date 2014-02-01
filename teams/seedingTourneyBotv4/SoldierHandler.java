package seedingTourneyBotv4;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
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
	public static Direction myHQtoenemyHQ;
	public static int myHQtoenemyHQint;
	public static MapLocation helpLoc;
	public static MapLocation myLoc;
	
	public SoldierHandler(RobotController rcin) throws GameActionException {
		super(rcin);
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
		myHQtoenemyHQ = myHQLocation.directionTo(enemyHQLocation);
		myHQtoenemyHQint = myHQtoenemyHQ.ordinal();
		closeToMe = new MapLocation((myHQLocation.x + enemyHQLocation.x) / 2, 
													 (myHQLocation.y + enemyHQLocation.y) / 2);
		Map.init(rc);
		Navigation.init(rc);
		Navigation.setDest(closeToMe, 10);
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
		myLoc = rc.getLocation();
		checkToFillSpots();
		
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
				MapLocation destination = pastrLocation.add(dir[7], 1);
				for (int d = 8; d-- > 0;) {
					destination = pastrLocation.add(dir[d % 8], 1);
					TerrainTile tt = rc.senseTerrainTile(destination);
					if (tt != TerrainTile.VOID && tt != TerrainTile.OFF_MAP) {
						break;
					}					
				}
				buildNoiseTower(destination);
			} else {
				tryToBeUseful();				
			}
		}	
		// There is a location for the PASTR we want to build.
		
		if (rc.isActive()) {
			if (shouldRetreat()){
				Robot[] nearbyEnemies=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
				MapLocation closestEnemy=closestTarget(nearbyEnemies);
				Direction enemyDir=rc.getLocation().directionTo(closestEnemy);
				
				retreat(enemyDir,closestEnemy);
			} else if (shouldAttack()) {
				tryAttack();
			} else if (shouldHelp()){
				goHelp();
				if (targetLocation.x != -1 && (rc.getLocation().x != targetLocation.x || 
						rc.getLocation().y != targetLocation.y)
						&& rc.readBroadcast(40000+id)==0) {
					tryMove();
				}
			} else {
				goBackToYourRole();

				if (targetLocation.x != -1 && (rc.getLocation().x != targetLocation.x || 
						rc.getLocation().y != targetLocation.y)
						&& rc.readBroadcast(40000+id)==0) {
					tryMove();					
				} else {

					moveForwardAndBack();
				}
			}
		}
		
		calculate();
	}
	
	private boolean shouldRetreat() throws GameActionException {
		Robot[] nearbyEnemies=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		Robot[] nearbyAllies=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam());
		
		if (nearbyEnemies.length!=0){
			MapLocation closestEnemy=closestTarget(nearbyEnemies);

			if (nearbyAllies.length<nearbyEnemies.length || rc.getLocation().distanceSquaredTo(closestEnemy)<=8){
				return true;
			}
		}
		return false;
	}

	private void goBackToYourRole() throws GameActionException {
		if (bumRushing) {
			if (enemyPASTRs.length >= 1) {
				targetLocation = getMostVulnerableEnemyLocation();
			}
		} else {
			for (int i = 5; i-- > 0; ) {
				if (rc.readBroadcast(21000 + i) == id) {
					int bc = rc.readBroadcast(22000 + i);
					MapLocation defendLoc = new MapLocation(bc / 100, bc % 100);
					if (targetLocation.x != defendLoc.x || targetLocation.x!=defendLoc.y){
						targetLocation=defendLoc;
						Navigation.setDest(targetLocation);
						//Navigation.setDest(targetLocation, 16);
					}

				}
			}
			if (rc.readBroadcast(30000)==id){
				if (targetLocation.x!=pastrLocation.x || targetLocation.y!=pastrLocation.y){
					targetLocation = pastrLocation;
					Navigation.setDest(targetLocation);
				}
			}
			if (rc.readBroadcast(30001)==id){
				MapLocation destination = pastrLocation.add(dir[7], 1);
				if (targetLocation.x!=destination.x ||targetLocation.y!=destination.y){
					targetLocation = destination;
					Navigation.setDest(targetLocation);
				}
			}
		}
	}

	private void goHelp() throws GameActionException {
		Navigation.setDest(targetLocation);
	}

	private void callForHelp() throws GameActionException {
		Robot nearestenemy=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent())[0];
		MapLocation enemyLoc=rc.senseRobotInfo(nearestenemy).location;
		myLoc = rc.getLocation();
		MapLocation helpLoc=myLoc.add(myLoc.directionTo(enemyLoc), (int)Math.sqrt(myLoc.distanceSquaredTo(enemyLoc))/2);
		
		rc.broadcast(50000, helpLoc.x*100+helpLoc.y);
		rc.setIndicatorString(1, helpLoc.x+", "+helpLoc.y);
	}

	private void buildNoiseTower(MapLocation destination) throws GameActionException {
			// Check if 3 / 5 of the robots on our side are in decent defense position
			if (rc.readBroadcast(30001) == 0) {
				Navigation.setDest(destination);
				targetLocation = destination;
				rc.broadcast(30001, id);
				rc.broadcast(15006, Clock.getRoundNum());
			} else if (rc.readBroadcast(30001) == id) {
				// Change this later if they are not going in order
				int count = 0;
				for (int i = 5; i-- > 0;) {
					if (rc.readBroadcast(23000 + i) != 0) {
						count++;
					}
				}
				if (count >= 4 && rc.getLocation().distanceSquaredTo(destination) == 0 && rc.isActive()) {
					rc.construct(RobotType.NOISETOWER);
				}
			}		
	}

	/**
	 * If we really don't know what to do right now, we can try to be useful.
	 * @throws GameActionException 
	 */
	private void tryToBeUseful() throws GameActionException {
		targetLocation = closeToMe;
		Navigation.setDest(targetLocation, 10);
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
	
	private boolean shouldHelp() throws GameActionException {
		int loc=rc.readBroadcast(50000);
		if (loc!=0) {
			MapLocation helpLoc=new MapLocation(loc/100,loc%100);
			if (rc.getLocation().distanceSquaredTo(helpLoc)<=36){
				targetLocation=helpLoc;
				return true;
			}
		}
		return false;
	}

	private boolean shouldCallForHelp() throws GameActionException {
		Robot[] nearbyEnemy = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		if (nearbyEnemy.length!=0){
			if (nearbyEnemy.length==1 && rc.senseRobotInfo(nearbyEnemy[0]).type==RobotType.HQ){
				return false;
			} else {
				rc.broadcast(51000, nearbyEnemy.length+2);
				return true;
			}
		} 
		return false;
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
		
	private boolean shouldBumRush() throws GameActionException {
		updateBumRushInfo();
		if (bumRushing) {
			if (enemyPASTRs.length >= 1) {
				targetLocation = getMostVulnerableEnemyLocation();
				return true;
			}
		}
		if (enemyPASTRs.length == 0) {
			bumRushing = false;
			for (int i = 5; i-- > 0; ) {
				if (rc.readBroadcast(21000 + i) == id) {
					int bc = rc.readBroadcast(22000 + i);
					targetLocation = new MapLocation(bc / 100, bc % 100);
					//Navigation.setDest(targetLocation, 16);
					Navigation.setDest(targetLocation);
				}
			}
		}
		if (enemyPASTRs.length >= 2 || (rc.senseTeamMilkQuantity(rc.getTeam().opponent()) > 
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
		if (shouldCallForHelp()){
			callForHelp();
		}
		
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
		Robot[] nearbyEnemies=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		Robot[] nearbyAllies=rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam());
		
		if (nearbyEnemies.length==0){
			Navigation.move();
		} else if (nearbyAllies.length>nearbyEnemies.length){
			Navigation.move(); 
		} 
	}
	
	/* Does calculations */
	private void calculate() throws GameActionException {
		Navigation.calculate(1000);
		Map.calculate(1000);
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
				} else if (ml.distanceSquaredTo(targetLocation) <= 10) {
					rc.broadcast(channelClaimed + 2000, id);
				}
			}
		}
	}
	
	public MapLocation closestTarget(Robot[] nearbyEnemies) throws GameActionException {
		MapLocation closestML = null;
		int distance = 100000;		
		for (int i = nearbyEnemies.length; i-- > 0;) {
			RobotInfo ri = rc.senseRobotInfo(nearbyEnemies[i]);
			MapLocation newLoc = ri.location;
			int nd = myLoc.distanceSquaredTo(newLoc);
			if (nd < distance) {
				closestML = newLoc;
				distance = nd;
			}
		}			
		return closestML;
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
    	} else if (rc.readBroadcast(21000) == id) {
    		rc.broadcast(15001, Clock.getRoundNum());
		} else if (rc.readBroadcast(21001) == id) {
		    rc.broadcast(15002, Clock.getRoundNum());
		} else if (rc.readBroadcast(21002) == id) {
		    rc.broadcast(15003, Clock.getRoundNum());
		} else if (rc.readBroadcast(21003) == id) {
		    rc.broadcast(15004, Clock.getRoundNum());
		} else if (rc.readBroadcast(21004) == id) {
		    rc.broadcast(15005, Clock.getRoundNum());
		} else if (rc.readBroadcast(30001) == id) {
			rc.broadcast(15006, Clock.getRoundNum());
		}
	}
	
	private void moveForwardAndBack() throws GameActionException {
//		for (int i = 5; i-- > 0;) {
//			if (rc.readBroadcast(21000 + i) == id) {
//				System.out.println("the location");
//				int bc = rc.readBroadcast(22000 + i);
//				if (rc.getLocation().distanceSquaredTo(new MapLocation(bc / 100, bc % 100)) > 2) {
//					return;
//				}
//			}
//		}
		if (rc.readBroadcast(21000)==id || rc.readBroadcast(21001)==id || rc.readBroadcast(21002)==id 
				|| rc.readBroadcast(21003)==id || rc.readBroadcast(21004)==id){
			if (rc.readBroadcast(40000+id)==0){
				if (rc.readBroadcast(15007)==(Clock.getRoundNum()-1) && Clock.getRoundNum()%50==0){
					if (rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent()).length==0){
						if (rc.isActive() && rc.canMove(myHQtoenemyHQ)){
							rc.broadcast(40000+id, myHQtoenemyHQint+8);
							rc.move(myHQtoenemyHQ);
						} else if (rc.isActive() && rc.canMove(Navigation.dir[(myHQtoenemyHQint+1+8)%8])){
							rc.broadcast(40000+id, myHQtoenemyHQint+1+8);
							rc.move(Navigation.dir[(myHQtoenemyHQint+1+8)%8]);
						} else if (rc.isActive() && rc.canMove(Navigation.dir[(myHQtoenemyHQint-1+8)%8])){
							rc.broadcast(40000+id, myHQtoenemyHQint-1+8);
							rc.move(Navigation.dir[(myHQtoenemyHQint-1+8)%8]);
						}
					}
				}
			} else {
				int backdir =(rc.readBroadcast(40000+id))%8;
				if (rc.isActive() && rc.canMove(Navigation.dir[(backdir+4)%8])){
					rc.broadcast(40000+id, 0);
					rc.move(Navigation.dir[(backdir+4)%8]);
				}
			}
		}
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
	public boolean retreat(Direction enemyDir, MapLocation ml) throws GameActionException {
		Direction oppositeDir = dir[(enemyDir.ordinal() + 6) % 8];
		int maxDistance = rc.getLocation().distanceSquaredTo(ml);
		for (int i = 4; i-- > 0;) {
			Direction dirAdd = dir[(enemyDir.ordinal() + i + 2) % 8];
			int newDistance = rc.getLocation().add(dirAdd).distanceSquaredTo(ml);
			if (newDistance > maxDistance) {
				oppositeDir = dir[(enemyDir.ordinal() + i + 2) % 8];
				maxDistance = newDistance;
			}
		}
		Direction toMyHQ = myLoc.directionTo(myHQLocation);
		int hqCloseness = myLoc.distanceSquaredTo(myHQLocation);
		return takeStep(oppositeDir, toMyHQ, hqCloseness, 100);
	}

	/**
	 * Takes a step in the direction 
	 *
	 */
	private boolean takeStep(Direction dir1) throws GameActionException {
		if (rc.isActive() && rc.canMove(dir1)) {
			rc.move(dir1);
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
	private boolean takeStep(Direction d1, Direction d2, int w1, int w2) throws GameActionException {
		Direction dir1 = dir[((d1.ordinal() * w1 + d2.ordinal() * w2) / (w1 + w2) + 8) % 8];
		if (rc.isActive()) {
			if (rc.canMove(dir1)) {
				rc.move(dir1);
				return true;
			} 
			Direction dir2 = dir[(dir1.ordinal() + 1) % 8];
			if (rc.canMove(dir2)) {
				rc.move(dir2);
				return true;
			} 
			Direction dir3 = dir[(dir1.ordinal() + 7) % 8];
			if (rc.canMove(dir3)) {
				rc.move(dir3);
				return true;
			}
		}
		return false;
	}
	
}
