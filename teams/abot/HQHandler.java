package abot;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	static MapLocation ourLoc;
	static MapLocation enemyLoc;
	static int[] spawnlist;
	
	public HQHandler(RobotController rcin) {
		super(rcin);
		ourLoc = rc.senseHQLocation();
		enemyLoc = rc.senseEnemyHQLocation();
		spawnlist = new int[8];
		
		// Generates spawn order
		int fromEnemy = enemyLoc.directionTo(ourLoc).ordinal();
		spawnlist[0] = fromEnemy + ((fromEnemy + 1) % 2);
		spawnlist[1] = fromEnemy - (fromEnemy % 2);
		int[] rot90 = new int[] {4, -2, 2};
		for (int i = 3; i-- > 0;) {
			spawnlist[2+i] = (spawnlist[0] + rot90[i] + 8) % 8;
			spawnlist[5+i] = (spawnlist[1] + rot90[i] + 8) % 8;
		}
	}

	@Override
	public void execute() throws GameActionException {
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			trySpawn();
		}
		tryAttack();
		calculate();
	}

	private void trySpawn() throws GameActionException {
		for (int i = spawnlist.length; i-- > 0;) {
			if (rc.senseObjectAtLocation(ourLoc.add(dir[spawnlist[i]])) == null) {
				rc.spawn(dir[spawnlist[i]]);
				return;
			}
		}
	}
	
	private void tryAttack() throws GameActionException {

		Robot[] enemy = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
		for (int i = enemy.length; i-- > 0;) {
			MapLocation loc = rc.senseRobotInfo(enemy[i]).location;
			if (rc.canAttackSquare(loc)) {
				rc.attackSquare(loc);
				return;
			}
			else {
				loc = new MapLocation(loc.x - Integer.signum(loc.x - ourLoc.x), loc.y - Integer.signum(loc.y - ourLoc.y));
				if (rc.canAttackSquare(loc)) {
					rc.attackSquare(loc);
					return;
				}
			}
		}
	}
	
	private void calculate() {
		while (Clock.getBytecodesLeft() > 1000) {
			
		}
	}

}
