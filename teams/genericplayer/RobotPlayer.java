package genericplayer;

import battlecode.common.*;

public class RobotPlayer {
	static Rand rand;
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public static void run(RobotController rc) {
		rand = new Rand(rc.getRobot().getID());
		
		while (true) {
			switch(rc.getType()) {
			case HQ:
				try {
					runHQ();
				}
				catch (Exception e) {
					System.out.println("HQ Exception");
				}
				break;
			case SOLDIER:
				try {
					runSoldier();
				}
				catch (Exception e) {
					System.out.println("Soldier Exception");
				}
				break;
			case PASTR:
				break;
			case NOISETOWER:
				break;
			}
			rc.yield();
		}
	}

	private static void runHQ() {
			
	}
	
	private static void runSoldier() {
		
	}
	
}