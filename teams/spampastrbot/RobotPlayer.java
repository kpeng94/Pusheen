package spampastrbot;

import battlecode.common.*;

public class RobotPlayer {
static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	
	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	
	static int temp;
	
	/* Main Method */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		
		while (true) {
			/* Save dynamic values */
			curLoc = rc.getLocation();
			
			/* Distributes actions to their respective functions */
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
				try {
					runPastr();
				}
				catch (Exception e) {
					System.out.println("Pastr Exception");
				}
				break;
			case NOISETOWER:
				try {
					runNoise();
				}
				catch (Exception e) {
					System.out.println("NoiseTower Exception");
				}
				break;
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	/* Runs code for HQ */
	private static void runHQ() throws GameActionException {
		// Spawn in a random direction if its able to spawn
//		if (rc.isActive() && rc.senseRobotCount() < 25) {
//			Direction spawnDir = dir[rand.nextAnd(0b111)];
//			if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null)
//				rc.spawn(spawnDir);
//		}
		if (rc.isActive()) {
			if (rc.senseRobotCount() == 0) {
				rc.spawn(rc.getTeam() == Team.A ? Direction.EAST : Direction.WEST);
			}
		}
		// Otherwise, spend all most remaining bytecodes left on calculations
		while (Clock.getBytecodesLeft() > 1000) {
			
		}
	}
	
	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			if (rc.getTeam() == Team.B) {
				rc.construct(RobotType.PASTR);
			}
			else {
				if (curLoc.x == 23) {
					Direction x = Direction.EAST;
					Direction y = Direction.NORTH;
					if (rand.nextAnd(0b1) == 1) {
						rc.attackSquare(curLoc.add(x,2).add(y,2));
					}
					else {
						rc.attackSquare(curLoc.add(x,2).add(y,-2));
					}
				}
				else {
					rc.move(Direction.EAST);
				}
			}
//			if (curLoc.x == 14) {
//				rc.construct(RobotType.NOISETOWER);
//			}
//			else {
//				rc.move(Direction.EAST);
//			}

//			Direction moveDir = dir[rand.nextAnd(0b111)];
//			if (rc.canMove(moveDir))
//				rc.move(moveDir);
		}
	}
	
	/* Runs code for PASTRS */
	private static void runPastr() throws GameActionException {
		
	}

	/* Runs code for Noise Towers */
	private static void runNoise() throws GameActionException {
		if (rc.getActionDelay() > 0) {
			System.out.println(rc.getActionDelay());
		}
		if (rc.isActive()) {
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam().opponent());
			if (pastrs.length == 0)
				return;
			int start = temp;
			while (!rc.canAttackSquare(pastrs[temp])) {
				temp = (temp + 1) % pastrs.length;
				if (start == temp)
					return;
			}
			rc.attackSquare(pastrs[temp]);
			temp = (temp + 1) % pastrs.length;
		}
	}
	
}