package kevinBot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static MapLocation enemyHQLocation;
	public static MapLocation myHQLocation;
	public static MapLocation closeToMe; 
	public static Direction myHQtoenemyHQ;
	public static int myHQtoenemyHQint;
	
	public SoldierHandler(RobotController rcin) {
		super(rcin);
		enemyHQLocation = rc.senseEnemyHQLocation();
		myHQLocation = rc.senseHQLocation();
		myHQtoenemyHQ = myHQLocation.directionTo(enemyHQLocation);
		myHQtoenemyHQint = myHQtoenemyHQ.ordinal();
		closeToMe = new MapLocation((myHQLocation.x + enemyHQLocation.x) / 2, 
													 (myHQLocation.y + enemyHQLocation.y) / 2);
		Navigation.init(rc, closeToMe, 25);
		Attack.init(rc, id, closeToMe);
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
		// Navigation for each soldier
		if (!Navigation.mapDone) {
			if (rc.readBroadcast(1) == 1)
				Navigation.mapDone = true;
		}
		int squadronNumber = rc.readBroadcast(13300 + id);
		boolean squadronLeader = rc.readBroadcast(13000 + squadronNumber) == id;		
		if (rc.isActive()) {
			if (!squadronLeader && rc.readBroadcast(13040 + squadronNumber) != 0) {
				tryAttack();
			} else if (squadronLeader) {
				tryAttack();
				tryMove();													
			} else {
				tryMove();													
			}
		}
		
		calculate();
	}	
	
	private void detectLeader() throws GameActionException {
		
	}
	
	/* Attempts to attack */
	private void tryAttack() throws GameActionException {
		Attack.attackMicro();
	}
	
	/* Attempts to move */
	private void tryMove() throws GameActionException {
		if (rc.isActive()) {
			Navigation.swarmMove();
			rc.setIndicatorString(1, "I moved on round " + Clock.getRoundNum());
		}
	}
	
	/* Does calculations */
	private void calculate() {
		Navigation.calculate();
	}

	/**
	 * 
	 */
	private void selectStrategy() {
		
	}
}
