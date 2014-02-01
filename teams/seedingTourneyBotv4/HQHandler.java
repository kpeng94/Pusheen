package seedingTourneyBotv4;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	MapLocation ourLoc;
	MapLocation enemyLoc;
	boolean curAttack;
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
		curAttack = false;
	
		if (rc.readBroadcast(51000)==0){
			//danger is cleared
			rc.broadcast(50000, 0);
		} else {
			//reset
			rc.broadcast(51000, 0);
		}
		if (rc.readBroadcast(15000) != Clock.getRoundNum() - 1) {
			rc.broadcast(30000, 0);
		} else if (rc.readBroadcast(15001) != Clock.getRoundNum() - 1) {
			rc.broadcast(20000, 0);
			rc.broadcast(21000, 0);	    
		} else if (rc.readBroadcast(15002) != Clock.getRoundNum() - 1) {
			rc.broadcast(20001, 0);
			rc.broadcast(21001, 0);	    
		} else if (rc.readBroadcast(15003) != Clock.getRoundNum() - 1) {
			rc.broadcast(20002, 0);
			rc.broadcast(21002, 0);	    
		} else if (rc.readBroadcast(15004) != Clock.getRoundNum() - 1) {
			rc.broadcast(20003, 0);
			rc.broadcast(21003, 0);	    
		} else if (rc.readBroadcast(15005) != Clock.getRoundNum() - 1) {
			rc.broadcast(20004, 0);
			rc.broadcast(21004, 0);	    
		} else if (rc.readBroadcast(15006) != Clock.getRoundNum() - 1) {
			rc.broadcast(30001, 0);
		}

		if (rc.isActive()) {
			tryAttack();
			if (!curAttack && rc.senseRobotCount() < 25) {
				trySpawn();
			}
		}
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
				curAttack = true;
				rc.attackSquare(loc);
				return;
			}
			else {
				loc = new MapLocation(loc.x - Integer.signum(loc.x - ourLoc.x), loc.y - Integer.signum(loc.y - ourLoc.y));
				if (rc.canAttackSquare(loc)) {
					curAttack = true;
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
		MapLocation [] ml = new MapLocation[5];
		if (CowMap.bestLoc == null) {
			return;
		}
		switch (CowMap.bestLoc.directionTo(enemyLoc)) {
			case WEST:
				ml[0] = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, 2);
				ml[1] = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, -2);
				ml[2] = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, 1);
				ml[3] = CowMap.bestLoc.add(Direction.WEST, 3).add(Direction.SOUTH, -1);
				ml[4] = CowMap.bestLoc.add(Direction.WEST, 3);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.WEST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.EAST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case NORTH:
				ml[0] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 2);
				ml[1] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, -2);
				ml[2] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, -1);
				ml[4] = CowMap.bestLoc.add(Direction.NORTH, 3);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.NORTH, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.SOUTH, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case SOUTH:
				ml[0] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 2);
				ml[1] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, -2);
				ml[2] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, -1);
				ml[4] = CowMap.bestLoc.add(Direction.SOUTH, 3);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.SOUTH, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.NORTH, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case SOUTH_WEST:
				ml[0] = CowMap.bestLoc.add(Direction.SOUTH, 4);
				ml[1] = CowMap.bestLoc.add(Direction.WEST, 4);				
				ml[2] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.WEST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.WEST, 3);
				ml[4] = CowMap.bestLoc.add(Direction.SOUTH, 2).add(Direction.WEST, 2);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.SOUTH_WEST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.EAST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case NORTH_WEST:
				ml[0] = CowMap.bestLoc.add(Direction.NORTH, 4);
				ml[1] = CowMap.bestLoc.add(Direction.WEST, 4);				
				ml[2] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.WEST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.WEST, 3);
				ml[4] = CowMap.bestLoc.add(Direction.NORTH, 2).add(Direction.WEST, 2);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.NORTH_WEST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.EAST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case NORTH_EAST:
				ml[0] = CowMap.bestLoc.add(Direction.NORTH, 4);
				ml[1] = CowMap.bestLoc.add(Direction.EAST, 4);				
				ml[2] = CowMap.bestLoc.add(Direction.NORTH, 3).add(Direction.EAST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.EAST, 3);
				ml[4] = CowMap.bestLoc.add(Direction.NORTH, 2).add(Direction.EAST, 2);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.NORTH_EAST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.WEST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case SOUTH_EAST:
				ml[0] = CowMap.bestLoc.add(Direction.SOUTH, 4);
				ml[1] = CowMap.bestLoc.add(Direction.EAST, 4);				
				ml[2] = CowMap.bestLoc.add(Direction.SOUTH, 3).add(Direction.EAST, 1);
				ml[3] = CowMap.bestLoc.add(Direction.SOUTH, 1).add(Direction.EAST, 3);
				ml[4] = CowMap.bestLoc.add(Direction.SOUTH, 2).add(Direction.EAST, 2);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.SOUTH_EAST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.NORTH, 1).add(Direction.WEST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
			case EAST:
			default: 
				ml[0] = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, 2);
				ml[1] = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, -2);
				ml[2] = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, 1);
				ml[3] = CowMap.bestLoc.add(Direction.EAST, 3).add(Direction.NORTH, -1);
				ml[4] = CowMap.bestLoc.add(Direction.EAST, 3);
				for (int i = 5; i-- > 0;) {
					TerrainTile tt = rc.senseTerrainTile(ml[i]);
					if (tt == TerrainTile.VOID || tt == TerrainTile.OFF_MAP) {
						for (int j = 5; j-- > 0;) {
							if (j == 2)
								continue;
							MapLocation ml2 = ml[0].add(Direction.EAST, j - 2);
							TerrainTile tt2 = rc.senseTerrainTile(ml2);
							if (tt2 != TerrainTile.VOID && tt2 != TerrainTile.OFF_MAP) {
								ml[i] = ml2;
								break;
							}
						}
					}
				}
//				ml = CowMap.bestLoc.add(Direction.WEST, 1);
//				rc.broadcast(22100, ml.x * 100 + ml.y);
				break;
		}
		rc.broadcast(22004, ml[0].x * 100 + ml[0].y);
		rc.broadcast(22003, ml[1].x * 100 + ml[1].y);
		rc.broadcast(22002, ml[2].x * 100 + ml[2].y);
		rc.broadcast(22001, ml[3].x * 100 + ml[3].y);
		rc.broadcast(22000, ml[4].x * 100 + ml[4].y);
	}
	
	private boolean in(MapLocation mapLocation, MapLocation[] mapLocations) {
		for (int i = mapLocations.length; i-- > 0;) {
			if (mapLocation.distanceSquaredTo(mapLocations[i]) == 0) {
				return true;
			}
		}
		return false;
	}
}


 
