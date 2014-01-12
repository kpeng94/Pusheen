package genericplayer;

import battlecode.common.*;

public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	static UnitHandler unit;
	
	/* Main Method */
	public static void run(RobotController rc) {
		switch(rc.getType()) {
		case HQ:
			unit = new HQHandler(rc);
			break;
		case SOLDIER:
			unit = new SoldierHandler(rc);
			break;
		case PASTR:
			unit = new PastrHandler(rc);
			break;
		case NOISETOWER:
			unit = new NoisetowerHandler(rc);
			break;
		}

		while (true) {
			try {
				unit.execute();
			}
			catch (Exception e) {
//				e.printStackTrace();
				System.out.println(rc.getType() + " Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}
	
}