package seedingTourneyBotv1;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	MapLocation ourLoc;
	MapLocation enemyLoc;
	int[] spawnlist;
	int numberOfRobots = 0;
	int numberOfNoiseTowers = 0;

	public HQHandler(RobotController rcin) {
		super(rcin);
		ourLoc = rc.senseHQLocation();
		enemyLoc = rc.senseEnemyHQLocation();
		getSpawn();
		CowMap.init(rc);
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
		
//	  if (rc.readBroadcast(15000) != Clock.getRoundNum() - 1) {
//		  System.out.println("broadcast read: " + rc.readBroadcast(15000));
//		  System.out.println("round number: " + Clock.getRoundNum());
//	    rc.broadcast(30000, 0);
//	  } else if (rc.readBroadcast(15001) != Clock.getRoundNum() - 1) {
//	    rc.broadcast(20000, 0);
//	  } else if (rc.readBroadcast(15002) != Clock.getRoundNum() - 1) {
//	    rc.broadcast(20001, 0);
//	  } else if (rc.readBroadcast(15003) != Clock.getRoundNum() - 1) {
//	    rc.broadcast(20002, 0);
//	  } else if (rc.readBroadcast(15004) != Clock.getRoundNum() - 1) {
//	    rc.broadcast(20003, 0);
//	  } else if (rc.readBroadcast(15005) != Clock.getRoundNum() - 1) {
//	  	rc.broadcast(20004, 0);
//	  }
	 
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
				numberOfRobots++;
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
	private void calculate() throws GameActionException {
		CowMap.calculate();
		calculateGoodPointsNearbyToGoTo();
	}
	
	/**
	 * 
	 * @throws GameActionException
	 */
	private void calculateGoodPointsNearbyToGoTo() throws GameActionException {
		MapLocation ml;
		switch (CowMap.bestLoc.directionTo(enemyLoc)) {
			case WEST:
				ml = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, 2);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, -2);
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, -1);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 3);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case NORTH:
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 2);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, -2);
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, -1);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case SOUTH:
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 2);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, -2);
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, -1);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case SOUTH_WEST:
				ml = CowMap.bestLoc.add(Direction.SOUTH, 4);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 4);				
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.WEST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.WEST, 3);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 2).add(Direction.WEST, 2);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case NORTH_WEST:
				ml = CowMap.bestLoc.add(Direction.NORTH, 4);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.WEST, 4);				
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.WEST, 3);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 2).add(Direction.WEST, 2);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case NORTH_EAST:
				ml = CowMap.bestLoc.add(Direction.NORTH, 4);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 4);				
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.EAST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.EAST, 3);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.NORTH, 2).add(Direction.EAST, 2);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case SOUTH_EAST:
				ml = CowMap.bestLoc.add(Direction.SOUTH, 4);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 4);				
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.EAST, 3);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.SOUTH, 2).add(Direction.EAST, 2);
				rc.broadcast(22000, ml.x * 100 + ml.y);
			case EAST:
			default: 
				ml = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, 2);
				rc.broadcast(22004, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, -2);
				rc.broadcast(22003, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, 1);
				rc.broadcast(22002, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, -1);
				rc.broadcast(22001, ml.x * 100 + ml.y);
				ml = CowMap.bestLoc.add(Direction.EAST, 3);
				rc.broadcast(22000, ml.x * 100 + ml.y);
		}
	}
}
