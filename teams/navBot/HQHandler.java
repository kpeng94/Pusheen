package navBot;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	MapLocation ourLoc;
	MapLocation enemyLoc;
	int[] spawnlist;
	
	public HQHandler(RobotController rcin) {
		super(rcin);
		ourLoc = rc.senseHQLocation();
		enemyLoc = rc.senseEnemyHQLocation();
		getSpawn();
	}

	/* Generates spawn list order */
	private void getSpawn() {
		spawnlist = new int[8];
		
		int fromEnemy = ourLoc.directionTo(enemyLoc).ordinal();
		int[] rot = new int[] {4, -3, 3, -2, 2, -1, 1, 0};
		for (int i = 8; i-- > 0;) {
			spawnlist[i] = (fromEnemy + rot[i] + 8) % 8;
		}
	}
	
	@Override
	public void execute() throws GameActionException {
		super.execute();
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			trySpawn();
		}
		tryAttack();
		calculate();
	}

	/* Tries to spawn a unit based on spawnlist */
	private void trySpawn() throws GameActionException {
		for (int i = spawnlist.length; i-- > 0;) {
			MapLocation spawnLoc = ourLoc.add(dir[spawnlist[i]]);
			TerrainTile tile = rc.senseTerrainTile(spawnLoc);
			if (tile == TerrainTile.OFF_MAP || tile == TerrainTile.VOID) {
				continue;
			}
			if (rc.senseObjectAtLocation(spawnLoc) == null) {
				rc.spawn(dir[spawnlist[i]]);
				return;
			}
		}
	}
	
	/* Attempts to attack nearby enemies */
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
	
	/* Do calculations with leftover bytecode */
	private void calculate() {
		while (Clock.getBytecodesLeft() > 1000) {

		}
	}

}
