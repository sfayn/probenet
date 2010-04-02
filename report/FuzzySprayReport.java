/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package report;

import core.DTNHost;
import core.Message;
import java.util.LinkedList;
import java.util.Vector;
import routing.FuzzySprayRouter;

/**
 *
 * @author Jad Makhlouta
 */
public class FuzzySprayReport extends MessageStatsReport {

	private class message_info
	{
		public int hop_count=0;
		public double starting_time=-1;
		public double finishing_time=-1;
		LinkedList<Double> priorities=new LinkedList<Double>();
		int copies_in_network=0;
		int dropped_copies=0;
		int removed_copies=0;

		message_info(double start)
		{
			starting_time=start;
		}
		public double average_priority()
		{
			double sum=0;
			for(int i=0;i< priorities.size();i++)
				sum+=priorities.get(i);
			return sum/priorities.size();
		}
		public double latency()
		{
			return (reached()?finishing_time-starting_time:-1);
		}
		public boolean reached()
		{
			return (finishing_time>starting_time);
		}
		@Override
		public String toString()
		{
			return "hop_count: "+hop_count+ (reached()?" latency: "+format(latency()):" did_not_reach")+ 
					" average_priority: "+format(average_priority())+ " copies_in_network: "+copies_in_network+
					" copies_dropped: "+ dropped_copies+" copies_removed: "+removed_copies;
		}

	};

	Vector<message_info> messages=new Vector<message_info>(1000);
	int num_of_nodes=0;
	double sum_message_count=0;

	public FuzzySprayReport() {
		super();
		messages.add(0,null);
	}

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		super.messageDeleted(m,where,dropped);
		if (isWarmupID(m.getId())) {
			return;
		}
		message_info info=messages.get(Integer.parseInt(m.getId().substring(1)));
		if (dropped) {
				info.dropped_copies++;
		}
		else {
			info.removed_copies++;
		}
	}

	@Override
	public void newMessage(Message m) {
		super.newMessage(m);
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}

		messages.add(Integer.parseInt(m.getId().substring(1)),new message_info(getSimTime()));
	}

	public void bufferSize(DTNHost host, int size)
	{
		sum_message_count+=size;
		num_of_nodes++;
	}

	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to,	boolean finalTarget) {
		super.messageTransferred(m, from, to, finalTarget);
		if (isWarmupID(m.getId())) {
			return;
		}

		int i=Integer.parseInt(m.getId().substring(1));
		message_info info=messages.get(i);
		info.hop_count++;
		info.copies_in_network++;
		info.priorities.add(from.getRouter() instanceof FuzzySprayRouter?FuzzySprayRouter.FTCComparator.getPriority(m):0.6);
		messages.set(i,info);

		if (finalTarget) {
				info.finishing_time=getSimTime();
				messages.set(i,info);
		}
	}

@Override
	public void done() {
		write("---------Additional Stats for " + getScenarioName()+"--------");
		double [] sum_average_latency=new double[10];
		int [] sum_in_network=new int[10];
		int [] sum_dropped=new int[10];
		int [] sum_removed=new int[10];
		int [] num_reached=new int[10];
		int [] not_reach=new int[10];
		int len=sum_average_latency.length;
		for (int j=0;j<len;j++)
		{
			sum_average_latency[j]=0;
			num_reached[j]=0;
			not_reach[j]=0;
			sum_in_network[j]=0;
			sum_dropped[j]=0;
		}
		for (int i=1;i<messages.size();i++)
		{
			message_info m=messages.get(i);
			for (int j=0;j<len;j++)
			{
				double av=m.average_priority();
				if (av>j/(double)len && av<=(j+1)/(double)len)
				{
					if (m.reached())
					{
						sum_average_latency[j]+=m.latency();
						num_reached[j]++;
						break;
					}
					else
						not_reach[j]++;
					sum_in_network[j]+=m.copies_in_network;
					sum_dropped[j]+=m.dropped_copies;
					sum_removed[j]+=m.removed_copies;
				}
			}

		}
		int dropped=0,removed=0;
		int in_net=0;
		int /*total_reached=0,*/total=0;
		for (int j=0;j<len;j++)
		{
			dropped+=sum_dropped[j];
			removed+=sum_removed[j];
			in_net+=sum_in_network[j];
			//total_reached+=num_reached[j];
			total+=num_reached[j]+not_reach[j];
			assert(total==messages.size());
			if (getScenarioName().equals("FuzzySprayRouter"))
			{
				write(" for priority ["+format(j/(double)len) + "," +format((j+1)/(double)len)+"] average latency "
					+format(sum_average_latency[j]/(double)num_reached[j])+" av_copies "+format(sum_in_network[j]/(double)(num_reached[j]+not_reach[j]))
					+" av_dropped "+format(sum_dropped[j]/(double)(num_reached[j]+not_reach[j]))+" av_removed "+format(sum_removed[j]/(double)(num_reached[j]+not_reach[j]))
					+" not reached "+not_reach[j]+" from "+(not_reach[j]+num_reached[j]));
			}
		}
		write("\naverage number of messages per node "+format(sum_message_count/(double)num_of_nodes)+"\n"+
				"\naverage copies/message "+format(in_net/(double)total)+
				"\naverage dropped/message "+format(dropped/(double)total)+
				"\naverage removed/message "+format(removed/(double)total)+"\n");
		/*write("--------------Details------------------------\n");
		for (int i=1;i<messages.size();i++)
		{
			write(i+" "+messages.get(i).toString());
		}*/
		write("--------------Normal Stats-------------------\n");
		super.done();
	}
}
