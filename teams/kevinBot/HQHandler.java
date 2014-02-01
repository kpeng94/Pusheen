package kevinBot;

import battlecode.common.*;

/**
 * Broadcast system:
 * 100 - 199: Broadcasts by our robots (round numbers + location)
 * 11000 - 11099: Locations that are good for building PASTRs based on order
 * 11100 - 11199: Memory, where we see the enemy units at
 * 11300 - 11399: Where our robots are located (11300 + robot_id as offset)
 * 
 * 12000 - 120xx: surround locations
 * 12100 - 121xx: whether the surround locations are occupied
 * 
 * 12300: How many robots we have (controlled by the HQ)
 * 12301 - 12303: Assuming we spawn at most 90 robots, we can encode the ids of all robots alive in these 3 numbers.
 * 		Description: For robots 1 to 30, the HQ will sense if they are alive or not, then sum (1 or 0) * 2^robot_id 
 * 					 and store that as an integer in channel 30001.
 * 					 The HQ will do similarly for 31 - 60 (channel 30002) and 61 - 90 (channel 30003).
 * 12304 - 12310: What the HQ will consider to be "safe" locations.
 * 
 * 12311: # of pastrs
 * 12312: # of NTs
 * 12313: # of enemies
 * 
 * 13000 - 13009: Squadron leaders
 * 13010 - 13019: Squadron locations roughly (avg)
 * 13020 - 13029: Squadron army size
 * 13030 - 13039: Squadron target locations
 * 13040 - 13049: Squadron leader commands
 * 13050 - 13059: Latest addition to squadron
 * 
 * 13300 - 13399: Squadron number (for attackers and pastr builders)
 * 13400 - 13499: HQ designates what each robots goal is.
 * 		1: Attack
 * 		2: Get PASTR
 * 		3: Scout
 * 		4: Build PASTR
 * 		5: Build NT
 * 		6: Defend
 * 13500 - 13599: Robots know they have already been suicided on.
 * 13600 - 13699: Robots know they're being attacked.					
 * 13700 - 13799: Robots think that they're about to get suicided on.
 * 13800 - 13899: Robot types
 * 
 */

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static boolean diagonalSymmetry = true;
	private static boolean horizontalSymmetry = true;
	private static boolean verticalSymmetry = true;
	private static boolean calculatedSymmetry = false;
	static final MapLocation[] testLocations = {new MapLocation(1, 5), new MapLocation(9, 3), new MapLocation(8, 7), new MapLocation(8, 4),
												new MapLocation(0, 9), new MapLocation(5, 9), new MapLocation(2, 2), new MapLocation(2, 7),
												new MapLocation(9, 9), new MapLocation(7, 0), new MapLocation(5, 5), new MapLocation(5, 0)};
	// -o-ff-s-ets -t-op -r-ight -X-
	private static final int[] ostrX = {0, 1, 2, 3, 4, 5, 5, 5};
	private static final int[] ostrY = {-5, -5, -5, -5, -4, -3, -2, -1};
	private static final int[] ostlX = {-5, -5, -5, -5, -4, -3, -2, -1};
	private static final int[] ostlY = {0, -1, -2, -3, -4, -5, -5, -5};
	private static final int[] osblX = {0, -1, -2, -3, -4, -5, -5, -5};
	private static final int[] osblY = {5, 5, 5, 5, 4, 3, 2, 1};
	private static final int[] osbrX = {5, 5, 5, 5, 4, 3, 2, 1};
	private static final int[] osbrY = {0, 1, 2, 3, 4, 5, 5, 5};
	private static final int[] ostrXR = {5, 5, 5, 4, 3, 2, 1, 0};
	private static final int[] ostrYR = {-1, -2, -3, -4, -5, -5, -5, -5};
	private static final int[] ostlXR = {-1, -2, -3, -4, -5, -5, -5, -5};
	private static final int[] ostlYR = {-5, -5, -5, -4, -3, -2, -1, -0};
	private static final int[] osblXR = {-5, -5, -5, -4, -3, -2, -1, -0};
	private static final int[] osblYR = {1, 2, 3, 4, 5, 5, 5, 5};
	private static final int[] osbrXR = {1, 2, 3, 4, 5, 5, 5, 5};
	private static final int[] osbrYR = {5, 5, 5, 4, 3, 2, 1, 0};
	private static int[] squadronOne;
	private static int[] squadronTwo;
	private static int[] squadronThree;
	private static int[] squadronFour;
	private static int robotIDs = 0;
	
	MapLocation myHQLoc, enemyHQLoc;
	boolean curAttack;
	int[] spawnlist;
	int numberOfRobots = 0;
	int numberOfNoiseTowers = 0;
	int[] robotsAlive = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	
	public HQHandler(RobotController rcin) throws GameActionException {
		super(rcin);
		myHQLoc = rc.senseHQLocation();
		enemyHQLoc = rc.senseEnemyHQLocation();
		getSpawn();
		CowMap.init(rc);
		calculateSurround();
	}

	/* Generates spawn list order */
	private void getSpawn() {
		spawnlist = new int[8];
		
		int fromEnemy = myHQLoc.directionTo(enemyHQLoc).ordinal();
		int[] rot = new int[] {4, -3, 3, -2, 2, -1, 1, 0};
		for (int i = 8; i-- > 0;) {
			spawnlist[i] = (fromEnemy + rot[i] + 8) % 8;
		}
	}
	
	@Override
	public void execute() throws GameActionException {
		super.execute();
		curAttack = false;
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
			MapLocation spawnLoc = myHQLoc.add(dir[spawnlist[i]]);
			TerrainTile tile = rc.senseTerrainTile(spawnLoc);
			if (tile == TerrainTile.OFF_MAP || tile == TerrainTile.VOID) {
				continue;
			}
			if (rc.senseObjectAtLocation(spawnLoc) == null) {
				rc.spawn(dir[spawnlist[i]]);
				/**
				 * Update number of robots in bc system. Update which robots are alive in bc system.
				 * Update number of robots internally. Update which robots are alive internally.
				 * Update squadron for newly spawned robot. 
				 * Update mission for newly spawned robot.
				 */
				
//				TODO: UPDATE SQUADRON NUMBERS LATER
//				Currently all squad 1, attackers
				if (Clock.getRoundNum() == 0) {
					rc.broadcast(13301 + rc.readBroadcast(0), 1);
					rc.broadcast(13401 + rc.readBroadcast(0), 1);
					rc.broadcast(13020 + 1, rc.readBroadcast(13020 + 1) + 1);
					rc.broadcast(13001, 1);
				} else {
					rc.broadcast(13300 + rc.readBroadcast(0), 1);
					rc.broadcast(13400 + rc.readBroadcast(0), 1);					
					rc.broadcast(13020 + 1, rc.readBroadcast(13020 + 1) + 1);
				}
				updateLiveRobots();
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
				loc = new MapLocation(loc.x - Integer.signum(loc.x - myHQLoc.x), loc.y - Integer.signum(loc.y - myHQLoc.y));
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
	}
	
	private boolean in(MapLocation mapLocation, MapLocation[] mapLocations) {
		for (int i = mapLocations.length; i-- > 0;) {
			if (mapLocation.distanceSquaredTo(mapLocations[i]) == 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Updates internally for the HQ and broadcasts to channels 30001 - 30003 
	 * to maintain which robots are alive.
	 * 
	 * Rough bytecode cost:
	 */
	private void updateLiveRobots() throws GameActionException {
		if (Clock.getRoundNum() > 0) {
			int firstSet = 0;
			int secondSet = 0;
			int thirdSet = 0;
			int aliveCount = 0;
			
			// Go only up to RobotIDs
			int bc = rc.readBroadcast(0);
			for (int i = 30; i-- > 0;) {
				firstSet <<= 1;
				int bc2 = (rc.readBroadcast(101 + i)) & 2047;
				if (bc2 == Clock.getRoundNum() - 1) {
					firstSet++;
					robotsAlive[aliveCount] = i;
					aliveCount++;
				} else if (bc2 > 0) {
					// Robot has died. Get Squadron number
					// Shouldn't happen too often.
					int sqnum = rc.readBroadcast(13301 + i);
					rc.broadcast(13020 + sqnum, (rc.readBroadcast(13020 + sqnum)) - 1);
				} 
			}
			rc.broadcast(12301, firstSet);		
			if (bc > 30) {
				for (int i = 30; i-- > 0;) {
					secondSet <<= 1;
					int bc2 = rc.readBroadcast(131 + i) & 2047;
					if (bc2 == Clock.getRoundNum() - 1) {
						firstSet++;
						robotsAlive[aliveCount] = i;
						aliveCount++;						
					} else if (bc2 > 0) {
						// Robot has died. Get Squadron number
						int sqnum = rc.readBroadcast(13331 + i);
						rc.broadcast(13020 + sqnum, (rc.readBroadcast(13020 + sqnum)) - 1);
					} 
				}
				rc.broadcast(12302, secondSet);				
				if (bc > 60) {
					for (int i = 30; i-- > 0;) {
						thirdSet <<= 1;
						int bc2 = rc.readBroadcast(131 + i) & 2047;
						if (bc2 == Clock.getRoundNum() - 1) {
							firstSet++;
							robotsAlive[aliveCount] = i;
							aliveCount++;						
						} else if (bc2 > 0) {
							// Robot has died. Get Squadron number
							int sqnum = rc.readBroadcast(13361 + i);
							rc.broadcast(13020 + sqnum, (rc.readBroadcast(13020 + sqnum)) - 1);
						} 
					}
					rc.broadcast(12303, thirdSet);								
				}
			} 
			for (int i = aliveCount; i < 25; i++) {
				robotsAlive[aliveCount] = -1;
			}			
		}
	}
	
	/**
	 * Attempts to figure out the symmetries using the test locations.
	 */
	private void figureOutSymmetry() {
		for (int i = testLocations.length; i-- > 0; ) {
			MapLocation testLocation = testLocations[i];
			TerrainTile tt = rc.senseTerrainTile(testLocation);
			double[][] cowGrowths = rc.senseCowGrowth(); // Change later
			double cgtl = cowGrowths[testLocation.x][testLocation.y];
			if (diagonalSymmetry) {
				if (rc.senseTerrainTile(new MapLocation(rc.getMapWidth() - testLocation.x, rc.getMapHeight() - testLocation.y)) != tt ||
 				    cgtl != cowGrowths[rc.getMapWidth() - testLocation.x][rc.getMapHeight() - testLocation.y]) {
					diagonalSymmetry = false;
				}
			} 
			if (horizontalSymmetry) {
				if (rc.senseTerrainTile(new MapLocation(rc.getMapWidth() - testLocation.x, testLocation.y)) != tt ||
				    cgtl != cowGrowths[rc.getMapWidth() - testLocation.x][testLocation.y]) {
					horizontalSymmetry = false;
				}
			}
			if (verticalSymmetry) { 
				if (rc.senseTerrainTile(new MapLocation(testLocation.x, rc.getMapHeight() - testLocation.y)) != tt ||
				 	cgtl != cowGrowths[testLocation.x][rc.getMapHeight() - testLocation.y]) {
			 		verticalSymmetry = false;
				}
			}
			
			if ((!diagonalSymmetry && !horizontalSymmetry) || 
				(!diagonalSymmetry && !verticalSymmetry) ||
				(!verticalSymmetry && !horizontalSymmetry)) {
				calculatedSymmetry = true;
				break;
			}
		}
	}
	
	/**
	 * Calculates the important positions that need to be surrounded if we're using a surround strategy.
	 */
	private void calculateSurround() throws GameActionException {
		boolean ignoreLeft = enemyHQLoc.x <= 10;
		boolean ignoreRight = rc.getMapWidth() - enemyHQLoc.x <= 10;
		boolean ignoreTop = enemyHQLoc.y <= 10;
		boolean ignoreBottom = rc.getMapWidth() - enemyHQLoc.y <= 10;
		int count = 0;
		broadcastSurroundLocations(!ignoreTop, !ignoreRight, !ignoreBottom, !ignoreLeft, true);
	}

	private void squadronSplit() {
		
	}
	
	private void updateSquadrons() throws GameActionException {
		int sqoc= 0;
		squadronOne = new int[25];
		squadronTwo = new int[25];
		squadronThree = new int[25];
		squadronFour = new int[25];
		for (int i = rc.readBroadcast(0); i-- > 0;) {
			if (rc.readBroadcast(101 + i) == Clock.getRoundNum() - 1) {
				if (rc.readBroadcast(13301 + i) == 1) {
					squadronOne[sqoc] = i;
					sqoc++;
				}
			}
		}
	}
	
	private void broadcastSurroundLocations(boolean top, boolean right, boolean bottom, boolean left, 
											boolean clockwise) throws GameActionException {
		int count = 0;
		if (clockwise) {
			if (top) {
				if (right) {
					for (int os = 8; os-- > 0; ) {
						if (count >= 15) {
							break;
						}
						MapLocation ml = enemyHQLoc.add(ostrX[os], ostrY[os]);
						if (rc.senseTerrainTile(ml) != TerrainTile.VOID) {
							rc.broadcast(12000 + count, ml.x * 100 + ml.y);
							count++;
						}
					}
				}
				if (left) {
					for (int os = 8; os-- > 0; ) {
						if (count >= 15) {
							break;
						}
						MapLocation ml = enemyHQLoc.add(ostlX[os], ostlY[os]);
						if (rc.senseTerrainTile(ml) != TerrainTile.VOID) {
							rc.broadcast(12000 + count, ml.x * 100 + ml.y);
							count++;
						}
					}
				}
			}
			if (bottom) {
				if (left) {
					for (int os = 8; os-- > 0; ) {
						if (count >= 15) {
							break;
						}
						MapLocation ml = enemyHQLoc.add(osblX[os], osblY[os]);
						if (rc.senseTerrainTile(ml) != TerrainTile.VOID) {
							rc.broadcast(12000 + count, ml.x * 100 + ml.y);
							count++;
						}
					}
				}
				if (right) {				
					for (int os = 8; os-- > 0; ) {
						if (count >= 15) {
							break;
						}
						MapLocation ml = enemyHQLoc.add(osbrX[os], osbrY[os]);
						if (rc.senseTerrainTile(ml) != TerrainTile.VOID) {
							rc.broadcast(12000 + count, ml.x * 100 + ml.y);
							count++;
						}
					}
				}
			}
		} else {
			
		}
	} 
}


 
