package mapBot;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public SoldierHandler(RobotController rcin) {
		super(rcin);
		Navigation.init(rc, rc.senseEnemyHQLocation(), 25);
	}

	@Override
	public void execute() throws GameActionException {
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
	private void tryAttack() throws GameActionException {
		
	}
	
	/* Attempts to move */
	private void tryMove() throws GameActionException {
		Navigation.swarmMove();
	}
	
	/* Does calculations */
	private void calculate() {
		Navigation.calculate();
	}

}
