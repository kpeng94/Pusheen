package kevinBot;

import battlecode.common.*;

public class PastrHandler extends UnitHandler {

	public PastrHandler(RobotController rcin) {
		super(rcin);
	}

	@Override
	public void execute() throws GameActionException{
		super.execute();
		rc.broadcast(15000, Clock.getRoundNum());
	}
	
	/**
	 * There's no point in not having the PASTR run extra calculations (as long as we consider 
	 * them to be add-ons), since the PASTR location is revealed anyways.
	 */
	public void calculate() {
		
	}

}
