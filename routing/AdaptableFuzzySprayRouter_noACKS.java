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
import report.FuzzyComprehensiveReport;
import report.FuzzySprayReport;
//import sun.security.action.GetIntegerAction;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class AdaptableFuzzySprayRouter_noACKS extends EnergyAwareRouter {

		/** SprayAndWait router's settings name space ({@value})*/
		public static final String FUZZYSPRAY_NS = "FuzzySprayRouter";

	public static final String FTC_PROPERTY = FUZZYSPRAY_NS + "." + "ftc";


	protected int FTCmax;
	protected int MSmax;

		protected Set<Integer> known_nodes;

	public AdaptableFuzzySprayRouter_noACKS(Settings s) throws IOException {
		super(s);
	FTCmax=0;
	MSmax=0;
		known_nodes=new HashSet<Integer>();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected AdaptableFuzzySprayRouter_noACKS(AdaptableFuzzySprayRouter_noACKS r) {
		super(r);
	this.FTCmax=r.FTCmax;
	this.MSmax=r.MSmax;
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

	msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);
			
		return msg;
	}

	@Override
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message less_priority = null;
		double least_priority=1;
		FTCComparator f=new FTCComparator();
		for (Message m : messages) {
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			if (less_priority == null ) {
				less_priority = m;
				//least_priority=f.getPriority(less_priority);
			}
			else if (least_priority > f.getPriority(m)) {
				less_priority = m;
				least_priority=f.getPriority(less_priority);
			}
		}
		return less_priority;
	}

	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
	msg.addProperty(FTC_PROPERTY, (Integer)1);
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
			if (ml instanceof FuzzySprayReport )
			    ((FuzzySprayReport)ml).calculateStatistics(current_time);
						else if (ml instanceof FuzzyComprehensiveReport)
							((FuzzyComprehensiveReport)ml).calculateStatistics(current_time);

		    }
		    
		}

		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryOtherMessages();
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
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}


	msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);
		/* was the message delivered to the final recipient? */
		if (msg.getTo() == con.getOtherNode(getHost())) {
			this.deleteMessage(msg.getId(), false); // delete from buffer
		 //       System.out.println("DD");

		}
	   

	}

    	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * hop counts and their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	protected Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();
	
		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			AdaptableFuzzySprayRouter_noACKS othRouter = (AdaptableFuzzySprayRouter_noACKS)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				/* skip messages that the other host has or that have
				 * passed the other host */
				if (othRouter.hasMessage(m.getId()) ||
						m.getHops().contains(other)) {
					continue;
				}
				messages.add(new Tuple<Message, Connection>(m,con));
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		/* sort the message-connection tuples according to the criteria
		 * defined in FTCComparator */
		Collections.sort(messages,new FTCComparator());


	  
     
		return tryMessagesForConnected(messages);
	}

    public /*static*/ class FTCComparator implements Comparator<Tuple<Message, Connection> > {

  
		private /*static*/ int compute_fuzzy(int CDM, int size)
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
		public /*static*/ int getPriority(Message m)
		{
			return compute_fuzzy((Integer)m.getProperty(FTC_PROPERTY),(Integer)m.getSize());
		}
		public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
		
	    return (getPriority(t1.getKey())-getPriority(t2.getKey()));

	}

    }

	@Override
	public AdaptableFuzzySprayRouter_noACKS replicate() {
		return new AdaptableFuzzySprayRouter_noACKS(this);
	}
}
