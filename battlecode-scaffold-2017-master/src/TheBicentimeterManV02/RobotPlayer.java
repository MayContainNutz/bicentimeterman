package TheBicentimeterManV02;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	// broadcast info
	// broadcast 0 first scout flag
	// broadcast 1 x location of an enemy gardener
	// broadcast 2 y location of an enemy gardener
	// broadcast 3 x location of any spotted enemy
	// broadcast 4 y location of any spotted enemy
	// broadcast 5 first soldier out
	// broadcast channels
	private static final int BUILD_ORDER = 0;
	private static final int ENEMY_GARDENER_X = 1;
	private static final int ENEMY_GARDENER_Y = 2;
	private static final int SPOTTED_ENEMY_X = 3;
	private static final int SPOTTED_ENEMY_Y = 4;
	private static final int PANIC_X = 5;
	private static final int PANIC_Y = 6;
	private static final int CURRENT_GARDENERS = 7;
	private static final int GARDENER_COUNTING_ARCHON = 8;
	private static final int GARDENER_COUNTER = 9;

	private static final int DESIRED_NUM_GARDENERS = 15;

	private static RobotType[] buildOrder;
	// @SuppressWarnings("unused")

	// run called when robot spawns, if run ends/returns, robot explodes
	public static void run(RobotController rc) throws GameActionException {

		RobotPlayer.rc = rc;
		
		// switch is probably the easiest way to split operation based on robot
		// type for a small bytecode overhead in its first turn
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
			runTank();
			break;
		default:
			break;
		}
	}

	private static void runTank() {
		while (true)// as long as we are alive, do stuff
		{
			try {
				// TODO makes tanks do stuff
				checkForDonateWin();
				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private static void runArchon() {
		while (true)// as long as we are alive, do stuff
		{
			try {
				// do we win this turn
				checkForDonateWin();
				// run away if we are getting shot at
				//avoidDamage();
				// build econ units if required
				econBuild();

				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private static void runScout() {
		
		RobotInfo currentTarget = null;
		RobotInfo targetLastRound = null;
		int turnsOnTarget = 0;
		
		while (true)// as long as we are alive, do stuff
		{
			try {
				//checkForDonateWin();

				// dodge damage?
				// move away from melee/close range units
				avoidDamage();
				// go to help our gardeners

				MapLocation distressedGardener = findDistressedEcon();
				if (distressedGardener != null) {
					moveToward(distressedGardener);
				}

				// move toward nearest gardener
				MapLocation nearestGardener = nearestEnemyGardener();
				if (nearestGardener != null) {
					moveToward(nearestGardener);
				}
				// if none in sight, see if another scout has pinged one
				MapLocation pingedGardener = getPingedGardener();
				if (pingedGardener != null) {
					moveToward(pingedGardener);
				}
				/*
				 * //if no other gardeners location is known, try to engage
				 * regular combat units MapLocation pingedTarget =
				 * getPingedEnemy(); if(pingedTarget != null) {
				 * moveToward(pingedTarget); }
				 */
				// shake trees, either on your way, or if you have nothing
				// better to do
				shakeTrees();
				// visit the starting location of the enemy archon
				MapLocation enemyArchonStart = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
				// pick the first, its just a general area to start the search
				if (enemyArchonStart != null)// TODO remove when full search
												// works
				{
					moveToward(enemyArchonStart);
				}
				// full map coverage/search pattern
				// TODO full search
				// locate gardeners for the team
				pingGardeners();// if my nearest enemy gardener is still alive
								// at the end of my turn, request help
				pingEnemy();// locate other enemies so our other units know
							// where to go if they arent in combat
				
				if(turnsOnTarget > 2)
				{
					currentTarget = null;
					turnsOnTarget = 0;
				}
				if(currentTarget == null)
				{
					currentTarget = nearestEnemy();
				}else
				{
					targetLastRound = currentTarget;
					currentTarget = findTarget(currentTarget.getID());
					turnsOnTarget++;
				}
				//System.out.println("turnsOnTarget "+turnsOnTarget );
				attack(currentTarget,targetLastRound);
				
				//attack(nearestEnemy());
				RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
				if (nearbyRobots.length > 0) {
					MapLocation nearestFriendly = nearbyRobots[0].getLocation();
					if (nearestFriendly != null) {
						moveAwayFrom(nearestFriendly);
					}
				}
				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private static void runLumberjack() {
		while (true)// as long as we are alive, do stuff
		{
			try {
				checkForDonateWin();
				// ping enemies
				pingEnemy();
				//avoidDamage();

				// get nearest enemy
				RobotInfo nearestEnemy = nearestEnemy();
				// hit things if they are in range
				if (nearestEnemy != null) {
					attack(nearestEnemy);// nots sure about this
					// move toward things i can see
					moveToward(nearestEnemy.getLocation());
				}
				// go to help our gardeners
				MapLocation distressedGardener = findDistressedEcon();
				if (distressedGardener != null) {
					moveToward(distressedGardener);
				}
				// listen for target pinging and head towards
				MapLocation pingedEnemy = getPingedEnemy();
				if (pingedEnemy != null) {
					//moveToward(pingedEnemy);//dont bother, just chop out our area
				}
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, rc.getTeam().opponent());
				MapLocation nearestTree = null;
				if (nearbyTrees.length > 0) {
					nearestTree = nearbyTrees[0].getLocation();
				}
				// = rc.senseNearbyTrees(-1,
				// rc.getTeam().opponent())[0].getLocation();
				if (nearestTree == null) {
					nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
					if (nearbyTrees.length > 0) {
						nearestTree = nearbyTrees[0].getLocation();
					}
				}
				if (nearestTree != null) {
					moveToward(nearestTree);// will move nearer if not already
											// moved
					chopTrees(nearestTree);// chop trees if any in range
				}
				// TODO explore/search
				
				//for now, we will head towards an enemy archon
            	MapLocation[] enemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
            	//Direction targetArchon = rc.getLocation().directionTo(enemyArchons[0]);
            	moveToward(enemyArchons[0]);

				
				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private static void runSoldier() {
		
		RobotInfo currentTarget = null;
		RobotInfo targetLastRound = null;
		int turnsOnTarget = 0;
		
		while (true)// as long as we are alive, do stuff
		{
			try {
				checkForDonateWin();
				// shoot at stuff if appropriate
				// ping enemies
				pingEnemy();
				// avoid stuff
				avoidDamage();
				// listen for ping
				// go to help our gardeners
				MapLocation distressedGardener = findDistressedEcon();
				if (distressedGardener != null) {
					moveToward(distressedGardener);
				}
				MapLocation pingedEnemy = getPingedEnemy();
				if (pingedEnemy != null) {
					// move towards ping
					moveToward(pingedEnemy);
				}
				//just call attack?
				//i only want to get a new enemy every x turns, probably 3
				//System.out.println("step 1, have i selected a target " + currentTarget + " " + targetLastRound);
				if(turnsOnTarget > 2)
				{
					currentTarget = null;
					turnsOnTarget = 0;
				}
				if(currentTarget == null)
				{
					currentTarget = nearestEnemy();
					if(currentTarget == null)//if theres no enemy
					{
						targetLastRound = null;//dont try to extrapolate its position
					}
				}else
				{
					targetLastRound = currentTarget;
					currentTarget = findTarget(currentTarget.getID());
					if(currentTarget != null)//if we found it again
					{
						turnsOnTarget++;//track our turns on this target
					}else
					{
						targetLastRound = null;//if its gone, null its previous pos
					}
				}
				//System.out.println("step 1, have i selected a target " + currentTarget + " " + targetLastRound);
				attack(currentTarget,targetLastRound);
				//attack(nearestEnemy());
				RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
				if (nearbyRobots.length > 0) {
					MapLocation nearestFriendly = nearbyRobots[0].getLocation();
					if (nearestFriendly != null) {
						moveAwayFrom(nearestFriendly);
					}
				}
				// form perimeter?
				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}


	private static void runGardener() {// TODO bytecode spike while building,
										// seems to break everything
		boolean established = false;
		buildOrder = buildListGenerate();// makes the build order
														// list
		while (true)// as long as we are alive, do stuff
		{
			try {
				updateCurrGardeners();// update the count of gardeners so our
										// archons know how many to build
				checkForDonateWin();
				panicPing();// pings a special channel reserved for emergency
							// danger pings
				waterTrees();
				// find a spot to build
				if (!established)// if we already have a spot, dont bother
				{
					established = findBuildSite();
				}
				// build trees
				econBuild(established);
				// build combat units
				warBuild(established);

				Clock.yield();// ends this turn
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	// sees if we can win outright by donating all our bullets, or donates them
	// anyway if its the last turn
	private static void checkForDonateWin() throws GameActionException {
		// TODO since victory points have changed a bit, re think how when and
		// if to buy them
		/*
		 * float wincond = GameConstants.VICTORY_POINTS_TO_WIN *
		 * GameConstants.BULLET_EXCHANGE_RATE; if (rc.getTeamBullets()>=
		 * wincond) { rc.donate(wincond); } if (rc.getRoundNum() >
		 * rc.getRoundLimit()-2 )//check for last round 1 and 0 indexed :/ {
		 * 
		 * rc.donate(rc.getTeamBullets());//on the last round, donates all our
		 * bullets }
		 */
	}

	// pass a maplocation, if its a tree, and its in range, it gets the chop
	private static void chopTrees(MapLocation nearestTree) throws GameActionException {
		if (rc.canChop(nearestTree)) {
			rc.chop(nearestTree);
		}
	}

	private static void attack(RobotInfo currentTarget, RobotInfo targetLastRound) throws GameActionException {
		// TODO Auto-generated method stub
		//Theta = sin^-1 (V(r) x V(b) x y^2)
		//Theta = cos^-1 (V(r) x V(b) x x^2)
		//System.out.println("step 2, i want to shoot at " + currentTarget + " " + targetLastRound);
		if(currentTarget == null)
		{
			return;
		}
		if(targetLastRound == null)
		{
			attack(currentTarget.getLocation());
			return;
		}
		
		float ourBulletSpeed = rc.getType().bulletSpeed;
		float enemySpeed = currentTarget.getLocation().distanceTo(targetLastRound.getLocation());
		float distanceToEnemy = rc.getLocation().distanceTo(currentTarget.getLocation());
		float numbersAreSilly = ourBulletSpeed * enemySpeed * distanceToEnemy *distanceToEnemy;
		//TODO work out how to do theta with museli
		//double theta = Math.asin(ourBulletSpeed * enemySpeed * distanceToEnemy *distanceToEnemy);
		
		//System.out.println("mathy " + ourBulletSpeed +" "+ enemySpeed +" "+ distanceToEnemy +" "+theta +" "+numbersAreSilly);
		Direction toTarget = rc.getLocation().directionTo(currentTarget.getLocation());
		//Direction predictedDirection = toTarget.rotateLeftDegrees((float) theta);
		Direction targetsPredictedMove = targetLastRound.getLocation().directionTo(currentTarget.getLocation());
		//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(toTarget, 3), 255, 255, 255);
		//rc.setIndicatorLine(currentTarget.getLocation(), currentTarget.getLocation().add(targetsPredictedMove,3), 0, 255, 0);
		//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(predictedDirection,3), 255, 0, 0);
		//rc.setIndicatorDot(rc.getLocation().add(predictedDirection), 0, 0, 0);
		
		//attack(rc.getLocation().add(predictedDirection));
		//rc.setIndicatorLine(rc.getLocation(), currentTarget.getLocation(), 255, 0, 0);
		attack(currentTarget);
	}

	private static RobotInfo findTarget(int id) throws GameActionException {
		//returns the robotInfo of a robot if its in view range
		if(rc.canSenseRobot(id))
		{
			return rc.senseRobot(id);
		}
		return null;
	}

	private static void attack(RobotInfo nearestEnemy) throws GameActionException {
		if(nearestEnemy == null)
		{
			return;
		}
		attack(nearestEnemy.getLocation());
	}
	// general attack for all bots, passed an enemy
	private static boolean attack(MapLocation nearestEnemy) throws GameActionException {
		// figure out what we are, and thus what attacks we have available
		// check target is within range
		// check that nothing will block the shot
		// strike or shoot
		/*
		if(!rc.canSenseLocation(nearestEnemy))
		{
			return false;
		}
		*/
		if (nearestEnemy == null)// if no nearest enemy, it will be null
		{
			return false;// so we bail out early
		}
		
		if (rc.canStrike()) {
			rc.strike();
			return true;
		}
		//System.out.println("I'm trying to shoot");
		if (rc.canFirePentadShot() && canShoot(nearestEnemy, 5)) {
			rc.firePentadShot(rc.getLocation().directionTo(nearestEnemy));
			return true;
		}
		
		if (rc.canFireTriadShot() && canShoot(nearestEnemy, 3)) {
			rc.fireTriadShot(rc.getLocation().directionTo(nearestEnemy));
			return true;
		}
		
		if (rc.canFireSingleShot() && canShoot(nearestEnemy, 1)) {
			rc.fireSingleShot(rc.getLocation().directionTo(nearestEnemy));
			return true;
		}
		return false;
	}

	private static boolean canShoot( MapLocation target, int numShots) throws GameActionException {
		// TODO check los for bullets
		float distanceToTarget = Math.min(
				rc.getLocation().distanceTo(target) - (2),
				rc.getType().sensorRadius - 1);
		Direction targetDirection = rc.getLocation().directionTo(target);
		float checkSize = (float) 0.3 * numShots;//size of the check that we have a clear shot
		if (distanceToTarget <= rc.getType().bodyRadius + 1) {
			return true;
		}
		for (float i = 0; i < distanceToTarget; i = i + (float) 0.1) {
			// System.out.println(" " + i );
			// if any point is occupied buy anything other than an enemy, dont
			// shoot
			// rc.setIndicatorDot(me.getLocation().add(targetDirection,
			// i),0,0,0);
			
			if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(targetDirection, i), checkSize)) {
				// found something
				return false;
			}
		}
		return true;
	}

	private static MapLocation getPingedEnemy() throws GameActionException {
		int enemyX = rc.readBroadcast(SPOTTED_ENEMY_X);
		int enemyY = rc.readBroadcast(SPOTTED_ENEMY_Y);
		if (enemyX != 0 || enemyY != 0) {
			MapLocation target = new MapLocation(enemyX, enemyY);
			return target;
		}
		return null;
	}

	private static MapLocation getPingedGardener() throws GameActionException {
		int enemyX = rc.readBroadcast(ENEMY_GARDENER_X);
		int enemyY = rc.readBroadcast(ENEMY_GARDENER_Y);
		if (enemyX != 0 || enemyY != 0) {
			MapLocation target = new MapLocation(enemyX, enemyY);
			return target;
		}
		return null;
	}

	private static RobotInfo nearestEnemy() {
		RobotInfo robots[] = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (robots.length > 0) {
			return robots[0];// since the sense arrays are sorted by distance
								// already, just return the first one
		}
		return null;
	}

	private static void econBuild() throws GameActionException {
		// build econ(gardener) for use by archon
		int currGardeners = getCurrGardeners();
		Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
		int minBullets = 0;
		if(currGardeners > 0 && rc.getRobotCount() < 5)//helps get the early game combat units built, to stop rushes
		{
			minBullets = 180;
		}
		if (currGardeners < DESIRED_NUM_GARDENERS && rc.getTeamBullets() > minBullets) {
			if (rc.isBuildReady() && rc.hasRobotBuildRequirements(RobotType.GARDENER)) {
				if (currGardeners > 6) {
					currGardeners -= 6;
				}
				switch (currGardeners) {
				case 0:
					// infront
					if (hireGardener(dir)) {
						break;
					}
				case 1:
					// left 50
					if (hireGardener(dir.rotateLeftDegrees(50))) {
						break;
					}
				case 2:
					// right 50
					if (hireGardener(dir.rotateRightDegrees(50))) {
						break;
					}
				case 3:
					// left 100
					if (hireGardener(dir.rotateLeftDegrees(100))) {
						break;
					}
				case 4:
					// right 100
					if (hireGardener(dir.rotateRightDegrees(100))) {
						break;
					}
				case 5:
					// left 150
					if (hireGardener(dir.rotateLeftDegrees(150))) {
						break;
					}
				case 6:
					// right 150
					hireGardener(dir.rotateRightDegrees(150));
					break;

				}
			}
		}
	}

	private static int getCurrGardeners() throws GameActionException {
		// gets the current number of gardeners as counted by updatCurrGardeners
		// we have to 'elect' an archon to reset the counter, otherwise we get
		// rogues who spam gardeners
		// TODO hold elections every * rounds, mostly to account for the
		// currently elected archon dying
		// TODO poss error with incorrect reporting, believed to be related to
		// pathfinding causing massive spikes in bytecode
		int controlArchon = rc.readBroadcast(GARDENER_COUNTING_ARCHON);
		int currentGardeners = rc.readBroadcast(CURRENT_GARDENERS);
		// every 50? rounds, reset the control archon
		// int countedGardeners = rc.readBroadcast(GARDENER_COUNTER);
		if (controlArchon == 0 || controlArchon == rc.getID()) {
			int countedGardeners = rc.readBroadcast(GARDENER_COUNTER);
			rc.broadcast(GARDENER_COUNTER, 0);
			rc.broadcast(CURRENT_GARDENERS, countedGardeners);
			rc.broadcast(GARDENER_COUNTING_ARCHON, rc.getID());
		}
		// System.out.println("currgard " + currentGardeners + " "
		// +controlArchon+ " "+countedGardeners);
		return currentGardeners;
	}

	private static void updateCurrGardeners() throws GameActionException {
		// read broadcast channel
		// increment
		// write broadcast channel
		int currentGardeners = rc.readBroadcast(GARDENER_COUNTER);
		currentGardeners++;
		rc.broadcast(GARDENER_COUNTER, currentGardeners);
	}

	private static boolean hireGardener(Direction dir) throws GameActionException {
		if (rc.canHireGardener(dir)) {
			rc.hireGardener(dir);
			return true;
		}
		return false;
	}

	// tree building function for gardeners
	private static void econBuild(boolean established) throws GameActionException {
		// if we here with a true, its time to build
		if (!established)// if established is false
		{
			return;// bail out and dont build
		}
		// start looking away from the archon
		Direction buildDirection = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0]);
		// build a map of possible build locations
		Direction[] buildDirections = new Direction[6];
		buildDirection = buildDirection.rotateLeftDegrees(60);// 60 offcentre,
																// so the combat
																// units come
																// out facing
																// DIRECTLY away
																// from the
																// archon
		for (int i = 0; i < 6; i++)// 6 spots load up the tree matrix
		{
			buildDirections[i] = buildDirection;
			buildDirection = buildDirection.rotateLeftDegrees(60);// 60 degrees
																	// apart
		}
		int botSpot = -1;
		// check the build spots, first one thats free is earmarked for building
		// units, otherwise fill them up with treeeeeeees
		for (int i = 0; i < 6; i++) {
			// other units i dont care about, only one that wont move away is
			// archon, and i moved from it to start with
			if (!rc.isLocationOccupiedByTree(rc.getLocation().add(buildDirections[i], rc.getType().bodyRadius + 1))) {
				if (botSpot == -1) {
					botSpot = i;
				}
				if (rc.canPlantTree(buildDirections[i]) && botSpot != i) {
					rc.plantTree(buildDirections[i]);
				}
			}
		}
	}

	private static void warBuild(boolean established) throws GameActionException {
		// TODO build warmaking units
		Direction buildDirection = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0]);
		/*
		if (!established) {

			buildNextCombatUnit(buildDirection);
			return;
		}
		*/
		// Direction[] buildDirections = new Direction[6];
		// buildDirection = buildDirection.rotateLeftDegrees(60);//60 offcentre,
		// so the combat units come out facing DIRECTLY away from the archon
		for (int i = 0; i < 40; i++)// 6 spots load up the tree matrix
		{
			// try and build in default direction
			buildNextCombatUnit(buildDirection);
			buildDirection = buildDirection.rotateLeftDegrees(i * 9);
		}

	}

	private static void buildNextCombatUnit(Direction buildDirection) throws GameActionException {
		// TODO combat unit build order/logic
		int buildCounter = rc.readBroadcast(BUILD_ORDER);
		if (rc.canBuildRobot(buildOrder[buildCounter], buildDirection)) {
			rc.buildRobot(buildOrder[buildCounter], buildDirection);
			//we just built something, update the build order
			buildCounter++;
			if(buildCounter >= buildOrder.length)//if we exceed our build order, start at the beginning
			{
				rc.broadcast(BUILD_ORDER, 0);
			}else
			{
				rc.broadcast(BUILD_ORDER, buildCounter);//if not, let the next unit know what to build
			}
		}
	}

	private static RobotType[] buildListGenerate() {
		RobotType[] buildOrder = new RobotType[6];
		buildOrder[0] = RobotType.SCOUT;
		buildOrder[1] = RobotType.SOLDIER;
		buildOrder[2] = RobotType.LUMBERJACK;
		buildOrder[3] = RobotType.LUMBERJACK;
		buildOrder[4] = RobotType.SOLDIER;
		buildOrder[5] = RobotType.SOLDIER;
		return buildOrder;
	}

	private static boolean findBuildSite() throws GameActionException {
		// TODO gardener farm location picking needs a rework, new spot
		// choosing, or at least pathfinding
		

		// be aware of where friendly robots are
		RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
		RobotInfo friendlyGardener = null;
		RobotInfo friendlyArchon = null;
		for (int i = 0; i < friendlyRobots.length; i++) {
			if (friendlyRobots[i].getType() == RobotType.ARCHON && friendlyArchon == null) {
				// if we find an archon in range
				friendlyArchon = friendlyRobots[i];
				moveAwayFrom(friendlyRobots[i].getLocation());// move away from
																// it
				return false;// return false to keep looking for a spot
			}
		}
		for (int i = 0; i < friendlyRobots.length; i++)
		{
			if (friendlyRobots[i].getType() == RobotType.GARDENER && friendlyGardener == null) {
				// this time its a friendly robot in the way
				friendlyGardener = friendlyRobots[i];
				moveAwayFrom(friendlyRobots[i].getLocation());
				return false;
			}
		}

		// lets get a little further way from the edge, so things can path bast
		// us
		int distanceFromEdge = 5;
		Direction mapEdgeDirection = Direction.getNorth();
		MapLocation lookingForMapEdge = rc.getLocation().add(mapEdgeDirection, distanceFromEdge);
		for (int i = 1; i < 5; i++) {
			if (!rc.onTheMap(lookingForMapEdge)) {
				moveAwayFrom(lookingForMapEdge);
				return false;
			}
			// if it IS on the map, rotate and try again
			lookingForMapEdge = rc.getLocation().add(mapEdgeDirection.rotateLeftDegrees(i * 90), distanceFromEdge);
		}

		return true;// if there arent any robots that trip up in the loop, we
					// are clear to build
	}

	private static void waterTrees() throws GameActionException {
		// of all the trees I can see, which ever is the weakest
		TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());

		if (trees.length > 0)// if there are any trees
		{
			float weakestTree = trees[0].maxHealth;
			int waterMe = -1;
			for (int i = 0; i < trees.length; i++)// for each tree
			{
				if (trees[i].health < weakestTree)// if it has the lowest health
													// yet
				{
					if (rc.canWater(trees[i].ID)) {
						weakestTree = trees[i].health;// record lowest health
						waterMe = i;// record its index
					}
				}
			}
			if (waterMe != -1)// if we chose a tree with < max health
			{
				rc.water(trees[waterMe].ID);// water it
			}
		}
	}

	// move away from lumberjacks and bullets(heatmap?)
	// TODO better bullet dodging
	private static void avoidDamage() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		if (bullets.length > 0) {
			// if there are bullets, at least try to avoid them a little
			for (int i = 0; i < bullets.length; i++) {
				Direction dirToMe = bullets[i].getLocation().directionTo(rc.getLocation());
				Direction bullettravel = bullets[i].getDir();
				// System.out.println("I'm dodging "+
				// dirToMe.degreesBetween(bullettravel));
				if (dirToMe.degreesBetween(bullettravel) < 45 && dirToMe.degreesBetween(bullettravel) > -1) {
					// System.out.println("actually dodging "+
					// dirToMe.degreesBetween(bullettravel));
					// rc.setIndicatorLine(rc.getLocation(),
					// bullets[i].getLocation(), 255, 255, 255);
					Direction dirToAvoid = dirToMe.rotateRightDegrees(90);// gotta
																			// figure
																			// out
																			// if
																			// this
																			// is
																			// away
																			// or
																			// towards
					MapLocation dodge = rc.getLocation().add(dirToAvoid, rc.getType().strideRadius);
					moveToward(dodge);
					// System.out.println("I really dodged a BULLET there");
					break;
				}
				if (dirToMe.degreesBetween(bullettravel) < 0 && dirToMe.degreesBetween(bullettravel) > -45) {
					// System.out.println("actually dodging "+
					// dirToMe.degreesBetween(bullettravel));
					// rc.setIndicatorLine(rc.getLocation(),
					// bullets[i].getLocation(), 0, 0, 0);
					Direction dirToAvoid = dirToMe.rotateLeftDegrees(90);// gotta
																			// figure
																			// out
																			// if
																			// this
																			// is
																			// away
																			// or
																			// towards
					MapLocation dodge = rc.getLocation().add(dirToAvoid, rc.getType().strideRadius);
					moveToward(dodge);
					// System.out.println("I really dodged a BULLET there");
					break;
				}

			}
		}
		RobotInfo[] robots = rc.senseNearbyRobots(5);// i actually want ALL this
														// time, not just
														// enemies, but i only
														// want to be out of
														// melee range
		if (robots.length > 0)// if there are robots
		{
			for (int i = 0; i < robots.length; i++) {
				if (robots[i].getType() == RobotType.LUMBERJACK)// if any of
																// them are
																// lumberjacks
				{
					moveAwayFrom(robots[i].getLocation());// play keep away
				}
			}
		}

	}

	private static void pingGardeners() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			for (int i = 0; i < enemyRobots.length; i++) {
				if (enemyRobots[i].getType() == RobotType.GARDENER) {
					// get its location, broadcast
					MapLocation gardenerLocation = enemyRobots[i].getLocation();
					rc.broadcast(ENEMY_GARDENER_X, (int) gardenerLocation.x);
					rc.broadcast(ENEMY_GARDENER_Y, (int) gardenerLocation.y);
					break;
				}
			}
		}

	}

	private static void moveToward(MapLocation destination) throws GameActionException {
		// TODO intelligent pathfinding
		// check that we havent already moved
		if (rc.hasMoved()) {
			return;// we already moved, no point going further
		}
		Direction dir = rc.getLocation().directionTo(destination);
		if (rc.canMove(dir) && !rc.hasMoved()) {
			rc.move(dir);
			return;
		}
		if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(dir, 2), 1)
				|| !rc.onTheMap(rc.getLocation().add(dir, 2), 2)) {
			for (int i = 1; i < 100; i++) {
				// rc.setIndicatorDot(rc.getLocation().add(desiredDir, 2), 255,
				// 0, 0);
				// rc.setIndicatorLine(rc.getLocation(),
				// rc.getLocation().add(desiredDir), 255, 0, 0);//this turn
				if (rc.canMove(dir) && !rc.hasMoved()) {
					partMove(dir);
					// rc.move(dir);
					return;
					// return dir;
				} else {
					dir = dir.rotateLeftDegrees(i * 21);// rotate for the next
														// attempt
					// tryMove(moveLastTurnDir.rotateLeftDegrees(i*21));
				}
			}
		}

	}

	private static void partMove(Direction dir) throws GameActionException {
		float maxRange = rc.getType().strideRadius;
		// System.out.println("5 " + Clock.getBytecodeNum());
		for (float f = maxRange; f > 0; f = f - (float) 0.1) {
			if (rc.canMove(dir, f)) {
				rc.move(dir, f);
				break;
			}
		}

	}

	private static void moveAwayFrom(MapLocation target) throws GameActionException {
		// used to run away from stuff, figures out a position on sight range,
		// opposite of a target
		// then calls moveToward to try to get there
		Direction dirOfTarget = rc.getLocation().directionTo(target);
		Direction oppositeOfTarget = dirOfTarget.opposite();
		MapLocation targetLoc = rc.getLocation().add(oppositeOfTarget, rc.getType().sensorRadius);
		moveToward(targetLoc);
	}

	// when something that really shouldnt even be seeing enemies, sees enemies
	// has a higher response priority than other pings
	private static void panicPing() throws GameActionException {
		// before we start, clean up any old panics
		panicCleanup();
		// then see if there are any robots to actually panic about
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (robots.length > 0) {
			// get its location, broadcast
			MapLocation enemyLocation = robots[0].getLocation();
			rc.broadcast(PANIC_X, (int) enemyLocation.x);
			rc.broadcast(PANIC_Y, (int) enemyLocation.y);
		}
		// if we did panic cleanup here
		// the 2nd gardener, who sees nothing, could overwrite the
		// valid panic ping, of the 1st gardener
		// TODO is this appropriate
		if (robots.length > 0) {
			// spawn a scout to save us, or if we are rich a lumberjack
			Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0]);
			if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
				rc.buildRobot(RobotType.SOLDIER, dir);
			}
		}
	}

	// every 10 turns, clear the panic broadcasts,
	// to remove any dead pings from past panics
	private static void panicCleanup() throws GameActionException {
		if (rc.getRoundNum() % 10 == 0)// if round is evenly divisble by 10
		{
			rc.broadcast(PANIC_X, 0);// set x.y to 0
			rc.broadcast(PANIC_Y, 0);
		}
	}

	// if panic ping has been called, return the location of the call for help
	private static MapLocation findDistressedEcon() throws GameActionException {
		// read broadcast, if they are non-zero, make a maploc and send it
		int targetX = rc.readBroadcast(PANIC_X);
		int targetY = rc.readBroadcast(PANIC_Y);
		if (targetX != 0 || targetY != 0)// if either one of them is
											// non-zero(ill take the chance i
											// miss someone at 0.0)
		{
			MapLocation target = new MapLocation(targetX, targetY);
			return target;
		}
		return null;
	}

	// returns the maplocation of the nearest gardener on the enemy team
	private static MapLocation nearestEnemyGardener() {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for (int i = 0; i < robots.length; i++) {
			if (robots[i].getType() == RobotType.GARDENER) {
				return robots[i].getLocation();
			}
		}
		return null;
	}

	// move to and shake the nearest tree, if its shake-able
	private static void shakeTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		if (trees.length > 0)// theres one, get im
		{
			for (int i = 0; i < trees.length; i++) {
				if (trees[i].containedBullets > 0)// if it has bullets, i wanna
													// SHAKE em
				{
					if (rc.canShake(trees[i].ID)) {
						rc.shake(trees[i].ID);
					} else if (!rc.hasMoved()) {
						// System.out.println("moving to a tree to shake");
						moveToward(trees[i].getLocation());
						if (rc.canShake(trees[i].ID)) {
							rc.shake(trees[i].ID);
						}
					}
				}
			}
		}
	}

	// finds the nearest enemy robot and broadcasts its location(just the int
	// part)
	private static void pingEnemy() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		// If there are some...
		if (robots.length > 0) {
			// And we have enough bullets, and haven't attacked yet this turn...
			rc.broadcast(SPOTTED_ENEMY_X, (int) robots[0].getLocation().x);
			rc.broadcast(SPOTTED_ENEMY_Y, (int) robots[0].getLocation().y);
		}

	}

}
