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
import core.Settings;
import core.SimClock;
import core.Tuple;
import java.io.File;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import report.FuzzySprayReport;
//import sun.security.action.GetIntegerAction;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class FuzzySprayRouter extends ActiveRouter {

    /** identifier for the ftcmax setting ({@value})*/
       public static final String FTCMAX = "ftcmax";
      /** identifier for the msmax setting ({@value})*/
       public static final String MSMAX = "msmax";
	   /** identifier for the logger file setting ({@value})*/
       //public static final String FILENAME = "filename";

	/** SprayAndWait router's settings name space ({@value})*/
	public static final String FUZZYSPRAY_NS = "FuzzySprayRouter";
	/** IDs of the messages that are known to have reached the final dst */
    protected Set<String> ackedMessageIds;

    public static final String FTC_PROPERTY = FUZZYSPRAY_NS + "." + "ftc";


        protected static int FTCmax;
        protected static int MSmax;
		/*protected static File file;
		protected static FileWriter logger;*/

	public FuzzySprayRouter(Settings s) throws IOException {
		super(s);
		Settings snwSettings = new Settings(FUZZYSPRAY_NS);


        FTCmax=snwSettings.getInt(FTCMAX);
        MSmax=snwSettings.getInt(MSMAX);
        ackedMessageIds = new HashSet<String>();
		/*file= new File(snwSettings.getSetting(FILENAME));
		if (!file.exists())
			file.createNewFile();
		else
		{
			file.delete();
			file.createNewFile();
		}
		file.setWritable(true);
		logger =new FileWriter(file);
		*/
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FuzzySprayRouter(FuzzySprayRouter r) {
		super(r);
	
        //this.FTCmax=r.FTCmax;
        //this.MSmax=r.MSmax;
        ackedMessageIds = new HashSet<String>();
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);

		/* was this node the final recipient of the message? */
		if (isDeliveredMessage(msg)) {
			this.ackedMessageIds.add(id);
		}
                msg.updateProperty(FTC_PROPERTY, (Integer)msg.getProperty(FTC_PROPERTY)+1);
				/*try {
					FTCComparator r=new FTCComparator();
					logger.write("Message " + msg.getId() +(msg.getTo()==getHost()?" reached " : " relayed_to ")+getHost().toString()+ " at "+SimClock.getTime()+ " of_priority "+r.getPriority(msg)+"\n");
					logger.flush();
				} catch (IOException ex) {
					Logger.getLogger(FuzzySprayRouter.class.getName()).log(Level.SEVERE, null, ex);
				}*/
		return msg;
	}


	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
                msg.addProperty(FTC_PROPERTY, (Integer)1);
		addToMessages(msg, true);
		/*try {
			logger.write("Message " + msg.getId() + " created_at "+getHost().toString()+ " at "+SimClock.getTime()+"\n");
			logger.flush();
		} catch (IOException ex) {
			Logger.getLogger(FuzzySprayRouter.class.getName()).log(Level.SEVERE, null, ex);
		}*/
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
			this.ackedMessageIds.add(msg.getId()); // yes, add to ACKed messages
			this.deleteMessage(msg.getId(), false); // delete from buffer
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

		if (mListeners.get(0) instanceof FuzzySprayReport)
			((FuzzySprayReport)mListeners.get(0)).bufferSize(getHost(), messages.size());
		/*try {
			logger.write("number_of_messages_at " +getHost().toString() +" "+ messages.size()+"\n");
			logger.flush();
		} catch (IOException ex) {
			Logger.getLogger(FuzzySprayRouter.class.getName()).log(Level.SEVERE, null, ex);
		}*/
              //System.out.println("number of messages: "+messages.size());
		return tryMessagesForConnected(messages);
	}

    public static class FTCComparator implements Comparator<Tuple<Message, Connection> > {

     //   /*static */int FTCmax=10; //setting the fuzzy membership function (FTC) range - Global constants
	//	/*static */int MSmax=1000; //setting the fuzzy membership function (message size) range - Global constants

		private static double compute_fuzzy(int CDM, int size)
		{
			double BS0 = 0;
			double BS1 = 0.2;
			double BS2 = 0.3;
			double BS3 = 0.4;
			double BS4 = 0.5;
			double BS5 = 0.6;
			double BS6 = 0.7;
			double BS7 = 0.8;
			double BS8 = 1;

			String FTC=null;
			String MS=null;
			double BS=0;
			double P;
			/*try {
				logger.write("CDM:"+CDM+"\n");
				logger.write("size:"+size+"\n");
				logger.write("FTCMAX:"+FTCmax+"\n");
				logger.write("MSMAX:"+MSmax+"\n");
				logger.flush();
			} catch (IOException ex) {
				Logger.getLogger(FuzzySprayRouter.class.getName()).log(Level.SEVERE, null, ex);
			}*/
                        //System.out.println("CDM:"+CDM);
                        //System.out.println("size:"+size);
                        //System.out.println("FTCMAX:"+FTCmax);
                        //System.out.println("MSMAX:"+MSmax);
			//FTC membership function
			if (CDM < FTCmax/3) FTC = "low";
			else if (CDM > (FTCmax*2)/3) FTC = "high";
			else FTC = "medium";
		   // System.out.print("FTC="+FTC+"\n");

			//Message size membership function
			if (size < MSmax/4) MS = "small";
			else if (size > (MSmax*3)/4) MS = "large";
			else MS = "medium";
		   // System.out.print("MS="+MS+"\n");

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
			P = 1-BS;
			/*try {
				logger.write("Priority:" + P + "\n");
				logger.flush();
			} catch (IOException ex) {
				Logger.getLogger(FuzzySprayRouter.class.getName()).log(Level.SEVERE, null, ex);
			}*/

                        //System.out.println("Priority:"+P);

			return P;
		}
		public static double getPriority(Message m)
		{
			return compute_fuzzy((Integer)m.getProperty(FTC_PROPERTY),(Integer)m.getSize());
		}
		public int compare(Tuple<Message, Connection> t1, Tuple<Message, Connection> t2) {
                   // System.out.println("Size:"+(Integer)t1.getKey().getSize());
                  //  System.out.println("FTC:"+(Integer)t1.getKey().getProperty(FTC_PROPERTY));
            return (int)(10.0*(getPriority(t1.getKey())-getPriority(t2.getKey())));

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

				assert mRouter instanceof FuzzySprayRouter : "FuzzySprayRouter only works "+
				" with other routers of same type";
				FuzzySprayRouter otherRouter = (FuzzySprayRouter)mRouter;

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
	public FuzzySprayRouter replicate() {
		return new FuzzySprayRouter(this);
	}
}
