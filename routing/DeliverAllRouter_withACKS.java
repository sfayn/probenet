/*
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.io.IOException;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
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
public class DeliverAllRouter_withACKS extends EnergyAwareRouter {

	/** IDs of the messages that are known to have reached the final dst */
	protected Set<String> ackedMessageIds;

	public DeliverAllRouter_withACKS(Settings s) throws IOException {
		super(s);
	ackedMessageIds = new HashSet<String>();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected DeliverAllRouter_withACKS(DeliverAllRouter_withACKS r) {
		super(r);
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
			
		return msg;
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

		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		this.tryAllMessagesToAllConnections();
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
		/* was the message delivered to the final recipient? */
		if (msg.getTo() == con.getOtherNode(getHost())) {
			this.ackedMessageIds.add(msg.getId()); // yes, add to ACKed messages
			this.deleteMessage(msg.getId(), false); // delete from buffer
		 //       System.out.println("DD");

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

				assert mRouter instanceof DeliverAllRouter_withACKS : "DeliverAllRouter_withACKS only works "+
				" with other routers of same type";
				DeliverAllRouter_withACKS otherRouter = (DeliverAllRouter_withACKS)mRouter;

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
	public DeliverAllRouter_withACKS replicate() {
		return new DeliverAllRouter_withACKS(this);
	}
}
