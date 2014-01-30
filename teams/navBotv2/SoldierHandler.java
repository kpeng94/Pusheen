package navBotv2;

import battlecode.common.*;

public class SoldierHandler extends UnitHandler {

	public SoldierHandler(RobotController rcin) throws GameActionException {
		super(rcin);
		Map.init(rc);
		Navigationv2.init(rc, rc.senseEnemyHQLocation());
	}

	@Override
	public void execute() throws GameActionException {
		super.execute();
		rc.setIndicatorString(0, "" + Navigationv2.dest);
		
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
		Navigationv2.move();
	}
	
	/* Does calculations */
	private void calculate() throws GameActionException {
		Navigationv2.calculate(1000);
		Map.calculate(1000);
	}

}
