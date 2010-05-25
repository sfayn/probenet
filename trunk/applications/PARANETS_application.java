/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package applications;

import java.util.Random;

import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;
import routing.ParanetAdaptableFuzzySprayAndWaitRouter;

/**
 * Paranets Application
 * 
 * The corresponding <code>PARANETS_AppReport</code> class can be used to record
 * information about the application behavior.
 * 
 * @see PARANETS_AppReport
 * @author Jad Makhlouta
 */
public class PARANETS_application extends Application {
	/** MA or SA */
	public static final String NODE_ROLE = "role";
	/** REQUEST generation interval */
	public static final String REQUEST_INTERVAL = "interval";
	/** SA address  */
	public static final String SA_ADDRESS = "SA_Address";
	/** Seed for the app's random number generator */
	public static final String SEED = "seed";
	/** Size of the request message */
	public static final String REQUEST_SIZE = "requestSize";
	/** Size of the data messages */
	public static final String MESSAGES_RANGE = "MessageSizeRanges";

	public static final String COST_PER_SIZE = "CostsPerTechnology";

	public static final String NOMINAL_TH="NominalThroughput";

	public static final String TYPE_PROPERTY = "PARANET_type";
	public static final String TYPE_REQUEST = "request";
	public static final String TYPE_DATA = "data";
	
	/** Application ID */
	public static final String APP_ID = "jh.PARANETS_Application";

    public class network_conditions
	{

		private class fuzzy_parameters
		{
			public double Tmin=0;
			public double Tmax=10;//just example might be changed
		}

		private class throughputMeasurements{
			public double throughput;
			public double stamp;

			public throughputMeasurements(double nominal_TH){
			    throughput=nominal_TH;
			    stamp=SimClock.getTime();
			}
		}

		private fuzzy_parameters params;


   //     private double stampSensed;
	private throughputMeasurements throughputSensedEstimate;
  //      private double stampShared;
	private throughputMeasurements throughputEstimate;
	private throughputMeasurements throughputShared;
		
		public void setInitially(double nominal_TH)
		{
			throughputSensedEstimate =new throughputMeasurements(nominal_TH);
			throughputEstimate =new throughputMeasurements(nominal_TH);
			throughputShared=new throughputMeasurements(nominal_TH);
			params =new fuzzy_parameters();
		}

		public void sensed(double sensed_TH)
		{
			double time_diff=SimClock.getTime()-throughputSensedEstimate.stamp;
			
			params=getFuzzyParams();
			throughputSensedEstimate.throughput=getNewEstimate();
			throughputSensedEstimate.stamp=SimClock.getTime(); //for next time
		}
		public void shared(double shared_TH)
		{
			double time_diff=SimClock.getTime()-throughputShared.stamp;
			
			params=getFuzzyParams();
			throughputEstimate.throughput=getNewEstimate();
			throughputShared.stamp=SimClock.getTime(); //for next time
		}

		private fuzzy_parameters getFuzzyParams() {
			throw new UnsupportedOperationException("Not yet implemented");
		}

		private double getNewEstimate() {
			throw new UnsupportedOperationException("Not yet implemented");
		}

		public double getTH_estimate()
		{
			return throughputEstimate.throughput;
		}
		public double last_stamp()
		{
			return (throughputSensedEstimate.stamp>throughputShared.stamp?throughputSensedEstimate.stamp:throughputShared.stamp);
		}
	}

	public network_conditions [] conditions =new network_conditions[3];;

	// Private vars
	private double	lastRequest = 0;
	private double	min_interval = 500;
	private double	max_interval = 600;
	private double  current_interval=min_interval;
	private boolean SA = false;
	private int		seed = 0;
/*	private int		MA_Min=1;
	private int		MA_Max=59;*/
	static public int	SA_ID=0;
	static public double wlan_cost=0;
	static public double cellular_cost=0;
	static public double satellite_cost=0;
	private int		requestSize=1;
	private int		minSize=100;
	private int		maxSize=200;
	private Random	rng;
	private int msg_count=0;
	private double wlan_nominal=1000000;
	private double cellular_nominal=10000000;
	private double satellite_nominal=8000000;

	
	/** 
	 * Creates a new PARANETS Application with the given settings.
	 * 
	 * @param s	Settings to use for initializing the application.
	 */
	public PARANETS_application(Settings s) {
		if (s.contains(NODE_ROLE)){
			if ( s.getSetting(NODE_ROLE).equalsIgnoreCase("SA"))
				SA=true;
			else if ( s.getSetting(NODE_ROLE).equalsIgnoreCase("MA"))
				SA=false;
			else
				assert false: "INVALID SETTING "+NODE_ROLE;
		}
		if (s.contains(REQUEST_INTERVAL)){
			int[] interval = s.getCsvInts(REQUEST_INTERVAL,2);
			this.min_interval = interval[0];
			this.max_interval = interval[1];
			this.current_interval=this.min_interval;
		}
		if (s.contains(SEED)){
			this.seed = s.getInt(SEED);
		}
		if (s.contains(REQUEST_SIZE)) {
			this.requestSize = s.getInt(REQUEST_SIZE);
		}
		if (s.contains(MESSAGES_RANGE)) {
			int[] size = s.getCsvInts(MESSAGES_RANGE,2);
			this.minSize = size[0];
			this.maxSize = size[1];
		}
		if (s.contains(COST_PER_SIZE)){
			double[] cost = s.getCsvDoubles(COST_PER_SIZE,3);
			PARANETS_application.wlan_cost = cost[0];
			PARANETS_application.cellular_cost = cost[1];
			PARANETS_application.satellite_cost = cost[2];
		}
		/*if (s.contains(MA_RANGE)){
			int[] range = s.getCsvInts(MA_RANGE,2);
			this.MA_Min = range[0];
			this.MA_Max = range[1];
		}*/
		if (s.contains(SA_ADDRESS)){
			PARANETS_application.SA_ID = s.getInt(SA_ADDRESS);
		}

		if (s.contains(NOMINAL_TH)){
			double[] nominals = s.getCsvDoubles(NOMINAL_TH,3);
			for (int i=0;i<this.conditions.length;i++)
			{
				this.conditions[i]=new network_conditions();
				conditions[i].setInitially(nominals[i]);
			}
			wlan_nominal=nominals[0];
			cellular_nominal=nominals[1];
			satellite_nominal=nominals[2];
		}

		rng = new Random(this.seed);
		super.setAppID(APP_ID);
	}
	
	/** 
	 * Copy-constructor
	 * 
	 * @param a
	 */
	public PARANETS_application(PARANETS_application a) {
		super(a);
		this.lastRequest = a.lastRequest;
		this.min_interval = a.min_interval;
		this.max_interval = a.max_interval;
		this.SA = a.SA;
		this.seed = a.seed;
		/*this.MA_Min=a.MA_Min;
		this.MA_Max=a.MA_Max;*/
		//this.SA_ID=a.SA_ID;
		this.requestSize=a.requestSize;
		this.minSize=a.minSize;
		this.maxSize=a.maxSize;
		this.current_interval=a.current_interval;
		this.rng = new Random(this.seed);
		this.msg_count=0;

		for (int i=0;i<this.conditions.length;i++)
			this.conditions[i]=new network_conditions();
		this.conditions[0].setInitially(wlan_nominal);
		this.conditions[1].setInitially(cellular_nominal);
		this.conditions[2].setInitially(satellite_nominal);
	}
	
	/** 
	 * Handles an incoming message. 
	 * 
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty(TYPE_PROPERTY);
		if (type==null) return msg; // Not a PARANET message
		
		// Respond with data if role is SA
		if (msg.getTo()==host && type.equalsIgnoreCase(TYPE_REQUEST))
		{
			assert (SA);
			//String interf=(String)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			Integer wlan_size = (Integer)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN);
			Integer cellular_size = (Integer)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR);
			Integer satellite_size = (Integer)msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE);
			assert (wlan_size!=null && cellular_size !=null && satellite_size !=null);
			String id = msg.getId()+"-data";
			int [] sizes  ={wlan_size,cellular_size,satellite_size};
			String [] interfaces={ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN,
									ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR,
									ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE};
			super.sendEventToListeners("GotRequest",msg, msg.getFrom());
			//int size=0;
			for (int i=0;i<sizes.length;i++)
			{
				Message m = new Message(host, msg.getFrom(), id+"-"+interfaces[i], sizes[i]);
				m.addProperty(TYPE_PROPERTY, TYPE_DATA);
				m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY, interfaces[i]);
				m.setAppID(APP_ID);
				host.createNewMessage(m);
				super.sendEventToListeners("SentData", m, msg.getFrom());
				//size+=sizes[i];
			}
			//super.sendEventToListeners("SentData", ""+size, msg.getFrom());
		}
		
		// Received a data reply
		if (msg.getTo()==host && type.equalsIgnoreCase(TYPE_DATA) ) {
			assert(!SA);
			//String interface_=(String) msg.getProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY);
			super.sendEventToListeners("GotData", msg, host);
		}
		return msg;
	}

	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost randomHost(int min,int max) {
		int destaddr = 0;
		if (max == min) {
			destaddr = min;
		}
		else
		{
			destaddr = min + rng.nextInt(max - min);
		}
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(destaddr);
	}

	private double random_in_range(double min, double max) {
		if (max == min) {
			return min;
		}
		else if (max>min)
		{
			return min + rng.nextDouble()*(max - min);
		}
		else
		{
			assert max>=min: "Invalid range";
			return -1;
		}
	}

	private int random_in_range(int min, int max) {
		if (max == min) {
			return min;
		}
		else if (max>min)
		{
			return min + rng.nextInt(max - min);
		}
		else
		{
			assert max>=min: "Invalid range";
			return -1;
		}
	}

	@Override
	public Application replicate() {
		return new PARANETS_application(this);
	}

	/** 
	 * Sends a request for data packet if this is an MA application instance.
	 * 
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
		if (SA) return;
		double curTime = SimClock.getTime();
		if (curTime - this.lastRequest >= this.current_interval) {
			// Time to send a new request
			msg_count++;
			Message m = new Message(host, randomHost(SA_ID,SA_ID), host.getAddress()+"."+msg_count,	random_in_range(minSize,maxSize));
			m.addProperty(TYPE_PROPERTY, TYPE_REQUEST);
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_PROPERTY, ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR); //can be changed later (means that request sent on the cellualar channel)
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_WLAN, (Integer)(m.getSize()/3));//just for testing
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_CELLULAR, (Integer)(m.getSize()/3));//just for testing
			m.addProperty(ParanetAdaptableFuzzySprayAndWaitRouter.INTERFACE_SATELLITE, (Integer)(m.getSize()/3));//just for testing
			m.setAppID(APP_ID);
			host.createNewMessage(m);
			
			// Call listeners
			super.sendEventToListeners("SentRequest", m, host);
			
			this.lastRequest = curTime;
			this.current_interval=random_in_range(min_interval, max_interval); //next interval
		}
	}
};
