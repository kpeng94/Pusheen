package kevinBot;

import battlecode.common.*;

/**
 * Strategy:
 * 
 * Things needed in broadcast system:
 * Delegating squadrons (SUPER IMPORTANT if we want to WIN)
 * 
 * Broadcast system:
 * 20000 - 29999: Locations
 *
 * 20000 - 20099: Locations that are good for building PASTRs based on order
 * 20100 - 20199: Locations that we need to cover to blockade the enemy HQ
 * 20200 - 20299: Memory, where we see the enemy units at
 *
 *
 * 24900 - 24999: Where our robots are located (24900 + robot_id as offset)
 * 
 * 25999: length of surround locations
 * 26000 - 260xx: surround locations
 * 27000 - 270xx: whether the surround locations are occupied
 * 28000 - 280xx: whether the surround locations are claimed?
 * 
 * 30000 - 39999: Logistic Information 
 * 
 * 30000: How many robots we have (controlled by the HQ)
 * 30001 - 30003: Assuming we spawn at most 90 robots, we can encode the ids of all robots alive in these 3 numbers.
 * 		Description: For robots 1 to 30, the HQ will sense if they are alive or not, then sum (1 or 0) * 2^robot_id 
 * 					 and store that as an integer in channel 30001.
 * 					 The HQ will do similarly for 31 - 60 (channel 30002) and 61 - 90 (channel 30003).
 * 30004 - 30010: What the HQ will consider to be "safe" locations.
 * 
 * 31000: # of pastrs
 * 31001: # of NTs
 * 
 * 35000 - 35009: Squadron leaders
 * 35010 - 35019: Squadron locations roughly
 * 35020 - 35029: Squadron army size
 * 
 * 
 * 39300 - 39399: Squadron number (for attackers and pastr builders)
 * 39400 - 39499: HQ designates what each robots goal is.
 * 		1: Attack
 * 		2: Get PASTR
 * 		3: Scout
 * 		4: Build PASTR
 * 		5: Build NT
 * 		6: Defend
 * 39500 - 39599: Robots know they have already been suicided on.
 * 39600 - 39699: Robots know they're being attacked.					
 * 39700 - 39799: Robots think that they're about to get suicided on.
 * 39800 - 39899: Robot types
 * 39900 - 39999: Round number broadcast by our robots
 * 
 * 
 * 40000 - 40100: Target points around an enemy PASTR that our bots have already focused.
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
	
	MapLocation myHQLoc, enemyHQLoc;
	boolean curAttack;
	int[] spawnlist;
	int numberOfRobots = 0;
	int numberOfNoiseTowers = 0;

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
		int firstSet = 0;
		int secondSet = 0;
		int thirdSet = 0;
		for (int i = 30; i-- > 0;) {
			firstSet <<= 1;
			if (rc.readBroadcast(39900 + i) == Clock.getRoundNum() - 1) {
				firstSet++;
			}
		}
		rc.broadcast(30001, firstSet);
		for (int i = 30; i-- > 0;) {
			secondSet <<= 1;
			if (rc.readBroadcast(39930 + i) == Clock.getRoundNum() - 1) {
				secondSet++;
			}
		}
		rc.broadcast(30002, secondSet);
		for (int i = 30; i-- > 0;) {
			thirdSet <<= 1;
			if (rc.readBroadcast(39960 + i) == Clock.getRoundNum() - 1) {
				thirdSet++;
			}
		}
		rc.broadcast(30003, thirdSet);
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
							rc.broadcast(26000 + count, ml.x * 100 + ml.y);
							rc.setIndicatorString(0, "I HAVE BROACASTED THIS");
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
							rc.broadcast(26000 + count, ml.x * 100 + ml.y);
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
							rc.broadcast(26000 + count, ml.x * 100 + ml.y);
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
							rc.broadcast(26000 + count, ml.x * 100 + ml.y);
							count++;
						}
					}
				}
			}
		} else {
			
		}
	} 
}


 
