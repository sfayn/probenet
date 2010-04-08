/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import java.util.Vector;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReportSpecial extends Report implements MessageListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
	
	/**
	 * Constructor.
	 */
	public MessageStatsReportSpecial() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		this.nrofAborted++;
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() - 
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);
			
			if (m.isResponse()) {
				this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}
	
         private class normal_statistics
        {
            		int created;
			int started;
			int relayed;
			int aborted;
			int dropped;
			int removed;
			int delivered;
			double delivery_prob;
			double response_prob;
			double overhead_ratio;
			double latency_avg;
			double latency_med;
			double hopcount_avg;
			double hopcount_med;
			double buffertime_avg;
			double buffertime_med;
			double rtt_avg;
			double rtt_med;


        };

        Vector<normal_statistics> statistics_vector =new Vector <normal_statistics>(100);//adjust to number of simulation hours

	public void calculateStatistics(double time) {
		//write("Message stats for scenario " + getScenarioName() +
		//		"\nsim_time: " + format(getSimTime()));
                normal_statistics n_stats=new normal_statistics();
		double deliveryProb = 0; // delivery probability
		double responseProb = 0; // request-response success probability
		double overHead = Double.NaN;	// overhead ratio

		if (this.nrofCreated > 0) {
			deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		if (this.nrofDelivered > 0) {
			overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
				this.nrofDelivered;
		}
		if (this.nrofResponseReqCreated > 0) {
            		responseProb = (1.0* this.nrofResponseDelivered) /
				this.nrofResponseReqCreated;
		}

                n_stats.created= this.nrofCreated ;
                n_stats.started= this.nrofStarted ;
                n_stats.relayed= this.nrofRelayed ;
                n_stats.aborted= this.nrofAborted ;
                n_stats.dropped= this.nrofDropped ;
                n_stats.removed= this.nrofRemoved ;
                n_stats.delivered= this.nrofDelivered ;
                n_stats.delivery_prob= deliveryProb ;
                n_stats.response_prob= responseProb ;
                n_stats.overhead_ratio= overHead ;
                n_stats.latency_avg= Double.parseDouble(getAverage(this.latencies)) ;
                n_stats.latency_med= Double.parseDouble(getMedian(this.latencies)) ;
                n_stats.hopcount_avg= Double.parseDouble(getIntAverage(this.hopCounts)) ;
                n_stats.hopcount_med= getIntMedian(this.hopCounts) ;
                n_stats.buffertime_avg= Double.parseDouble(getAverage(this.msgBufferTime)) ;
                n_stats.buffertime_med= Double.parseDouble(getMedian(this.msgBufferTime)) ;
                n_stats.rtt_avg= Double.parseDouble(getAverage(this.rtt)) ;
                n_stats.rtt_med=Double.parseDouble(getMedian(this.rtt));

                statistics_vector.add(n_stats);

		//write(statsText);
		//super.done();
	}

	@Override
	public void done() {

               

                write("Message stats for scenario " + getScenarioName() +
				"\nsim_time: " + format(getSimTime()));
                
                String output="";
                output=output.concat("created ");

                for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.created)+" ");
                    }
                output=output.concat("\nstarted ");
                for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.started)+" ");
                    }
                output=output.concat("\nrelayed ");
                for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.relayed)+" ");
                    }
                output=output.concat("\naborted ");
                for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.aborted)+" ");
                    }
                output=output.concat("\ndropped ");
                for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.dropped)+" ");
                    }
                output=output.concat("\nremoved ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.removed)+" ");
                    }
                output=output.concat("\ndelivered ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.delivered)+" ");
                    }
                output=output.concat("\ndelivery_prob ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.delivery_prob)+" ");
                    }
                output=output.concat("\nresponse_prob ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.response_prob)+" ");
                    }
                output=output.concat("\noverhead_ratio ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.overhead_ratio)+" ");
                    }
                output=output.concat("\nlatency_avg ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.latency_avg)+" ");
                    }
                output=output.concat("\nlatency_med ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.latency_med)+" ");
                    }
                output=output.concat("\nhopcount_avg ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.hopcount_avg)+" ");
                    }
                output=output.concat("\nhopcount_med ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.hopcount_med)+" ");
                    }
                output=output.concat("\nbuffertime_avg ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.buffertime_avg)+" ");
                    }
                output=output.concat("\nbuffertime_med ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.buffertime_med)+" ");
                    }
                output=output.concat("\nrtt_avg ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.rtt_avg)+" ");
                    }
                 output=output.concat("\nrtt_med ");
                 for (normal_statistics os:statistics_vector)
                    {
                    output=output.concat(format(os.rtt_med)+" ");
                    }



                output=output.concat("\n");
		
		write(output);
		super.done();
	}
	
}
