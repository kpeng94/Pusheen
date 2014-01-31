package genericplayer;

import battlecode.common.*;

public class MathUtils {
	
	public static double TAN_TWENTY_TWO = 0.414214;
	public static double TAN_SIXTY_SEVEN = 2.41421;
	public static double NEG_TAN_TWENTY_TWO = -0.414214;
	public static double NEG_TAN_SIXTY_SEVEN = -2.41421;
	
	
	public static Direction getClosestDirection(double d, double e){
		if(d == 0){
			if(e > 0){
				return Direction.NORTH;
			}
			else if(e < 0){
				return Direction.SOUTH;
			}
			return Direction.NONE;
		}
		
		double tangent = e/d;
		
		if(d > 0){
			if(e > 0){
				if(tangent < TAN_TWENTY_TWO){
					return Direction.EAST;
				}
				else if(tangent > TAN_SIXTY_SEVEN){
					return Direction.NORTH;
				}
				return Direction.NORTH_EAST;
			}
			else{
				if(tangent > NEG_TAN_TWENTY_TWO){
					return Direction.EAST;
				}
				else if(tangent < NEG_TAN_SIXTY_SEVEN){
					return Direction.SOUTH;
				}
				return Direction.SOUTH_EAST;
			}
		}
		else{
			if(e > 0){
				if(tangent > NEG_TAN_TWENTY_TWO){
					return Direction.WEST;
				}
				else if(tangent < NEG_TAN_SIXTY_SEVEN){
					return Direction.NORTH;
				}
				return Direction.NORTH_WEST;
			}
			else{
				if(tangent < TAN_TWENTY_TWO){
					return Direction.WEST;
				}
				else if(tangent > TAN_SIXTY_SEVEN){
					return Direction.SOUTH;
				}
				return Direction.SOUTH_WEST;
			}
		}
	}
	/* Debugging purposes
	public static void main(String[] args){
		System.out.println(MathUtils.getClosestDirection(-5, -25));
	}
	 */
}
