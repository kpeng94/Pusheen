package suicideMicroBot;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	public HQHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		MapLocation myLoc = rc.getLocation();
		if (Clock.getRoundNum() > 100 && rc.isActive() && rc.senseRobotCount() < 25) {
			Direction spawnDir = dir[rand.nextAnd(7)];
			if (rc.senseObjectAtLocation(myLoc.add(spawnDir)) == null)
				rc.spawn(spawnDir);
		}
	}

}
