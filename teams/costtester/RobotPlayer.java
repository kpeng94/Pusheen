package costtester;

import battlecode.common.*;

public class RobotPlayer {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	static int[][] data;
	
	public static void run(RobotController rcin) {
		data = new int[10][10];
		int start = Clock.getBytecodeNum();
		System.out.println(data[5][5]);
		System.out.println(Clock.getBytecodeNum() - start);
	}

}