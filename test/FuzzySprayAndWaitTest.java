package test;

import routing.FuzzySprayRouter;
import routing.MessageRouter;
import core.Message;
import core.SimScenario;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.RunListener;

/**
 * Tests for the FuzzySprayRouter routing module
 */
public class FuzzySprayAndWaitTest extends AbstractRouterTest
{

	private FuzzySprayRouter r1,r2,r3,r4;
	private static final int NROF_HOSTS = 10;
	private static final int NROF_COPIES = 10;
	private static final int MSMAX = 10;
	private static final int FTCMAX = 10;
	//private static final String filename="reports/logger_test.txt";


	@Override
	protected void setUp() throws Exception {
		ts.putSetting(MessageRouter.B_SIZE_S, ""+BUFFER_SIZE);
		ts.putSetting(SimScenario.SCENARIO_NS + "." + 
				SimScenario.NROF_GROUPS_S, "1");
		ts.putSetting(SimScenario.GROUP_NS + "." + 
				core.SimScenario.NROF_HOSTS_S, "" + NROF_HOSTS);
		//ts.putSetting(FuzzySprayRouter.FUZZYSPRAY_NS + "."+ FuzzySprayRouter.MSMAX, "" + NROF_COPIES);
		ts.putSetting(FuzzySprayRouter.FUZZYSPRAY_NS + "."+ FuzzySprayRouter.MSMAX, "" + MSMAX);
		ts.putSetting(FuzzySprayRouter.FUZZYSPRAY_NS + "."+ FuzzySprayRouter.FTCMAX, "" + FTCMAX);
		//ts.putSetting(FuzzySprayRouter.FUZZYSPRAY_NS + "."+ FuzzySprayRouter.FILENAME, filename);
		setRouterProto(new FuzzySprayRouter(ts));
		super.setUp();
		
		r1 = (FuzzySprayRouter)h1.getRouter();
		r2 = (FuzzySprayRouter)h2.getRouter();
		r3 = (FuzzySprayRouter)h3.getRouter();
		r4 = (FuzzySprayRouter)h4.getRouter();
	}
	
	
	public void testFTC() {
		int msgSize = 90;
		
		Message m1 = new Message(h1,h5, msgId1, msgSize);
		h1.createNewMessage(m1);
		checkCreates(1);
		h1.connect(h2);
		
		/* thresholds should be zero before any transfers */
		assertEquals((Integer)1, (Integer)m1.getProperty(FuzzySprayRouter.FTC_PROPERTY));
		
		/* simple delivery of msgId1 from h1 to h2 */
		updateAllNodes();
		checkTransferStart(h1, h2, msgId1);
		assertFalse(mc.next());
		clock.advance(5);
		updateAllNodes();
		assertFalse(mc.next()); // transfer should not be ready yet
		clock.advance(5); // now it should be done
		updateAllNodes();
		checkDelivered(h1, h2, msgId1, false);
		assertEquals((Integer)2, (Integer)((Message)(h1.getMessageCollection().toArray()[0])).getProperty(FuzzySprayRouter.FTC_PROPERTY));
		assertEquals((Integer)2, (Integer)((Message)(h2.getMessageCollection().toArray()[0])).getProperty(FuzzySprayRouter.FTC_PROPERTY));
		/*disconnect(h1);
		assertEquals(1, r1.calcThreshold());
		assertEquals(2, r2.calcThreshold());
		
		h2.connect(h3);
		deliverMessage(h2, h3, msgId1, msgSize, false);
		disconnect(h2);
		assertEquals(2, r2.calcThreshold());
		// msg at h3 has traveled 2 hops and "bsize > avgTransferredBytes > 0"
		// so threshold should be only msg's hopcount+1
		assertEquals(3, r3.calcThreshold());*/
	}
	/*
	public void testAckedMessageDeleting() {
		int msgSize = 10;
		Message m1 = new Message(h1,h5, msgId1, msgSize);
		h1.createNewMessage(m1);
		checkCreates(1);
		
		h1.connect(h2);
		deliverMessage(h1, h2, msgId1, msgSize, false);
		disconnect(h1);
		
		h1.connect(h3);
		deliverMessage(h1, h3, msgId1, msgSize, false);
		disconnect(h1);
		
		h1.connect(h5);
		deliverMessage(h1, h5, msgId1, msgSize, true);
		disconnect(h1);
		
		assertFalse(mc.next());
		h1.connect(h2); // h1 should notify h2 of the delivered msg
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(msgId1, mc.getLastMsg().getId());
		assertEquals(h2, mc.getLastFrom());
		// the deleted msg truly came from h1?
		assertEquals(h1, mc.getLastMsg().getHops().get(0));
		assertFalse(mc.next());
		
		// new msg to h3
		Message m2 = new Message(h3,h1, msgId2, msgSize);
		h3.createNewMessage(m2);
		checkCreates(1);
		
		h3.connect(h2); // h2 should notify h3 which should delete msgId1
		assertTrue(mc.next());
		assertEquals(mc.TYPE_DELETE, mc.getLastType());
		assertEquals(msgId1, mc.getLastMsg().getId());
		assertEquals(h3, mc.getLastFrom());
		assertFalse(mc.next());
		// msgId2 should NOT be deleted but it should be transferred to h2
		// during the next update
		deliverMessage(h3, h2, msgId2, msgSize, false);

	}
	
	public void testRouting() {
		int msgSize = 10;
		DTNHost th1 = utils.createHost(c0, "temp1");
		DTNHost th2 = utils.createHost(c0, "temp2");
		DTNHost th3 = utils.createHost(c0, "temp3");
		DTNHost th4 = utils.createHost(c0, "temp4");
		
		h4.connect(th1);
		h4.connect(h5);
		disconnect(h4);
		h4.connect(th1);
		disconnect(h4);
		// h4 should have probs: h5:0.25, th1:0.75
		
		h3.connect(th2);
		h3.connect(h4);
		disconnect(h3);
		// h3 probs: h4:0.5, th2:0.5
		// h4 probs: h3:0.5, th1:0.375, h5:0.125
		
		h2.connect(th3);
		disconnect(h2);
		h2.connect(h3);
		h2.connect(th3);
		disconnect(h2);
		h2.connect(th3);
		disconnect(h2);
		// h2 probs: th3:0.875, h3:0.125
		// h3 probs: h2:0.5, h4:0.25, th2:0.25
		
		h1.connect(th4);
		h1.connect(h2);
		disconnect(h1);
		// h1 probs: th4:0.5, h2:0.5
		// h2 probs: h1:0.5, th3:0.4375, h3:0.0625
		
		h4.connect(h5);
		disconnect(h4);
		// h4 probs: h3:0.25, th1:0.1875, h5:0.5625
		// these changes should not be visible to h3!
		Message m1 = new Message(h1,h5, msgId1, msgSize);
		h1.createNewMessage(m1);
		
		// msg with path h1 -> h2     -> h3       -> h4       -> h5
		double trueCost = (1-0.5) + (1-0.0625) + (1-0.25) + (1-0.125);
		double calcCost = r1.getCost(h1, h5);
		assertEquals(trueCost, calcCost);
	}
	///
	// Tests that more recent meeting probability sets replace older ones
	// but not vice versa.
	 //
	public void testMpsTimeStamps() {
		// create some messages so we can ask costs to destinations
		int msgIndx = 1;
		Message m1 = new Message(h1,h2, ""+msgIndx++, 1);
		h1.createNewMessage(m1);
		Message m2 = new Message(h1,h1, ""+msgIndx++, 1);
		h1.createNewMessage(m2);
		Message m3 = new Message(h1,h3, ""+msgIndx++, 1);
		h1.createNewMessage(m3);
		Message m4 = new Message(h3,h2, ""+msgIndx++, 1);
		h3.createNewMessage(m4);
		Message m5 = new Message(h3,h3, ""+msgIndx++, 1);
		h3.createNewMessage(m5);
		Message m6 = new Message(h4,h3, ""+msgIndx++, 1);
		h4.createNewMessage(m6);
		Message m7 = new Message(h4,h4, ""+msgIndx++, 1);
		h4.createNewMessage(m7);
		Message m8 = new Message(h2,h2, ""+msgIndx++, 1);
		h2.createNewMessage(m8);
		Message m9 = new Message(h2,h4, ""+msgIndx++, 1);
		h2.createNewMessage(m9);
		
		h1.connect(h2);
		disconnect(h1);
		// now we should have
		// h1': h2:1.0; h2:h2'
		// h2': h1:1.0; h1:h1'
		
		assertEquals(0.0, r1.getCost(h1, h2));
		assertEquals(0.0, r1.getCost(h2, h1));		
		
		clock.advance(1.0);
		h1.connect(h3);
		disconnect(h1);
		// h1'': h2:0.5, h3:0.5; h2:h2', h3:h3'
		// h3': h1:1.0; h1:h1'', h2:h2'
		
		assertEquals(0.5, r1.getCost(h1, h2));
		// h3 received h1's other probs properly?
		assertEquals(0.5, r3.getCost(h1, h2));
		assertEquals(0.5, r3.getCost(h1, h3));
		
		clock.advance(1.0);
		h1.connect(h4);
		disconnect(h1);
		// h1''': h2:0.25, h3:0.25, h4:0.5; h2:h2', h3:h3', h4:h4'
		// h4': h1:1.0; h1:h1''', h2:h2', h3:h3'
	
		assertEquals(0.75, r4.getCost(h1, h3));
		assertEquals(0.5, r4.getCost(h1, h4));
		
		clock.advance(1.0);
		h2.connect(h3);
		disconnect(h2);
		// h2'': h1:0.5, h3:0.5; h1:h1'', h3:h3'' (both from h3)
		// h3'': h1:0.5,  h2:0.5; h1:h1'', h2:h2'' (h1's probs should remain)
		
		assertEquals(0.5, r2.getCost(h1, h2)); // test the received h1''
		assertEquals(0.5, r3.getCost(h1, h2)); // is h1'' also still in h3?
		
		clock.advance(1.0);
		h1.connect(h2);
		disconnect(h1);
		// h1'''': h2:0.625, h3:0.125, h4:0.25; h2:h2''', h3:h3'', h4:h4'
		// h2''': h1:0.75, h3:0.25; h1:h1'''', h2:h3'', h4:h4'
		
		// msg path h2->h1->h4
		assertEquals((1-0.75)+(1-0.25), r2.getCost(h2, h4));
	}
	*/
	public static void main(String[] args)
	{
		JUnitCore core= new JUnitCore();
		RunListener listener= new TextListener(new RealSystem());
		core.addListener(listener);
		core.run(FuzzySprayAndWaitTest.class);
	}

}