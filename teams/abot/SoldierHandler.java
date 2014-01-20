package abot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public SoldierHandler(RobotController rcin) {
		super(rcin);
		Navigation.init(rc, rc.senseEnemyHQLocation());
	}

	@Override
	public void execute() throws GameActionException{
		super.execute();
		if (!Navigation.mapDone) {
			if (rc.readBroadcast(1) == 1)
				Navigation.mapDone = true;
		}
		
		if (rc.isActive()) {
			if (shouldAttack()) {
				tryAttack();
			}
			else {
				tryMove();
			}
		}
		
		calculate();
	}

	/* Determines whether the robot should attack this turn */
	private boolean shouldAttack() {
		return false;
	}
	
	/* Attempts to attack */
	private void tryAttack() {
		
	}
	
	/* Attempts to move */
	private void tryMove() {
		Navigation.swarmMove(4);
	}
	
	/* Does calculations */
	private void calculate() {
		
	}

}
