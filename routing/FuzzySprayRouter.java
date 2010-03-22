/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class FuzzySprayRouter extends ActiveRouter {
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value})*/ 
	public static final String BINARY_MODE = "binaryMode";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String FUZZYSPRAY_NS = "SprayAndWaitRouter";
	/** IDs of the messages that are known to have reached the final dst */
    protected Set<String> ackedMessageIds;
    /** Message property key */
	public static final String MSG_COUNT_PROPERTY = FUZZYSPRAY_NS + "." +
		"copies";
    public static final String FTC_PROPERTY = FUZZYSPRAY_NS + "." + "ftc";
	
	protected int initialNrofCopies;
	protected boolean isBinary;

	public FuzzySprayRouter(Settings s) {
		super(s);
		Settings snwSettings = new Settings(FUZZYSPRAY_NS);
		
		initialNrofCopies = snwSettings.getInt(NROF_COPIES);
		isBinary = snwSettings.getBoolean( BINARY_MODE);
        ackedMessageIds = new HashSet<String>();
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FuzzySprayRouter(FuzzySprayRouter r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		this.isBinary = r.isBinary;
        ackedMessageIds = new HashSet<String>();
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		
		assert nrofCopies != null : "Not a SnW message: " + msg;
		
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
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);
		return msg;
	}


	@Override 
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
        msg.addProperty(FTC_PROPERTY, (Integer)0);
		addToMessages(msg, true);
		return true;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		/* create a list of SAWMessages that have copies left to distribute */
		/*@SuppressWarnings(value = "unchecked")
		List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

		if (copiesLeft.size() > 0) {
			// try to send those messages
			this.tryMessagesToConnections(copiesLeft, getConnections());
		}*/
		
		tryOtherMessages();
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
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		//Collection<Message> msgCollection = getMessageCollection();
		List<Message> msgCollection=getMessagesWithCopiesLeft();
		
		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			FuzzySprayRouter othRouter = (FuzzySprayRouter)other.getRouter();
			
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
    
    private class FTCComparator implements Comparator<Tuple<Message, Connection> > {

        public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
            int ftc_compare=(Integer)t1.getKey().getProperty(FTC_PROPERTY)-(Integer)t2.getKey().getProperty(FTC_PROPERTY);
            if (ftc_compare==0)
                return t1.getKey().getSize()-t2.getKey().getSize();
            else
                return ftc_compare;
        }
        
    }
    @Override
	public void changedConnection(Connection con) {
		if (con.isUp()) { // new connection
			if (con.isInitiator(getHost())) {
				/* initiator performs all the actions on behalf of the
				 * other node too (so that the meeting probs are updated
				 * for both before exchanging them) */
				DTNHost otherHost = con.getOtherNode(getHost());
				MessageRouter mRouter = otherHost.getRouter();

				assert mRouter instanceof FuzzySprayRouter : "FuzzySprayRouter only works "+
				" with other routers of same type";
				FuzzySprayRouter otherRouter = (FuzzySprayRouter)mRouter;

				/* exchange ACKed message data */
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
	private void deleteAckedMessages() {
		for (String id : this.ackedMessageIds) {
			if (this.hasMessage(id) && !isSending(id)) {
				this.deleteMessage(id, false);
			}
		}
	}

	@Override
	public FuzzySprayRouter replicate() {
		return new FuzzySprayRouter(this);
	}
}
