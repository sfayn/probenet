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

                double priority=0;
		int copies_in_network=0;
		int dropped_copies=0;
		int removed_copies=0;

		message_info(double start,double start_priority)
		{

			starting_time=start;
                        priority=start_priority;

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
					" priority: "+priority+ " copies_in_network: "+copies_in_network+
					" copies_dropped: "+ dropped_copies+" copies_removed: "+removed_copies;
		}

	};
        private int priorities_count=10;
        private class output_statistics
        {
            
            double [] av_latency=new double[priorities_count];
            double [] av_copies=new double [priorities_count];
            double [] av_dropped=new double [priorities_count];
            double [] av_removed=new double [priorities_count];
            int [] not_reached=new int [priorities_count];
            int [] current_total=new int [priorities_count];
            double time;

        };
	Vector<message_info> messages=new Vector<message_info>(10000); //adjust to number of messages ids
        Vector<output_statistics> statistics =new Vector <output_statistics>(100);//adjust to number of simulation hours

	int num_of_nodes=0;
	double sum_message_count=0;

	public FuzzySprayReport() {
		super();
		messages.add(0,null);
                //statistica.add(0,null);
	}

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		super.messageDeleted(m,where,dropped);
		if (isWarmupID(m.getId())) {
			return;
		}
		int i=Integer.parseInt(m.getId().substring(1));
		message_info info=messages.get(i);
		if (dropped) {
			info.dropped_copies++;
		}
		else {
			info.removed_copies++;
		}
		messages.set(i,info);
	}

	@Override
	public void newMessage(Message m) {
		super.newMessage(m);
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
                int messID=Integer.parseInt(m.getId().substring(1));
                double priority=getScenarioName().equals("FuzzySpray")?FuzzySprayRouter.FTCComparator.getPriority(m):0.6;
		messages.add(messID,new message_info(getSimTime(),priority));

	}

	public void bufferSize(DTNHost host, int size)
	{
              //  System.out.println("Buffer");
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

		if (finalTarget) {
				info.finishing_time=getSimTime();
		}
		messages.set(i,info);
	}

        public void calculateStatistics(double time){
                System.out.println("entered stats");
                double [] sum_average_latency=new double[priorities_count];
		int [] sum_in_network=new int[priorities_count];
		int [] sum_dropped=new int[priorities_count];
		int [] sum_removed=new int[priorities_count];
		int [] num_reached=new int[priorities_count];
		int [] not_reach=new int[priorities_count];

                for (int j=0;j<priorities_count;j++)
		{
			sum_average_latency[j]=0;
			num_reached[j]=0;
			not_reach[j]=0;
			sum_in_network[j]=0;
			sum_removed[j]=0;
			sum_dropped[j]=0;
		}

                for (int i=1;i<messages.size();i++)
		{
			message_info m=messages.get(i);
			for (int j=0;j<priorities_count;j++)
			{
				if (m.priority>j/(double)priorities_count && m.priority<=(j+1)/(double)priorities_count )
				{
					sum_in_network[j]+=m.copies_in_network;
					sum_dropped[j]+=m.dropped_copies;
					sum_removed[j]+=m.removed_copies;
					if (m.reached())
					{
						sum_average_latency[j]+=m.latency();
						num_reached[j]++;
					}
					else
						not_reach[j]++;
					break;
				}
			}
		}

                output_statistics statistics_instance=new output_statistics();
                statistics_instance.time=time;
		for (int j=0;j<priorities_count;j++)
		{
                       
                        statistics_instance.current_total[j]=num_reached[j]+not_reach[j];
                        statistics_instance.av_latency[j]=sum_average_latency[j]/(double)num_reached[j];
                        statistics_instance.av_copies[j]=sum_in_network[j]/(double)statistics_instance.current_total[j];
                        statistics_instance.av_dropped[j]=sum_dropped[j]/(double)statistics_instance.current_total[j];
                        statistics_instance.av_removed[j]=sum_removed[j]/(double)statistics_instance.current_total[j];
                        statistics_instance.not_reached[j]=not_reach[j];

		}

                statistics.add(statistics_instance);

        }


@Override
	public void done() {
                calculateStatistics(43200);//adjust to total simulation time
		
               String output="";
               System.out.println("size of stats: "+statistics.size());

               if (getScenarioName().equals("FuzzySpray"))

               {
                       output=output.concat("---------Additional Stats--------\n");
                       output=output.concat("simulation_times ");

                        for (output_statistics os:statistics)
                        {
                            output=output.concat(format(os.time)+" ");
                        }
                        output=output.concat("\n");

                        for (int j=0;j<priorities_count;j++)
                        {
                           
                                output=output.concat ("P [ "+ format(j/(double)priorities_count)+" "+format((j+1)/(double)priorities_count)+" ] ");

                                output=output.concat("av_latency ");
                                for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.av_latency[j])+" ");
                                    }
                                output=output.concat("av_copies ");
                                for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.av_copies[j])+" ");
                                    }
                                output=output.concat("av_dropped ");
                                for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.av_dropped[j])+" ");
                                    }
                                output=output.concat("av_removed ");
                                for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.av_dropped[j])+" ");
                                    }
                                output=output.concat("not_reached ");
                                for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.not_reached[j])+" ");
                                    }
                                output=output.concat("out_of ");
                                 for (output_statistics os:statistics)
                                    {
                                    output=output.concat(format(os.current_total[j])+" ");
                                    }
                                output=output.concat("\n");
                            }
                }
       
                
		output=output.concat("--------------Normal Stats-------------------\n");
                write(output);
		super.done();
	}
}
