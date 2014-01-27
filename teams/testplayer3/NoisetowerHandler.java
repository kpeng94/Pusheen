package testplayer3;

import battlecode.common.*;

public class NoisetowerHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int lastNoise;
	static MapLocation myLoc;
	
	public NoisetowerHandler(RobotController rcin) {
		super(rcin);
		myLoc = rc.getLocation();
	}

	@Override
	public void execute() throws GameActionException{
		super.execute();
		
		rc.broadcast(15006, Clock.getRoundNum());
		rc.broadcast(15007, Clock.getRoundNum());
		if (rc.isActive()) {
			runNoise();
		}
	}

	private void runNoise() throws GameActionException{
		if (lastNoise < 8)
			lastNoise = (lastNoise + 1) % 8 + 8*4 + 40;
		else if (lastNoise >= 40)
			lastNoise -= 40;
		else
			lastNoise += 32;

		MapLocation atk;
		if (lastNoise % 2 == 0)
			atk = myLoc.add(dir[lastNoise % 8], 4 + 3*((lastNoise / 8) % 5));
		else
			atk = myLoc.add(dir[lastNoise % 8], 3 + 2*((lastNoise / 8) % 5));
		rc.attackSquare(atk);
		
	}

}
