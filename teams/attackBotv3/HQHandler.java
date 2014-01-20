package attackBotv3;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] dir = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static MapLocation curLoc;
	
	public HQHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		curLoc = rc.getLocation();
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			Direction spawnDir = dir[rand.nextAnd(7)];
			if (rc.senseObjectAtLocation(curLoc.add(spawnDir)) == null)
				rc.spawn(spawnDir);
		}
		// Attack nearby enemies
		attack : {
			Robot[] enemy = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
			for (int i = enemy.length; i-- > 0;) {
				MapLocation loc = rc.senseRobotInfo(enemy[i]).location;
				if (rc.canAttackSquare(loc)) {
					rc.attackSquare(loc);
					break attack;
				}
				else {
					loc = new MapLocation(loc.x - Integer.signum(loc.x - curLoc.x), loc.y - Integer.signum(loc.y - curLoc.y));
					if (rc.canAttackSquare(loc)) {
						rc.attackSquare(loc);
						break attack;
					}
				}
			}
		}
		// Otherwise, spend all most remaining bytecodes left on calculations
		while (Clock.getBytecodesLeft() > 1000) {

		}
	}

}
