package strategies;

import java.util.*;

import automail.*;
import exceptions.*;

public class MyMailPool implements IMailPool{
	/* Makes much more sense to use PriorityQueues here for the pools, that
     * way, we can access the mail by sorted priority and sorted arrival time
     * rather than simply the order in which they are generated. In addition,
     * a stack is the last thing I would use for this; we want the mail to be
     * FIFO, not LIFO. We are creating a queue with the heavy and a
     * queue with the light mail items for each type of mail in order to make
     * it easier to give mail to the weaker robot. Finally, we are adding our
     * custom comparator by either priority or arrival time for each of the
     * priorityQueues.
     */
	PriorityQueue<MailItem> nonPriorityPoolHeavy =
            new PriorityQueue <MailItem>(10,
                    new Comparator<MailItem>() {
                public int compare(MailItem m1, MailItem m2) {
                    if (m1.getArrivalTime() > m2.getArrivalTime()) {
                        return -1;
                    }
                    if (m1.getArrivalTime() > m2.getArrivalTime()) {
                        return 1;
                    }
                    return 0;
                }
            });
    
    PriorityQueue<MailItem> nonPriorityPoolLight =
            new PriorityQueue <MailItem>(10,
                    new Comparator<MailItem>() {
                public int compare(MailItem m1, MailItem m2) {
                    if (m1.getArrivalTime() > m2.getArrivalTime()) {
                        return -1;
                    }
                    if (m1.getArrivalTime() > m2.getArrivalTime()) {
                        return 1;
                    }
                    return 0;
                }
            });
    
    PriorityQueue<PriorityMailItem> priorityPoolHeavy =
            new PriorityQueue <PriorityMailItem>(10,
                    new Comparator<PriorityMailItem>() {
                public int compare(PriorityMailItem m1, PriorityMailItem m2) {
                    if (m1.getPriorityLevel() > m2.getPriorityLevel()) {
                        return -1;
                    }
                    if (m1.getPriorityLevel() > m2.getPriorityLevel()) {
                        return 1;
                    }
                    return 0;
                }
            });
    
    PriorityQueue<PriorityMailItem> priorityPoolLight =
            new PriorityQueue <PriorityMailItem>(10,
                    new Comparator<PriorityMailItem>() {
                public int compare(PriorityMailItem m1, PriorityMailItem m2) {
                    if (m1.getPriorityLevel() > m2.getPriorityLevel()) {
                        return -1;
                    }
                    if (m1.getPriorityLevel() > m2.getPriorityLevel()) {
                        return 1;
                    }
                    return 0;
                }
            });
	private static final int MAX_WEIGHT = 2000;
	private Robot robot1, robot2, robot3;
    private Robot[] robots = new Robot[3];

	public MyMailPool(){
		/* Already instantiated the queues at the top, so now we simply add the
         * robots to the robots array.
         */
        robots[0] = robot1;
        robots[1] = robot2;
        robots[2] = robot3;
	}

	public void addToPool(MailItem mailItem) {
		// Check whether it has a priority or not
		if(mailItem instanceof PriorityMailItem){
			if (mailItem.getWeight() < MAX_WEIGHT) {
				// Add to light priority items
				priorityPoolLight.add((PriorityMailItem) mailItem);
			}
			else {
				// Otherwise add to light priority items
				priorityPoolHeavy.add((PriorityMailItem) mailItem);
			}
		}
		else{
			if (mailItem.getWeight() < MAX_WEIGHT) {
				// Add to light items
				nonPriorityPoolLight.add(mailItem);
			}
			else {
				// Otherwise, add to heavy items
				nonPriorityPoolHeavy.add(mailItem);
			}
		}
	}
	
	private int getNonPriorityPoolHeavySize() {
		return nonPriorityPoolHeavy.size();
	}
	
	private int getNonPriorityPoolLightSize(){
		return nonPriorityPoolLight.size();
	}
	
	private int getPriorityPoolHeavySize(){
		return priorityPoolHeavy.size();
	}
	
	private int getPriorityPoolLightSize(){
		return priorityPoolLight.size();
	}

	private MailItem getNonPriorityMailHeavy(){
		if(getNonPriorityPoolHeavySize() > 0){
			return nonPriorityPoolHeavy.remove();
		}
		else{
			return null;
		}
	}
	
	private MailItem getNonPriorityMailLight(){
		if(getNonPriorityPoolLightSize() > 0){
			return nonPriorityPoolLight.remove();
		}
		else{
			return null;
		}
	}
	
	private MailItem getHighestPriorityMailHeavy(){
		if(getPriorityPoolHeavySize() > 0){
			return priorityPoolHeavy.remove();
		}
		else{
			return null;
		}
		
	}
	
	private MailItem getHighestPriorityMailLight(){
		if(getPriorityPoolLightSize() > 0){
			return priorityPoolLight.remove();
		}
		else{
			return null;
		}
		
	}
	
	@Override
	public void step() {
		for(Robot r : robots) {
			if (r != null) fillStorageTube(r);
		}
	}
	
	public static Comparator<MailItem> MailItemFloorComp = new Comparator<MailItem>() {

		public int compare(MailItem m1, MailItem m2) {
		   int floor1 = m1.getDestFloor();
		   int floor2 = m2.getDestFloor();

		   // sorting function
		   return floor1 - floor2;
	    }};
	
	private void fillStorageTube(Robot robot) {
		ArrayList<MailItem> temp = new ArrayList<MailItem>();
        StorageTube tube = robot.getTube();
        /* First, we will take the priority items, if there are any. If the
         * robot is strong, it will focus on the mail in this order:
         * priorityHeavy, priorityLight, nonPriorityHeavy, nonPriorityLight.
         * This is because only the strong robots can focus on the Heavy,
         * and also because I noticed no difference when accounting for
         * priority according to a function of currentTime - arrivalTime
         * compared to priority for nonPriority vs Priority items.
         * 
         * We will place all MailItems into a temporary array before putting
         * them in the tube. This allows us to sort them by destination floor
         * before placing them in the LIFO stack that is the robot's storage
         * tube.
         */
        try{
        	// First deal with Strong robots, as stated above
            if (robot.isStrong()) {
            	// look to see if there is any priorityHeavy Mail first
            	if (getPriorityPoolHeavySize() > 0) {
            		// Add all priorityHeavy items
                    while(temp.size() < 4 && getPriorityPoolHeavySize() > 0) {
                        temp.add(getHighestPriorityMailHeavy());
                    }
                }
            	// Now check on priorityLight Mail
            	if (getPriorityPoolLightSize() > 0) {
            		// Add all priorityLight items
                    while(temp.size() < 4 && getPriorityPoolLightSize() > 0) {
                        temp.add(getHighestPriorityMailLight());
                    }
                }
            	// Check on nonPriorityHeavy Mail
                if (getNonPriorityPoolHeavySize() > 0) {
                	// Add all of that
                    while(temp.size() < 4 && getNonPriorityPoolHeavySize() > 0) {
                        temp.add(getNonPriorityMailHeavy());
                    }
                }
                // Finally, check on nonPriorityLight Mail
                if (getNonPriorityPoolLightSize() > 0) {
                	// Add all of that
                    while(temp.size() < 4 && getNonPriorityPoolLightSize() > 0) {
                        temp.add(getNonPriorityMailLight());
                    }
                }
                
                /* Now we will sort the MailItems in the temporary array by
                 * their destination floor.
                 */
                Collections.sort(temp, MailItemFloorComp);
                
                for (MailItem m : temp) {
                	tube.addItem(m);
                }
               
                temp.clear();
                
                if (tube.getSize() > 0) robot.dispatch();
            }
            /* If the Robot is not strong, follow the same steps as above,
             * albeit without any heavy items.
             */
            else {
                if (getPriorityPoolLightSize() > 0) {
                	while(temp.size() < 4 && getPriorityPoolLightSize() > 0) {
                        temp.add(getHighestPriorityMailLight());
                    }
                }
                if (getNonPriorityPoolLightSize() > 0) {
                	while(temp.size() < 4 && getNonPriorityPoolLightSize() > 0) {
                        temp.add(getNonPriorityMailLight());
                    }
                }
                
                /* Now we will sort the MailItems in the temporary array by
                 * their destination floor.
                 */
                Collections.sort(temp, MailItemFloorComp);
                
                for (MailItem m : temp) {
                	tube.addItem(m);
                }
               
                temp.clear();
                
                if (tube.getSize() > 0) robot.dispatch();
            }
        }
        catch(TubeFullException e){
            e.printStackTrace();
        }
    }

	@Override
	public void registerWaiting(Robot robot) {
		for (int i = 0; i < robots.length; i++) {
			if (robots[i] == null) {
				robots[i] = robot;
				break;
			}
		}
	}

	@Override
	public void deregisterWaiting(Robot robot) {
		for (int i = 0; i < robots.length; i++) {
			if (robots[i] == robot) {
				robots[i] = null;
				break;
			}
		}
		
	}
}
