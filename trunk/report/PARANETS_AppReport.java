package report;

import applications.PARANETS_application;
import core.Application;
import core.ApplicationListener;
import core.DTNHost;
import core.Message;
import core.SimClock;
import java.util.HashMap;
import java.util.Map;
import routing.ParanetAdaptableFuzzySprayAndWaitRouter;

/**
 * Reporter for the <code>PARANETS_application</code>.
 * 
 * @author Jad Makhlouta
 */
public class PARANETS_AppReport extends Report implements ApplicationListener
{

	private double max(double d0, double d1, double d2) {
		if (d0>=d1 && d0>=d2)
			return d0;
		if (d1>=d0 && d1>=d2)
			return d1;
		else
			return d2;
	}
	public class Request
	{
		public double cost_for_request=0;
		public double cost_for_delivery=0;
		public int data_size=0;
		public int [] reached_size=new int[3];

		public double start_time=-1;
		public String request_interface;
		public double request_reached_time=-1;
		public double data_sent_time=-1;
		public double [] data_reached_time= new double[3];
		public String [] data_interface =new String[3];

		public int reached_size()
		{
			return reached_size[0]+reached_size[1]+reached_size[2];
		}

		public boolean reached()
		{
			int error=2;
			return (reached_size()+error>=data_size && reached_size()-error<=data_size);
		}

	}
	public static double getCost(int size, String interface_)
	{
		if (interface_.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN))
			return size* PARANETS_application.wlan_cost;
		else if (interface_.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR))
			return size* PARANETS_application.cellular_cost;
		else if (interface_.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE))
			return size* PARANETS_application.satellite_cost;
		else
			assert true: "Illigal interface type "+ interface_;
		return 0;//not needed will not reach here
	}

	public Map<String,Request> stats=new HashMap<String,Request>();
	public String [] last_delivered=new String[3];

	public void gotEvent(String event, Object params, Application app,
			DTNHost host) {
		// Check that the event is sent by correct application type
		if (!(app instanceof PARANETS_application)) return;
		//write(event+ ":"+params+" >> "+host);
		// Increment the counters based on the event type
		Message msg=(Message)params;
		if (event.equalsIgnoreCase("GotRequest")) {
			String id=msg.getId();
			Request current=stats.get(id);
			current.request_reached_time=SimClock.getTime();
			current.request_interface=(String)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			current.cost_for_request=getCost(msg.getSize(),current.request_interface);
			stats.put(id, current);
		}
		else if (event.equalsIgnoreCase("SentRequest")) {
			Request current=new Request();
			String id=msg.getId();
			current.start_time=SimClock.getTime();
			stats.put(id, current);
		}
		else if (event.equalsIgnoreCase("GotData")) {
			String id=msg.getId().split("-")[0];
			Request current=stats.get(id);
			String inter=(String)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			int index=-1;
			if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN))
				index=0;
			else if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR))
				index=1;
			else if (inter.equals(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE))
				index=2;
			else
				assert true: "Illigal interface type "+ inter;
			current.data_interface[index]=inter;
			current.data_reached_time[index]=SimClock.getTime();
			current.reached_size[index]=msg.getSize();
			current.cost_for_delivery=getCost(msg.getSize(), inter);
			((PARANETS_application)app).conditions[index].sensed((SimClock.getTime()-current.data_sent_time)/msg.getSize());
			if (current.reached())
				last_delivered[index]=id;
			stats.put(id, current);
		}
		else if (event.equalsIgnoreCase("SentData")) {
			String id=msg.getId().split("-")[0];
			Request current=stats.get(id);
			current.data_sent_time=SimClock.getTime();
			current.data_size+=msg.getSize();
			stats.put(id, current);
		}
	}

	@Override
	public void done() {
		write(getScenarioName() +
				"\nsim_time: " + format(getSimTime()));
		int total_delay=0;
		int total_reached=0;
		int total_sent=0;
		int total_cost=0;

		for (Request r: stats.values())
		{
			if (r.reached())
			{
				total_reached++;
				total_delay+=max(r.data_reached_time[0],r.data_reached_time[1],r.data_reached_time[2])-r.start_time;
				total_cost+=r.cost_for_delivery+r.cost_for_request;
			}
			total_sent++;
		}

		write( "probability_of_total_delivery\t" + (double)total_reached/total_sent);
		write( "average_cost_for_delivered\t" + (double)total_cost/total_reached);
		write( "average_delay_for_delivered\t" + (double)total_delay/total_reached); 
		super.done();
	}
}
