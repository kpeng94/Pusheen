package kevinBot;

import battlecode.common.*;

public class Strategy {
	private static RobotController rc;
	
	
	
	
	/**
	 * Initialize variables to be used for the strategy
	 * @param rcin input RobotController
	 */
	public static void init(RobotController rcin) {
		rc = rcin;
	}
	
	public static void chooseStrategy(int number) {
		
	}
	
	/**
	 * Strategy overview:
	 * 1. We choose to rush the enemy HQ. 
	 *    -Decision 1: how to rush? wait in a group or go forth immediately?
	 *    -Decision 2: how to micro? what happens if rush fails?
	 *    -Decision 3: send all of our units? or only part of them and have the others crowd?
	 * 2. After gaining a substantial advantage or a turn number X, we move to farm.
	 * 	  -Decision 1: how to check?
	 * 	  -Decision 2: how to farm? # of pastrs?
	 * 3. Defend
	 */
	public static void rushStrategy() {
		
	}
	

	/**
	 * Split farm strategy: 
	 * 1. Have multiple units go build noise towers in different locations.
	 * 2. Once the farm is good enough, rush to build PASTRs simultaneously in all locations.
	 */
	public static void splitFarmStrategy() {
		
	}
	
	/**
	 * Micro fight strategy:
	 * 1. Same as rush, except we win through a micro fight.
	 */
	
	/**
	 * Micro fight strategy v2:
	 * 1. Retreat and fall back to win fights
	 */
	
	/**
	 * Surround next potential pastr strategy
	 */
}
