/*
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.Tuple;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import report.FuzzySprayReport;
//import sun.security.action.GetIntegerAction;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class AdaptableFuzzySprayAndWaitRouter extends EnergyAwareRouter {
		/** identifier for the logger file setting ({@value})*/
		public static final String NROF_COPIES = "nrofCopies";
		/** identifier for the binary-mode setting ({@value})*/
		public static final String BINARY_MODE = "binaryMode";
		/** SprayAndWait router's settings name space ({@value})*/
		public static final String FUZZYSPRAY_NS = "FuzzySprayAndWaitRouter";

		/** IDs of the messages that are known to have reached the final dst */
        protected Set<String> ackedMessageIds;

        public static final String FTC_PROPERTY = FUZZYSPRAY_NS + "." + "ftc";
		public static final String MSG_COUNT_PROPERTY = FUZZYSPRAY_NS + "." +"copies";


        protected int FTCmax;
        protected int MSmax;

		protected Set<Integer> known_nodes;

		protected int initialNrofCopies;
		protected boolean isBinary;
                protected int bufferSizeBefore;
                protected int bufferSizeAfter;

	public AdaptableFuzzySprayAndWaitRouter(Settings s) throws IOException {
		super(s);
		Settings snwSettings = new Settings(FUZZYSPRAY_NS);


        FTCmax=0;
        MSmax=0;
		initialNrofCopies = snwSettings.getInt(NROF_COPIES);
		isBinary = snwSettings.getBoolean( BINARY_MODE);
        ackedMessageIds = new HashSet<String>();
		known_nodes=new HashSet<Integer>();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected AdaptableFuzzySprayAndWaitRouter(AdaptableFuzzySprayAndWaitRouter r) {
		super(r);
		this.FTCmax=r.FTCmax;
        this.MSmax=r.MSmax;
        this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
        ackedMessageIds = new HashSet<String>();
		known_nodes=new HashSet<Integer>();
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		if (m!=null)
		{
			if (m.getSize()>MSmax)
				MSmax=m.getSize();//maximum size = maximum known size
			for (DTNHost h : m.getHops())
				if (!known_nodes.contains(h.getAddress()))
					known_nodes.add(h.getAddress());
			FTCmax=(int) ((double) known_nodes.size() * 0.1);//10% of known nodes
		}
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);

		assert nrofCopies != null : "Not a FSnW message: " + msg;

		if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
		else {
			/* in standard S'n'W the receiving node gets only single copy */
			nrofCopies = 1;
		}

		/* was this node the final recipient of the message? */
		if (isDeliveredMessage(msg)) {
			this.ackedMessageIds.add(id);
		}
                msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);
				msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

		return msg;
	}


	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
                msg.addProperty(FTC_PROPERTY, (Integer)1);
				msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		addToMessages(msg, true);

		return true;
	}

	@Override
	public void update() {
		super.update();
                double current_time=SimClock.getTime();

                if(current_time-FuzzySprayReport.lastReportTime>=FuzzySprayReport.reportInterval)
                {
                    FuzzySprayReport.lastReportTime=current_time;

                    for (MessageListener ml:mListeners)
                    {
                        if (ml instanceof FuzzySprayReport)
                            ((FuzzySprayReport)ml).calculateStatistics(current_time);

                    }

                }

		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}
                bufferSizeBefore=getMessageCollection().size();

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
                 bufferSizeAfter=getMessageCollection().size();

                for (MessageListener ml:mListeners)
                {
                        if (ml instanceof FuzzySprayReport)
                                ((FuzzySprayReport)ml).bufferSize(getHost(), bufferSizeBefore,bufferSizeAfter);

                }
	}


	/**
	 * Called just before a transfer is finalized (by
	 * {@link ActiveRouter#update()}).
	 * Reduces the number of copies we have left for a message.
	 * In binary Spray and Wait, sending host is left with floor(n/2) copies,
	 * but in standard mode, nrof copies left is reduced by one.
	 */
	@Override
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}

		/* reduce the amount of copies left */
		nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		if (isBinary) {
			nrofCopies /= 2;
		}
		else {
			nrofCopies--;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);

		/* was the message delivered to the final recipient? */
		if (msg.getTo() == con.getOtherNode(getHost())) {
			this.ackedMessageIds.add(msg.getId()); // yes, add to ACKed messages
			this.deleteMessage(msg.getId(), false); // delete from buffer
		}



	}

    	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * hop counts and their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	protected void/*Tuple<Message, Connection>*/ tryOtherMessages() {
		/*List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();*/

		List<Message> msgCollection=getMessagesWithCopiesLeft();

		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		/*for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			FuzzySprayAndWaitRouter othRouter = (FuzzySprayAndWaitRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				/// skip messages that the other host has or that have
				 //passed the other host
				if (othRouter.hasMessage(m.getId()) ||
						m.getHops().contains(other)) {
					continue;
				}
				messages.add(new Tuple<Message, Connection>(m,con));
			}


		}*/

		/*if (messages.size() == 0) {
			return null;
		}*/

		/* sort the message-connection tuples according to the criteria
		 * defined in FTCComparator */

		Collections.sort(msgCollection,new FTCComparator1());

		if (msgCollection.size() > 0) {
			/* try to send those messages */
			this.tryMessagesToConnections(msgCollection, getConnections());
		}
               


		//return tryMessagesForConnected(messages);
	}

		/**
	 * Creates and returns a list of messages this router is currently
	 * carrying and still has copies left to distribute (nrof copies > 1).
	 * @return A list of messages that have copies left
	 */
	protected List<Message> getMessagesWithCopiesLeft() {
		List<Message> list = new ArrayList<Message>();

		for (Message m : getMessageCollection()) {
			Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
			assert nrofCopies != null : "FSnW message " + m + " didn't have " +
				"nrof copies property!";
			if (nrofCopies > 1) {
				list.add(m);
			}
		}

		return list;
	}

    public class FTCComparator implements Comparator<Tuple<Message, Connection> > {


		private int compute_fuzzy(int CDM, int size)
		{
                       // System.out.println("CDM: "+CDM);
			int BS0 = 0;
			int BS1 = 2;
			int BS2 = 3;
			int BS3 = 4;
			int BS4 = 5;
			int BS5 = 6;
			int BS6 = 7;
			int BS7 = 8;
			int BS8 = 10;

			String FTC=null;
			String MS=null;
			int BS=0;
			int P;

			if (CDM <= FTCmax/3) FTC = "low";
			else if (CDM >= (FTCmax*2)/3) FTC = "high";
			else FTC = "medium";

			//Message size membership function
			if (size < MSmax/4) MS = "small";
			else if (size > (MSmax*3)/4) MS = "large";
			else MS = "medium";

                        //System.out.println("FTC: "+FTC);
                        //System.out.println("MS: "+MS);
			//Inference rules and Defuzzification using Center of Area (COA)
			if (FTC.equals("low") && MS.equals("small")) BS = BS0;
			else if (FTC.equals("low") && MS.equals("medium")) BS = BS1;
			else if (FTC.equals("low") && MS.equals("large")) BS = BS2;
			else if (FTC.equals("medium") && MS.equals("small")) BS = BS3;
			else if (FTC.equals("medium") && MS.equals("medium")) BS = BS4;
			else if (FTC.equals("medium") && MS.equals("large")) BS = BS5;
			else if (FTC.equals("high") && MS.equals("small")) BS = BS6;
			else if (FTC.equals("high") && MS.equals("medium")) BS = BS7;
			else if (FTC.equals("high") && MS.equals("large")) BS = BS8;

			//Setting the priority of the message
			P = 10-BS;

                       // System.out.println("P: "+P);
			return P;
		}
		public int getPriority(Message m)
		{
			return compute_fuzzy((Integer)m.getProperty(FTC_PROPERTY),(Integer)m.getSize());
		}
		public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {

            return (getPriority(t1.getKey())-getPriority(t2.getKey()));

        }

    }

	public class FTCComparator1 implements Comparator<Message > {


		private int compute_fuzzy(int CDM, int size)
		{
                       // System.out.println("CDM: "+CDM);
			int BS0 = 0;
			int BS1 = 2;
			int BS2 = 3;
			int BS3 = 4;
			int BS4 = 5;
			int BS5 = 6;
			int BS6 = 7;
			int BS7 = 8;
			int BS8 = 10;

			String FTC=null;
			String MS=null;
			int BS=0;
			int P;

			if (CDM <= FTCmax/3) FTC = "low";
			else if (CDM >= (FTCmax*2)/3) FTC = "high";
			else FTC = "medium";

			//Message size membership function
			if (size < MSmax/4) MS = "small";
			else if (size > (MSmax*3)/4) MS = "large";
			else MS = "medium";

                        //System.out.println("FTC: "+FTC);
                        //System.out.println("MS: "+MS);
			//Inference rules and Defuzzification using Center of Area (COA)
			if (FTC.equals("low") && MS.equals("small")) BS = BS0;
			else if (FTC.equals("low") && MS.equals("medium")) BS = BS1;
			else if (FTC.equals("low") && MS.equals("large")) BS = BS2;
			else if (FTC.equals("medium") && MS.equals("small")) BS = BS3;
			else if (FTC.equals("medium") && MS.equals("medium")) BS = BS4;
			else if (FTC.equals("medium") && MS.equals("large")) BS = BS5;
			else if (FTC.equals("high") && MS.equals("small")) BS = BS6;
			else if (FTC.equals("high") && MS.equals("medium")) BS = BS7;
			else if (FTC.equals("high") && MS.equals("large")) BS = BS8;

			//Setting the priority of the message
			P = 10-BS;

                       // System.out.println("P: "+P);
			return P;
		}
		public int getPriority(Message m)
		{
			return compute_fuzzy((Integer)m.getProperty(FTC_PROPERTY),(Integer)m.getSize());
		}

		public int compare(Message m1, Message m2) {
			return (getPriority(m2)-getPriority(m1));
		}

    }


	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) { // new connection
			if (con.isInitiator(getHost())) {
				// initiator performs all the actions on behalf of the
				// other node too (so that the meeting probs are updated
				// for both before exchanging them)
				DTNHost otherHost = con.getOtherNode(getHost());
				MessageRouter mRouter = otherHost.getRouter();

				assert mRouter instanceof AdaptableFuzzySprayAndWaitRouter : "FuzzySprayAndWaitRouter only works "+
				" with other routers of same type";
				AdaptableFuzzySprayAndWaitRouter otherRouter = (AdaptableFuzzySprayAndWaitRouter)mRouter;

				// exchange ACKed message data
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
			}
		}
	}

    /**
	 * Deletes the messages from the message buffer that are known to be ACKed
	 */
	protected void deleteAckedMessages() {
		for (String id : this.ackedMessageIds) {
			if (this.hasMessage(id) && !isSending(id)) {
				this.deleteMessage(id, false);
			}
		}
	}

	@Override
	public AdaptableFuzzySprayAndWaitRouter replicate() {
		return new AdaptableFuzzySprayAndWaitRouter(this);
	}
}
