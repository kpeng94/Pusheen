package seedingTourneyBotv6;

import battlecode.common.*;

/**
 * 
 * @author Rene
 *
 * Abstract class for handling units
 *
 */
public abstract class UnitHandler {
	protected Rand rand; // Random number generator
	protected RobotController rc; // Robot Controller
	protected int id;
	protected int round;
	
	/* Initializes information during spawn */
	public UnitHandler(RobotController rcin) {
		rc = rcin;
		rand = new Rand(rc.getRobot().getID());
		try {
			id = rc.readBroadcast(0);
			rc.broadcast(0, id+1);
		}
		catch (Exception e) {
//			e.printStackTrace();
			System.out.println("Initialization Exception");
		}
	}
	
	/* Executes every round */
	public void execute() throws GameActionException {
		if (id != 0) {
			broadcastSelf();
		}
	}
	
	/* Broadcasts unit's own position and round number */
	private void broadcastSelf() throws GameActionException {
		round = Clock.getRoundNum();
		MapLocation curLoc = rc.getLocation();
		int intLoc = (curLoc.x << 7) + curLoc.y;
		rc.broadcast(100 * id, (intLoc << 11) + round);
	}
	
}
