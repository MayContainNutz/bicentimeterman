package TheBicentimeterManV01;
import battlecode.common.*;


public strictfp class RobotPlayer {
    static RobotController rc;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

    	
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

    	
        
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
        
        
    }
    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");
        int numGardners = 0;
        int deisredNumGardners = 7;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	//victory point check for auto win
            	GameConstants gc = null;
            	float wincond = gc.VICTORY_POINTS_TO_WIN * gc.BULLET_EXCHANGE_RATE;
            	if (rc.getTeamBullets()>= wincond)
            	{
            		rc.donate(wincond);
            	}
            	if (rc.getRoundNum() > rc.getRoundLimit()-2 )//check for last round 1 and 0 indexed :/
            	{

            		rc.donate(rc.getTeamBullets());//on the last round, donates all our bullets
            	}
            	
                // Generate a random direction
                Direction dir = randomDirection();

                
                //build gardeners

                if (rc.canHireGardener(dir) && (numGardners < deisredNumGardners)) {
                    rc.hireGardener(dir);
                    numGardners++;
                    System.out.println("I'm an archon!" + numGardners + " "+ deisredNumGardners);
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        //System.out.println("I'm a gardener!");
    	Team enemy = rc.getTeam().opponent();
    	Team friendly =rc.getTeam();
        RobotInfo myArchon = null;
        MapLocation ArchonStartingLoc = null;
        boolean canBuildHere = false;
        boolean builtLumberjack = false;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

            	//first thing to do is build 1 lumberjack per gardner, to start clearing space
            	if (builtLumberjack == false)
            	{
            		Direction randomDir = randomDirection();
            		if(rc.canBuildRobot(RobotType.LUMBERJACK, randomDir))
            		{
            			rc.buildRobot(RobotType.LUMBERJACK, randomDir);
            			builtLumberjack = true;
            		}
            	}

            	MapLocation myLocation = rc.getLocation();
                // Listen for home archon's location
                //int xPos = rc.readBroadcast(0);
                //int yPos = rc.readBroadcast(1);
                //MapLocation archonLoc = new MapLocation(xPos,yPos);
                //check for nearby trees that need watering
                TreeInfo[] trees = rc.senseNearbyTrees(-1,friendly);
                
                if(trees.length > 0)//if there are any trees
                {
                	float weakestTree = trees[0].maxHealth;
                	int waterMe = -1;
                	for(int i = 0; i < trees.length; i++)//for each tree
                	{
                		if (trees[i].health < weakestTree )//if it has the lowest health yet
                		{
                			if (rc.canWater(trees[i].ID))
                			{
                				weakestTree = trees[i].health;//record lowest health
                				waterMe = i;//record its index
                			}
                		}
                	}
                	if (waterMe != -1)//if we chose a tree with < max health
                	{
                		rc.water(trees[waterMe].ID);// water it
                	}
                }
                                
                // Generate a random direction
                Direction dir = randomDirection();
                
                //MapLocation destination = rc.getLocation();
                
                
                //find my archon

                RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, friendly);
                if (friendlyRobots.length > 0)
                {
                	for (int i=0;i<friendlyRobots.length;i++)//for each robot in sensor range
                	{
                		//find the nearest archon
                		if (friendlyRobots[i].type == RobotType.ARCHON)
                		{
                			myArchon = friendlyRobots[i];
                		}
                	}
                }
                //archon starting location is my staring point
                if (ArchonStartingLoc == null)
                {
	                if (! (myArchon == null))
	                {
	                	ArchonStartingLoc = myArchon.getLocation();
	                } else
	                {
	                	ArchonStartingLoc = rc.getLocation();
	                }
                }
                
                Direction[] buildDirections = new Direction[6];
                
                Direction buildDirection =  Direction.getNorth();
                for (int i = 0; i<6 ;i++)//6 spots
                {
                	//check that its a valid tree spot?
                	rc.setIndicatorDot(rc.getLocation().add(buildDirection, 3), 255, 255, 255);
                	if (!(rc.isCircleOccupied(rc.getLocation().add(buildDirection, 3), 1)))
                	{
                		canBuildHere = true;//we can build here, continue
                		buildDirections[i] = buildDirection;
                    	buildDirection = buildDirection.rotateLeftDegrees(60);//60 degrees apart
                	}else if (rc.canInteractWithTree(rc.getLocation().add(buildDirection, 2)))
                	{
                		canBuildHere = true;//we can build here, continue
                		buildDirections[i] = buildDirection;
                    	buildDirection = buildDirection.rotateLeftDegrees(60);//60 degrees apart
                	}else
                	{
                		canBuildHere = false;//we CANT build here
                		break;//and break loop so a valid spot doesnt flag the whole area as plantable
                	}
                	
                }
                //RobotInfo[] hotBotsInMyArea = rc.senseNearbyRobots(7, friendly);//i dont want my tree farms to clog up the whole map
                //move a little away from the archon
                if(rc.getLocation().distanceTo(ArchonStartingLoc) < 10 || !(friendlyRobots.length <= 1) ||  !canBuildHere)
                {
                	//move away from arhcon and from other gardners
                	if(friendlyRobots.length>0)
                	{
                		tryMove(awayFromTarget(rc.getLocation(),friendlyRobots[0].getLocation()));
                	}else if (!(tryMove(awayFromTarget(rc.getLocation(),ArchonStartingLoc))))
                	{
                		tryMove(randomDirection());
                	}
                	canBuildHere = false;
                }else
                {
                	//build 5 trees around me, then pump out combat units
                	for(int i = 0;i<5;i++)
                	{
                		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                			
                     	if ( rc.canPlantTree(buildDirections[i]))
                        {
                     		rc.plantTree(buildDirections[i]);
                        }

                     	
                     	if(i==5)
                     	{
                     		//all my eco should be up so
                     		//build combat bots
                     		//TODO build appropriate combat bots
                     		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                     		//if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDirection) && rc.isBuildReady()) {
                               // rc.buildRobot(RobotType.LUMBERJACK, buildDirection);
                     		//}
                     	}
                	}

                }
                
                // Move randomly
                //tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }


	static void runSoldier() throws GameActionException {
        //System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        //System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // no Robots in sight, check for trees
                        TreeInfo[] trees = rc.senseNearbyTrees(-1);
                        if(trees.length > 0 && !rc.hasAttacked())
                        {
                        	//if there are trees
                        	//if()
                        	{
                        		
                        	}
                        }
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    private static Direction awayFromTarget(MapLocation myLoc, MapLocation targetLoc) {
    	Direction myArchon = new Direction(myLoc,targetLoc);
    	return myArchon.opposite();
	}
}
