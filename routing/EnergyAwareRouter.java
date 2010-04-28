/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.Random;
import core.*;

/**
 * Energy level-aware variant of Epidemic router.
 */
public class EnergyAwareRouter extends ActiveRouter 
		implements ModuleCommunicationListener{
	/** Initial units of energy -setting id ({@value}). Can be either a 
	 * single value, or a range of two values. In the latter case, the used
	 * value is a uniformly distributed random value between the two values. */
	public static final String INIT_ENERGY_S = "intialEnergy";
	/** Energy usage per scanning -setting id ({@value}). */
	public static final String SCAN_ENERGY_S = "scanEnergy";
	/** Energy usage per second when sending -setting id ({@value}). */
	public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
	/** Energy usage per second when receiving -setting id ({@value}). */
	public static final String RECEIVE_ENERGY_S = "receiveEnergy";//I added this previously no energy loss on receive

	/** Energy update warmup period -setting id ({@value}). Defines the 
	 * simulation time after which the energy level starts to decrease due to 
	 * scanning, transmissions, etc. Default value = 0. If value of "-1" is 
	 * defined, uses the value from the report warmup setting 
	 * {@link report.Report#WARMUP_S} from the namespace 
	 * {@value report.Report#REPORT_NS}. */
	public static final String WARMUP_S = "energyWarmup";

	/** {@link ModuleCommunicationBus} identifier for the "current amount of 
	 * energy left" variable. Value type: double */
	public static final String ENERGY_VALUE_ID = "Energy.value";
	
	private final double[] initEnergy;
	private double warmupTime;
	private double currentEnergy;
	/** energy usage per scan */
	private double scanEnergy;
	private double transmitEnergy;
	private double receiveEnergy;//I added this previously no energy loss on receive
	private double lastScanUpdate;
	private double time_start_receive;
	private double lastUpdate;
	private double scanInterval;	
	private ModuleCommunicationBus comBus;
	private static Random rng = null;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EnergyAwareRouter(Settings s) {
		super(s);
		this.initEnergy = s.getCsvDoubles(INIT_ENERGY_S);
		
		if (this.initEnergy.length != 1 && this.initEnergy.length != 2) {
			throw new SettingsError(INIT_ENERGY_S + " setting must have " + 
					"either a single value or two comma separated values");
		}
		
		this.scanEnergy = s.getDouble(SCAN_ENERGY_S);
		this.transmitEnergy = s.getDouble(TRANSMIT_ENERGY_S);
		this.receiveEnergy=s.getDouble(RECEIVE_ENERGY_S);//I added this previously no energy loss on receive
		this.scanInterval  = s.getDouble(SimScenario.SCAN_INTERVAL_S);
		
		if (s.contains(WARMUP_S)) {
			this.warmupTime = s.getInt(WARMUP_S);
			if (this.warmupTime == -1) {
				this.warmupTime = new Settings(report.Report.REPORT_NS).
					getInt(report.Report.WARMUP_S);
			}
		}
		else {
			this.warmupTime = 0;
		}
	}
	
	/**
	 * Sets the current energy level into the given range using uniform 
	 * random distribution.
	 * @param range The min and max values of the range, or if only one value
	 * is given, that is used as the energy level
	 */
	protected void setEnergy(double range[]) {
		if (range.length == 1) {
			this.currentEnergy = range[0];
		}
		else {
			if (rng == null) {
				rng = new Random((int)(range[0] + range[1]));
			}
			this.currentEnergy = range[0] + 
				rng.nextDouble() * (range[1] - range[0]);
		}
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EnergyAwareRouter(EnergyAwareRouter r) {
		super(r);
		this.initEnergy = r.initEnergy;
		setEnergy(this.initEnergy);
		this.scanEnergy = r.scanEnergy;
		this.transmitEnergy = r.transmitEnergy;
		this.receiveEnergy=r.receiveEnergy;//I added this previously no energy loss on receive
		this.scanInterval = r.scanInterval;
		this.warmupTime  = r.warmupTime;
		this.comBus = null;
		this.lastScanUpdate = 0;
		this.lastUpdate = 0;
		this.time_start_receive=0;//I added this previously no energy loss on receive
	}
	
	@Override
	protected int checkReceiving(Message m) {
		if (this.currentEnergy <= 0) {
			return DENIED_UNSPECIFIED;
		}
		else {
			 return super.checkReceiving(m);
		}
	}

	@Override //I added this previously unchecked
	protected boolean canStartTransfer() {
		if (this.currentEnergy <= 0) {
			return false;
		}
		else {
			 return super.canStartTransfer();
		}
	}

	@Override //I added this previously unchecked but is not reached since all children are overriding it so I overided the following function
	public boolean createNewMessage(Message m) {
		if (this.currentEnergy <= 0) {
			return false;
		}
		else
			return super.createNewMessage(m);
	}

	@Override //I added this previously unchecked
	protected void addToMessages(Message m, boolean newMessage) {
		if (this.currentEnergy > 0)
			super.addToMessages(m,newMessage);
	}

	@Override //I added this previously no energy loss on receive
	public int receiveMessage(Message m, DTNHost from) {
		int recvCheck = super.receiveMessage(m, from);
		if (recvCheck==RCV_OK)
			time_start_receive=SimClock.getTime();
		return recvCheck;
	}

	@Override //I added this previously no energy loss on receive
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
			assert(time_start_receive>=0);
			reduceEnergy((SimClock.getTime()-time_start_receive)*receiveEnergy);
			time_start_receive=-1;
		return m;
	}

	@Override //I added this previously no energy loss on receive
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		super.messageAborted(id,from,bytesRemaining);
		assert(time_start_receive>=0);
		reduceEnergy((SimClock.getTime()-time_start_receive)*receiveEnergy);
		time_start_receive=-1;
	}

	/**
	 * Updates the current energy so that the given amount is reduced from it.
	 * If the energy level goes below zero, sets the level to zero.
	 * Does nothing if the warmup time has not passed.
	 * @param amount The amount of energy to reduce
	 */
	protected void reduceEnergy(double amount) {
		if (SimClock.getTime() < this.warmupTime) {
			return;
		}
		
		comBus.updateDouble(ENERGY_VALUE_ID, -amount);
		if (this.currentEnergy < 0) {
			comBus.updateProperty(ENERGY_VALUE_ID, 0.0);
		}
	}
	
	/**
	 * Reduces the energy reserve for the amount that is used by sending data
	 * and scanning for the other nodes. 
	 */
	protected void reduceSendingAndScanningEnergy() {
		double simTime = SimClock.getTime();
		
		if (this.comBus == null) {
			this.comBus = getHost().getComBus();
			this.comBus.addProperty(ENERGY_VALUE_ID, this.currentEnergy);
			this.comBus.subscribe(ENERGY_VALUE_ID, this);
		}
		
		if (this.currentEnergy <= 0) {
			/* turn radio off */
			this.comBus.updateProperty(NetworkInterface.RANGE_ID, 0.0);
			return; /* no more energy to start new transfers */
		}
		
		if (simTime > this.lastUpdate && sendingConnections.size() > 0) {
			/* sending data */
			assert(sendingConnections.size() == 1);
			reduceEnergy((simTime - this.lastUpdate) * this.transmitEnergy);
		}
		this.lastUpdate = simTime;
		
		if (simTime > this.lastScanUpdate + this.scanInterval) {
			/* scanning at this update round */
			reduceEnergy(this.scanEnergy);
			this.lastScanUpdate = simTime;
		}
	}
	
	@Override
	public void update() {
		super.update();
		reduceSendingAndScanningEnergy();
		/*
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		this.tryAllMessagesToAllConnections();*/
	}
		
	@Override
	public EnergyAwareRouter replicate() {
		return new EnergyAwareRouter(this);
	}
	
	/**
	 * Called by the combus is the energy value is changed
	 * @param key The energy ID
	 * @param newValue The new energy value
	 */
	public void moduleValueChanged(String key, Object newValue) {
		this.currentEnergy = (Double)newValue;
	}

	
	@Override
	public String toString() {
		return super.toString() + " energy level = " + this.currentEnergy;
	}	
}