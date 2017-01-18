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
            case SCOUT:
            	runScout();
            	break;
            case TANK:
            	//runTank();
            	break;
        }
        
        
    }
    static void runArchon() throws GameActionException {
        //System.out.println("I'm an archon!");
        int numGardners = 0;
        int deisredNumGardners = 7;
        int buildTurn = 0;
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
            	if (rc.isBuildReady() && rc.hasRobotBuildRequirements(RobotType.GARDENER))
            	{
	                switch (buildTurn) {
	                case 0:
	                    //infront
	                	if (hireGardener(dir))
	                	{
	                		break;
	                	}
	                case 1:
	                	//left 50
	                	if (hireGardener(dir.rotateLeftDegrees(50)))
	                	{
	                		break;
	                	}
	                case 2:
	                	//right 50
	                	if (hireGardener(dir.rotateRightDegrees(50)))
	                	{
	                		break;
	                	}
	                case 3:
	                	//left 100
	                	if (hireGardener(dir.rotateLeftDegrees(100)))
	                	{
	                		break;
	                	}
	                case 4:
	                	//right 100
	                	if (hireGardener(dir.rotateRightDegrees(100)))
	                	{
	                		break;
	                	}
	                case 5:
	                	//left 150
	                	if (hireGardener(dir.rotateLeftDegrees(150)))
	                	{
	                		break;
	                	}
	                case 6:
	                	//right 150
	                	if (hireGardener(dir.rotateRightDegrees(150)))
	                	{
	                		break;
	                	}
	                	buildTurn = 0;
	                	break;
	                }
	                buildTurn++;
            	}
                
            	
            	if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(dir, 2), 2))
            	{
            		dir = randomDirection();
            	}
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    //hires a gardener in a direction
    //returns true if gardener was hired
	private static boolean hireGardener(Direction dir) throws GameActionException {
        if (rc.canHireGardener(dir)) {
            	rc.hireGardener(dir);
            	return true;
        }
		return false;
	}
	static void runGardener() throws GameActionException {
        //System.out.println("I'm a gardener!");
    	//Team enemy = rc.getTeam().opponent();
    	Team friendly = rc.getTeam();
        RobotInfo myArchon = null;
        MapLocation ArchonStartingLoc = null;
        boolean canBuildHere = false;
        boolean begunBuilding = false;
        int buildOrder = 0;
        Direction moveLastTurnDir = null;
        int howOldIAm = 0;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

            	//first thing to do is build 1 scout GLOBAL MESAGING
            	int builtScout = rc.readBroadcast(0);
            	
            	if (builtScout == 0)
            	{
            		Direction randomDir = randomDirection();
            		if(rc.canBuildRobot(RobotType.SCOUT, randomDir))
            		{
            			rc.buildRobot(RobotType.SCOUT, randomDir);
            			rc.broadcast(0, 1);//broadcasts that we built a scout, so noone else will
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

                canBuildHere = true;
                if(begunBuilding)//if we started building here, then we already vetted the spot
                {
                	canBuildHere = true;//invalidates the checking above
                }else if (friendlyRobots.length > 0)//if we havent started building here, and the nearby robot might be the cause, 
                {
                	canBuildHere = false;
                }
                
                //move logic goes here
                if(  !canBuildHere || howOldIAm < 3) //if we cant build here, or we are too young
                {
                	if(!begunBuilding)//if we would normally move, but have already made trees, just wait this turn, and try again next turn
                	{
                		//System.out.println("Still Looking");
                		
                		if (!(moveLastTurnDir == null))//if i had to go around something
                		{
                			//System.out.println("trying again");
                			//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(moveLastTurnDir), 0, 0, 255);//this turn
                			//just try and go that way again
                			moveLastTurnDir = moveAround(moveLastTurnDir);
                			//moveLastTurnDir = null;//set it back to nothing so we move away normally next turn
                		}else if(rc.getLocation().distanceTo(ArchonStartingLoc) < 2)//arbitary distance to archon
                		{
                			//System.out.println("Away from archon");
                			Direction awayFromArchon = awayFromTarget(rc.getLocation(),ArchonStartingLoc);
                			if(rc.canMove(awayFromArchon))
                			{
                				rc.move(awayFromArchon);
                			}else
                			{
                				//avoidance code
                				//System.out.println("Trying avoidance");
                				moveLastTurnDir = moveAround(awayFromArchon);
                			}
                		}else if (friendlyRobots.length>0)//if there are other bots nearby
                		{
                			//System.out.println("away from friendlies");
                			Direction awayFromFriendlies = awayFromTarget(rc.getLocation(),friendlyRobots[0].getLocation());
                			if(rc.canMove(awayFromFriendlies))
                			{
                				rc.move(awayFromFriendlies);
                			}
                			else
                			{
                				//System.out.println("Trying avoidance");
                				//avoidance code
                				//tryMove(awayFromFriendlies);
                				moveLastTurnDir = moveAround(awayFromFriendlies);
                			}
                		}else
                		{
                			//System.out.println("why cant I move");
                		}
                	}
	                canBuildHere = false;//redundant?
	           
                }else
                {
             		begunBuilding = true;
             		//System.out.println("Ive chosen a spot");
                	//build 5 trees around me, then pump out combat units
                	for(int i = 0;i<6;i++)
                	{
                		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                     	if(i==5)
                     	{
                     		//build order
                     		//scout
                     		//5 lumberjacks
                     		//soldier spam
                     		//rc.setIndicatorDot(buildLocations[i], 255, 255, 255);
                     		if (buildOrder ==0)
                     		{
                     			if(rc.canBuildRobot(RobotType.SCOUT, buildDirections[i]))
                     			{
                     				rc.buildRobot(RobotType.SCOUT, buildDirections[i]);
                     				buildOrder++;
                     			}
                     		}else if (buildOrder > 0 && buildOrder <6)
                     		{
                     			if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDirections[i]))
                     			{
                     				rc.buildRobot(RobotType.LUMBERJACK, buildDirections[i]);
                     				buildOrder++;
                     			}
                     		}else if (buildOrder>=6)
                     		{
                     			if(rc.canBuildRobot(RobotType.SOLDIER, buildDirections[i]))
                     			{
                     				rc.buildRobot(RobotType.SOLDIER, buildDirections[i]);
                     				buildOrder++;//no required
                     			}
                     		}
                     		
                     		
                     	}else if ( rc.canPlantTree(buildDirections[i]))
                        {
                     		rc.plantTree(buildDirections[i]);
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
                //TODO try to build a lumberjack here, if i havent built anything this turn yet, i might not have space, to make a space making bot
                if (false && rc.hasRobotBuildRequirements(RobotType.LUMBERJACK))//mainly checking for bullets
                {
                	for (int i = 0;i<100;i++)
                	{
                		if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDirection))
                		{
                			rc.buildRobot(RobotType.LUMBERJACK, buildDirection);
                		}else
                		{
                			buildDirection = buildDirection.rotateLeftDegrees(i*21);
                		}
                	}
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                howOldIAm++;
                Clock.yield();
                //s

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }


    private static Direction moveAround(Direction desiredDir) throws GameActionException {
     	if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(desiredDir, 2), 1) || !rc.onTheMap(rc.getLocation().add(desiredDir, 2), 2))
		{
			for (int i = 1; i <360; i++)
			{
				//rc.setIndicatorDot(rc.getLocation().add(desiredDir, 2), 255, 0, 0);
				//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(desiredDir), 255, 0, 0);//this turn
		        if (rc.canMove(desiredDir))
		        {
		            rc.move(desiredDir);
		            return desiredDir;
		        }else
		        {
		            desiredDir = desiredDir.rotateLeftDegrees(i*21);//rotate for the next attempt
					//tryMove(moveLastTurnDir.rotateLeftDegrees(i*21));
				}
			}
		}
     	else
     	{
     		tryMove(desiredDir);
     		return desiredDir;
     	}
     	//System.out.println("in all the confusion i forgot to move");
     	return desiredDir;
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
        	//TODO
        	//squad rotation, so each can shoot
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                //MapLocation myLocation = rc.getLocation();
            	//shoot enemy
            	//get out of friendly way
            	//move toward ping
            	//move toward archon
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

	static void runScout() throws GameActionException {
        //System.out.println("I'm an soldier!");
        //Team enemy = rc.getTeam().opponent();
        while (true) {
        	try{
        		MapLocation[] enemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
            	Direction targetArchon = rc.getLocation().directionTo(enemyArchons[0]);
            	tryMove(targetArchon);
	        	//TODO
	        	//ping gardners
	        	//sense lumberjack/soldier @9, MOVE AWAY 1
	        	//attack gardener 
	        	//sense and move toward gardener
	        	//tree shaking?
	        	//move to archon
            	
            	//ping gardeners so our combat units can kill them
            	RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            	if(enemyRobots.length>0)
            	{
            		for(int i = 0;i<enemyRobots.length;i++)
            		{
            			if(enemyRobots[i].getType() == RobotType.GARDENER)
            			{
            				//get its location, broadcast
            				break;
            			}
            		}
            	}
            	Clock.yield();//ends this turn
        	}
        	catch (Exception e) {
        		System.out.println("Scout Exception");
        		e.printStackTrace();
        	}
        }
	}

    static void runLumberjack() throws GameActionException {
        //System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        MapLocation enemyArchom = rc.getLocation();
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
                        //rc.strike();//and swing incase they are hinding behind something//TODO we get stuck behind things if we see an enemy
                    } else {
                        // no Robots in sight, check for trees
                        TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().strideRadius, Team.NEUTRAL);//chopping range
                     	if (!(trees.length > 0))//if there arent any neutral trees
                    	{
                    		trees = rc.senseNearbyTrees(rc.getType().strideRadius, rc.getTeam().opponent());//find some enemy trees
                    	}
                     	if (trees.length > 0)
                     	{
                     		rc.setIndicatorDot(trees[0].getLocation(), 255, 255, 255);
                       	}
                        if(trees.length > 0 && !rc.hasAttacked())
                        {
                        	//if there are trees
                        	//System.out.println("I'm a lumberjack! but trees are hard");
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
                        		rc.setIndicatorDot(trees[0].getLocation(), 0, 0, 0);
                        		tryMove(rc.getLocation().directionTo(trees[0].getLocation()));
                            	if(rc.canChop(trees[0].getLocation()))
                            	{
                            		rc.chop(trees[0].getLocation());
                            	}
                        	}else //since there are no enemys, or trees in range, lets explore
                        	{
                        		//path to enemy archon
                            	MapLocation[] enemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
                            	Direction targetArchon = rc.getLocation().directionTo(enemyArchons[0]);
                            	moveAround(targetArchon);
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
        return tryMove(dir,1,30);
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
