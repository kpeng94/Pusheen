package seedingTourneyBotv2;

import battlecode.common.*;

public class NoisetowerHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int lastNoise;
	
	public NoisetowerHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		super.execute();
		runNoise();
	}

	private void runNoise() throws GameActionException{
		// TODO Auto-generated method stub
		MapLocation HQLoc = rc.senseHQLocation();
		if (lastNoise < 8)
			lastNoise = (lastNoise + 1) % 8 + 8*4 + 40;
		else if (lastNoise >= 40)
			lastNoise -= 40;
		else
			lastNoise += 32;

		MapLocation atk;
		if (lastNoise % 2 == 0)
			atk = HQLoc.add(dir[lastNoise % 8], 5 + 3*((lastNoise / 8) % 5));
		else
			atk = HQLoc.add(dir[lastNoise % 8], 4 + 2*((lastNoise / 8) % 5));
		rc.attackSquare(atk);
		
	
	}

}
