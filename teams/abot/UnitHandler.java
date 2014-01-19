package abot;

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
	public abstract void execute() throws GameActionException;
	
}
