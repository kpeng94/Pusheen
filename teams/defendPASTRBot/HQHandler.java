package defendPASTRBot;

import battlecode.common.*;

public class HQHandler extends UnitHandler {
	static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static MapLocation myLocation;

	public HQHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException {
		myLocation = rc.getLocation();
		spawnUnits();
		attack();
	}
	
	public void spawnUnits() throws GameActionException {
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			Direction spawnDir = directions[rand.nextAnd(7)];
			if (rc.senseObjectAtLocation(myLocation.add(spawnDir)) == null) {
				rc.spawn(spawnDir);				
			}
		}
		
	}
	
	public void attack() throws GameActionException {
		attack : {
			Robot[] enemy = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
			for (int i = enemy.length; i-- > 0;) {
				MapLocation loc = rc.senseRobotInfo(enemy[i]).location;
				if (rc.canAttackSquare(loc)) {
					rc.attackSquare(loc);
					break attack;
				}
				else {
					loc = new MapLocation(loc.x - Integer.signum(loc.x - myLocation.x), 
										  loc.y - Integer.signum(loc.y - myLocation.y));
					if (rc.canAttackSquare(loc)) {
						rc.attackSquare(loc);
						break attack;
					}
				}
			}
		}		
	}
}
