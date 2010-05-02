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
import core.UpdateListener;
import java.util.Vector;

/**
 * Comprehensive report for use in Fuzzy Routers
 */
public class FuzzyComprehensiveReport extends FuzzyEnergyReport implements UpdateListener , MessageListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofDelivered;

	private List<Double> delays;

	public FuzzyComprehensiveReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.delays = new ArrayList<Double>();
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofDelivered = 0;
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
		}
		if (finalTarget && !isWarmupID(m.getId())) {
			this.delays.add(getSimTime() - m.getCreationTime());
		}
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) { }

	public void messageDeleted(Message m, DTNHost where, boolean dropped) {	}

	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) { }
	
	private class normal_statistics
	{
		double delivery_prob;
		double overhead_ratio;
		//double latency_avg;
	};

	Vector<normal_statistics> statistics_vector =new Vector <normal_statistics>(100);//adjust to number of simulation hours

	public void calculateStatistics(double time) {
		normal_statistics n_stats=new normal_statistics();
		double deliveryProb = 0; // delivery probability
		double overHead = Double.NaN;	// overhead ratio

		if (this.nrofCreated > 0) {
			deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		if (this.nrofDelivered > 0) {
			overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /this.nrofDelivered;
		}
		n_stats.delivery_prob= deliveryProb ;
		n_stats.overhead_ratio= overHead ;
		//n_stats.latency_avg= Double.parseDouble(getAverage(this.latencies)) ;
		statistics_vector.add(n_stats);
	}

	@Override
	public void done() {
		calculateStatistics(43200);//adjust to total simulation time
		write(/*"Message stats for scenario " +*/ getScenarioName() +	"\nsim_time\t" + format(getSimTime()));

		String output="";
		output=output.concat("time\t");
		for (int i=1;i<=statistics_vector.size();i++)
		{
			output=output.concat(i+"\t");
		}
		output=output.concat("\ndelivery_prob\t");
		for (normal_statistics os:statistics_vector)
		{
			output=output.concat(format(os.delivery_prob)+"\t");
		}
		output=output.concat("\noverhead_ratio\t");
		for (normal_statistics os:statistics_vector)
		{
			output=output.concat(format(os.overhead_ratio)+"\t");
		}
		/*output=output.concat("\nlatency_avg\t");
		for (normal_statistics os:statistics_vector)
		{
			output=output.concat(format(os.latency_avg)+"\t");
		}*/
		write(output);
		/*
		if (delays.size() == 0) {
			write("----------");
			write("none_delivered");
			write("----------");
			super.done();
			return;
		}*/
		double cumProb = 0, total_delay=0; // cumulative probability

		java.util.Collections.sort(delays);
		String delay_s="latency",cumProb_s="cum_prob",average_delay_s="av_latency";

		for (int i=0; i < delays.size(); i++) {
			total_delay+=delays.get(i);
			cumProb+=1.0/nrofCreated;
			cumProb_s = cumProb_s+"\t"+ format(cumProb);
			delay_s=delay_s+"\t"+format(delays.get(i));
			average_delay_s=average_delay_s+"\t"+ format(total_delay/(i+1));
		}
		write("----------");
		write(cumProb_s);
		write(delay_s);
		write(average_delay_s);
		write("----------");
		super.done();
	}
	
}
