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
            	MapLocation[] enemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
            	if (enemyArchons.length > 0)
            	{
            		dir = rc.getLocation().directionTo(enemyArchons[0]);
            	}
            	
            	if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(dir, 2), 2))
            	{
            		dir = randomDirection();
            	}
                

                
                //build gardeners
                
                if (rc.canHireGardener(dir) && (rc.getRobotCount() < deisredNumGardners*2)) {//*2 allows for lumberjacks+gardner combo
                    //if(rc.getTeamBullets() > RobotType.LUMBERJACK.bulletCost+RobotType.GARDENER.bulletCost)//meant to give a chance to make lumberjacks if we are hard up for cash
                    {
	                	rc.hireGardener(dir);
	                    numGardners++;
                    }
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
    	Team friendly = rc.getTeam();
        RobotInfo myArchon = null;
        MapLocation ArchonStartingLoc = null;
        boolean canBuildHere = false;
        boolean builtLumberjack = false;
        boolean begunBuilding = false;
        int pathfindingTurn = 0;
        Direction moveLastTurnDir = null;
        int howOldIAm = 0;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

            	//first thing to do is build 1 lumberjack per gardner, to start clearing space
            	if (builtLumberjack == false)
            	{
            		Direction randomDir = randomDirection();
            		if(rc.canBuildRobot(RobotType.SCOUT, randomDir))
            		{
            			rc.buildRobot(RobotType.SCOUT, randomDir);
            			builtLumberjack = true; //rename this bool if scouts are a better choice
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
                
                Direction buildDirection =  awayFromTarget(rc.getLocation(),ArchonStartingLoc);
                buildDirection = buildDirection.rotateLeftDegrees(60);//60 offcentre, so the combat units come out facing DIRECTLY away from the archon
                for (int i = 0; i<6 ;i++)//6 spots load up the tree matrix
                {
            		buildDirections[i] = buildDirection;
                	buildDirection = buildDirection.rotateLeftDegrees(60);//60 degrees apart
                }
                /*
                for (int i = 0; i<6 ;i++)//now we check that the spots are 'treeable'
                {
                	//check that its a valid tree spot?
                	//rc.setIndicatorDot(rc.getLocation().add(buildDirection, 3), 255, 255, 255);
                	if (!(rc.isCircleOccupied(rc.getLocation().add(buildDirection, 3), 1)))//  ||   rc.canInteractWithTree(rc.getLocation().add(buildDirection, 2))     )
                	{
                		canBuildHere = true;//we can build here, or there is already a tree here, continue

                	}else
                	{
                		canBuildHere = false;//we CANT build here
                		break;//and break loop so a valid spot doesnt flag the whole area as plantable
                	}
                }*/
                canBuildHere = true;
                if(begunBuilding)//if we started building here, then we already vetted the spot
                {
                	canBuildHere = true;//invalidates the checking above
                }else if (friendlyRobots.length > 0)//if we havent started building here, and the nearby robot might be the cause, 
                {
                	canBuildHere = false;
                }
                //RobotInfo[] hotBotsInMyArea = rc.senseNearbyRobots(7, friendly);//i dont want my tree farms to clog up the whole map
                
                //move a little away from the archon
                if(  !canBuildHere || howOldIAm < 2)        //   rc.getLocation().distanceTo(ArchonStartingLoc) < 8 ||
                {
                	if(!begunBuilding)//if we would normally move, but have already made trees, just wait this turn, and try again next turn
                	{
                		if (pathfindingTurn == 0)//3 turn 'memory' of what direction we are moving, helps to get around obsticles
                		{
		                	//move away from the edge of the map
		                	if (!(rc.onTheMap(rc.getLocation().add(Direction.getNorth(), 2))))//check for north edge
		        			{
		        				tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(), rc.getLocation().add(Direction.getNorth(),4)));
		        			}
		                	else if (!(rc.onTheMap(rc.getLocation().add(Direction.getEast(), 2))))//east edge(go west life is peaceful there)
		                	{
		                		tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(), rc.getLocation().add(Direction.getEast(),4)));
		                	}
		                	else if (!(rc.onTheMap(rc.getLocation().add(Direction.getSouth(), 2))))//south edge
		                	{
		                		tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(), rc.getLocation().add(Direction.getSouth(),4)));
		                	}
		                	else if (!(rc.onTheMap(rc.getLocation().add(Direction.getWest(), 2))))//west edge
		                	{
		                		tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(), rc.getLocation().add(Direction.getWest(),4)));
		                	}else if(friendlyRobots.length>0 && (rc.getLocation().distanceTo(ArchonStartingLoc)) > 1)//arbitary distances from starting archon for smoothest dispersion of econ
		                	{ //if far enough from the map edge, move away from arhcon and from other gardners
		                		tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(),friendlyRobots[0].getLocation()));
		                	}else if (!(tryMove(moveLastTurnDir = awayFromTarget(rc.getLocation(),ArchonStartingLoc))))
		                	{
		                		//tryMove(randomDirection());
		                	}
		                	canBuildHere = false;
		                	pathfindingTurn++;
                		}else //we already picked a direction, lets go again
                		{

                     		
                			if (!(moveLastTurnDir == null))
                			{
                				//rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(dir, 2), 2))
                				if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(moveLastTurnDir, 2), 2))
                				{
                					for (int i = 1; i <360; i++)
                					{
                						if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(moveLastTurnDir.rotateLeftDegrees(i*21), 2), 2))
                						{
                							System.out.println("i moved "+i);
                							pathfindingTurn = 0;
                							tryMove(moveLastTurnDir.rotateLeftDegrees(i*21));
                							break;
                						}
                					}
                				}
                				//tryMove(moveLastTurnDir);
                				rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(moveLastTurnDir), 255, 255, 255);
                				pathfindingTurn++;
                				if (pathfindingTurn == 3){
                					pathfindingTurn = 0;
                					if ( rc.canPlantTree(moveLastTurnDir.opposite()))
                                    {
                                 		//rc.plantTree(moveLastTurnDir.opposite());
                                    }

                					//System.out.println("Pathfinding");
                				}
                         		if (rc.canBuildRobot(RobotType.LUMBERJACK, moveLastTurnDir.opposite()) && rc.isBuildReady()) {
                         			rc.buildRobot(RobotType.LUMBERJACK, moveLastTurnDir.opposite());
                         		}
                			}else
                			{
                				//System.out.println("random");
                				tryMove(randomDirection());//if our move logic fails, random
                			}
                		}
                	}
                }else
                {
                	//build 5 trees around me, then pump out combat units
                	for(int i = 0;i<6;i++)
                	{
                		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                     	if(i==5)
                     	{
                     		//TODO build appropriate combat bots, lumberjack rush for now
                     		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                     		if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDirections[i]) && rc.isBuildReady()) {
                     			rc.buildRobot(RobotType.LUMBERJACK, buildDirections[i]);
                     		}
                     	}else if ( rc.canPlantTree(buildDirections[i]))
                        {
                     		rc.plantTree(buildDirections[i]);
                     		begunBuilding = true;
                        }
                	}
                	TreeInfo[] treesIBuilt = rc.senseNearbyTrees(1, rc.getTeam());
                	
                	if(treesIBuilt.length <1)//if I havent built any trees yet
                	{
                		for (float i = 0; i<2;i = i + (float)0.1)
                		{
                			Direction currentDir = new Direction(i);
                			if ( rc.canPlantTree(currentDir))
                            {
                         		rc.plantTree(currentDir);
                         		begunBuilding = true;
                            }
                		}
                	}
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                howOldIAm++;
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }


    private static Direction awayFromTarget(MapLocation myLoc, MapLocation targetLoc) {
    	Direction myArchon = new Direction(myLoc,targetLoc);
    	return myArchon.opposite();
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
                RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

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
                        rc.strike();//and swing incase they are hinding behind something
                    } else {
                        // no Robots in sight, check for trees
                        TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().strideRadius, Team.NEUTRAL);//chopping range
                     	if (!(trees.length > 0))//if there arent any neutral trees
                    	{
                    		trees = rc.senseNearbyTrees(rc.getType().strideRadius, rc.getTeam().opponent());//find some enemy trees
                    	}
                        if(trees.length > 0 && !rc.hasAttacked())
                        {
                        	//if there are trees
                        	if(rc.canChop(trees[0].getLocation()))
                        	{
                        		rc.chop(trees[0].getLocation());
                        	}
                        }
                        else
                        {
                            trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);//are there any neutral trees nearby
                        	if (!(trees.length > 0))//if there arent any neutral trees
                        	{
                        		trees = rc.senseNearbyTrees(-1, rc.getTeam().opponent());//find some enemy trees
                        	}
                        	
                        	if (trees.length > 0)//if there are some trees in range, we will move toward them
                        	{
                        		tryMove(rc.getLocation().directionTo(trees[0].getLocation()));
                        	}else //since there are no enemys, or trees in range, lets explore
                        	{
                        		//lets try moving away from nearest friendly
                        		RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                        		if (friendlyRobots.length > 0)
                        		{
                        			tryMove(awayFromTarget(rc.getLocation(),friendlyRobots[0].getLocation()));
                        			
                        		}else
                        		{
	                        		//System.out.println("I'm a lumberjack, i tried to move again");
	                        		//tryMove(randomDirection());//all else fails, random
                        		}
                        	}
                        }
                        
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
        return tryMove(dir,5,36);
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

}
