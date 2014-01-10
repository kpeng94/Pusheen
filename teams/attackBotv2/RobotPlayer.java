package attackBotv2;

import battlecode.common.*;

/**
 * 
 * @author nivek
 *
 */
public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	/* Static values (these do not change over multiple rounds) */
	static Rand rand; // Random number generator
	static RobotController rc; // Robot Controller
	static MapLocation enemyHQ; // Location of enemy HQ
	static MapLocation teamHQ; // Location of team HQ
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	static MapLocation swarmLoc;

	/* Dynamic values (these change every round, but are cached to prevent recomputation */
	static MapLocation curLoc; // Current location of the robot

	static int temp;
	static int divider = 3;

	/* Main Method */
	public static void run(RobotController rcin) {
		/* Initialize static values */
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		
		// Calculates a swarming position between the enemy and team HQ [parameter divider puts how far along the line between the HQs the swarm goes]
		enemyHQ = rc.senseEnemyHQLocation();
		teamHQ = rc.senseHQLocation();
		int swarmX = teamHQ.x + (enemyHQ.x-teamHQ.x) / divider;
		int swarmY = teamHQ.y + (enemyHQ.y-teamHQ.y) / divider;
		rc.setIndicatorString(0, swarmX+"");
		rc.setIndicatorString(1, swarmY+"");
		swarmLoc = new MapLocation(swarmX, swarmY);

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
					e.printStackTrace();
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
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			Direction spawnDir = dir[rand.nextAnd(0b111)];
			if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null)
				rc.spawn(spawnDir);
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
		// Otherwise, spend all most remaining bytecodes left on calculations
		while (Clock.getBytecodesLeft() > 1000) {

		}
	}

	/* Runs code for Soldiers */
	private static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam().opponent());
			if (enemyRobots.length > 0){
				for(int i = enemyRobots.length; i-- > 0;){
					RobotInfo info = rc.senseRobotInfo(enemyRobots[i]);
					if(info.type != RobotType.HQ && rc.canAttackSquare(info.location)){
						rc.attackSquare(info.location);
						return;
					}
				}
			}

			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam().opponent());
			Direction moveDir;
			int switchCase = rand.nextAnd(0b1111);
			switch(switchCase){
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				moveDir = curLoc.directionTo(swarmLoc);
				// Only attack PASTRs that are not within the striking range of an HQ.
				if(pastrs.length > 0){
					for(int i = pastrs.length; i-->0;){
						if(pastrs[i].distanceSquaredTo(enemyHQ) > 25){
							moveDir = curLoc.directionTo(pastrs[0]);
							break;
						}
					}
				}
				break;
			default:
				moveDir = dir[switchCase-8];
				break;
			}
			if (moveDir != Direction.OMNI && moveDir != Direction.NONE && rc.canMove(moveDir))
				BasicPathing.tryToMove(moveDir, true, rc, directionalLooks, dir);
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