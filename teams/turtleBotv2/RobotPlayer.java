package turtleBotv2;

import battlecode.common.*;

/* Written by Rene */
public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	static MapLocation ourLoc; // Location of our HQ
	static MapLocation enemyLoc; // Location of enemy HQ
	
	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot
	static int lastNoise;
	
	/* Main Method */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		ourLoc = rc.senseHQLocation();
		enemyLoc = rc.senseEnemyHQLocation();
		
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
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			int[] rot90 = new int[] {4, -2, 2};
			int fromEnemy = enemyLoc.directionTo(ourLoc).ordinal();
			int farCorner = fromEnemy + ((fromEnemy + 1) % 2);
			int farSide = fromEnemy - (fromEnemy % 2);
			if (trySpawn(dir[farCorner]))
				return;
			if (trySpawn(dir[farSide]))
				return;
			for (int i = 3; i-- > 0;) {
				if (trySpawn(dir[(farCorner + rot90[i] + 8) % 8]))
					return;
			}
			for (int i = 3; i-- > 0;) {
				if (trySpawn(dir[(farSide + rot90[i] + 8) % 8]))
					return;
			}
		}
		// Attack nearby enemies
		attack : {
			Robot[] enemy = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
			for (int i = enemy.length; i-- > 0;) {
				MapLocation loc = rc.senseRobotInfo(enemy[i]).location;
				if (rc.canAttackSquare(loc)) {
					rc.attackSquare(loc);
					break attack;
				}
				else {
					loc = new MapLocation(loc.x - Integer.signum(loc.x - curLoc.x), loc.y - Integer.signum(loc.y - curLoc.y));
					if (rc.canAttackSquare(loc)) {
						rc.attackSquare(loc);
						break attack;
					}
				}
			}
		}
	}

	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			if (Math.abs(curLoc.x - ourLoc.x) + Math.abs(curLoc.y - ourLoc.y) == 2) {
				if (Clock.getRoundNum() > 80) {
					rc.construct(RobotType.PASTR);
				}
			}
			else if (Clock.getRoundNum() > 30) {
				rc.construct(RobotType.NOISETOWER);
			}
		}
	}
	
	/* Runs code for PASTRS */
	private static void runPastr() throws GameActionException {
		
	}

	/* Runs code for Noise Towers */
	private static void runNoise() throws GameActionException {
		MapLocation HQLoc = rc.senseHQLocation();
		if (lastNoise < 8)
			lastNoise = (lastNoise + 1) % 8 + 8*4 + 40;
		else if (lastNoise >= 40)
			lastNoise -= 40;
		else
			lastNoise += 32;

		MapLocation atk;
		if (lastNoise % 2 == 0)
			atk = HQLoc.add(dir[lastNoise % 8], 5 + 3*((lastNoise / 8) % 5));
		else
			atk = HQLoc.add(dir[lastNoise % 8], 4 + 2*((lastNoise / 8) % 5));
		rc.attackSquare(atk);
	}
	
	private static boolean trySpawn(Direction spawnDir) throws GameActionException {
		if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null) {
			rc.spawn(spawnDir);
			return true;
		}
		return false;
	}
	
}