package navBotv3;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public SoldierHandler(RobotController rcin) throws GameActionException {
		super(rcin);
		Map.init(rc);
		Navigation.init(rc);
		Navigation.setDest(rc.senseEnemyHQLocation());
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
//		if (!Navigation.mapDone) {
//			if (rc.readBroadcast(1) == 1)
//				Navigation.mapDone = true;
//		}
		rc.setIndicatorString(2, "" + Navigation.dest);
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
		Navigation.move();
	}
	
	/* Does calculations */
	private void calculate() throws GameActionException {
		Navigation.calculate(1000);
		Map.calculate(1000);
	}

}
