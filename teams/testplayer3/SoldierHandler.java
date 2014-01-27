package testplayer3;

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
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
		closeToMe = new MapLocation((2*myHQLocation.x + enemyHQLocation.x) / 3, 
													 (2*myHQLocation.y + enemyHQLocation.y) / 3);
		Navigation.init(rc, closeToMe, 25);
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
		
		rc.setIndicatorString(0, targetLocation.x+", "+targetLocation.y);
		// Navigation for each soldier
		if (!Navigation.mapDone) {
			if (rc.readBroadcast(1) == 1)
				Navigation.mapDone = true;
		}
		
		// Keep checking for the best cow growth rate location until the HQ broadcasts it.
		// There is a location for the PASTR we want to build.
		
		if (Clock.getRoundNum()<=100){
			targetLocation=new MapLocation(5,25);
			Navigation.setDest(targetLocation);
		} else if (Clock.getRoundNum()<=200){
			targetLocation=new MapLocation(25,5);
			Navigation.setDest(targetLocation);
		} else {
			targetLocation=new MapLocation(5,25);
			Navigation.setDest(targetLocation);			
		}
		if (rc.isActive()) {
			tryMove();
		}
		
		calculate();
	}


	private void tryMove() throws GameActionException {
		Navigation.swarmMove();
	}
	
	/* Does calculations */
	private void calculate() {
		Navigation.calculate();
	}
}
