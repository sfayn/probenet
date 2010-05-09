package interfaces;

import core.Connection;
import core.NetworkInterface;
import core.Settings;
import java.util.Collection;
import java.util.List;
import routing.ParanetAdaptableFuzzySprayAndWaitRouter;

/**
 *
 * @author Jad Makhlouta
 */
public class ParanetsInterface extends InterferenceLimitedInterface {

	public ParanetsInterface(Settings s) {
		super(s);
	}

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public ParanetsInterface(ParanetsInterface ni) {
		super(ni);
	}

	@Override
	public NetworkInterface replicate() {
		return new ParanetsInterface(this);
	}

	/**
	 * Updates the state of current connections (i.e., tears down connections
	 * that are out of range).
	 */

	@Override
	public void update() {
		// First break the old ones
		optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			} else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces)
			connect(i);

		// Find the current number of transmissions
		// (to calculate the current transmission speed)
		numberOfTransmissions = 0;
		List<NetworkInterface> host_interfaces=this.getHost().getInterfaces();
		int [] num_per_interface=new int[host_interfaces.size()];
		for (int i=0; i<host_interfaces.size();i++)
			num_per_interface[i]=0;
		for (int i=0; i<host_interfaces.size();i++)
		{
			for (Connection con : host_interfaces.get(i).getConnections())
			{
				if (con.getMessage() != null) {
					numberOfTransmissions++;
					num_per_interface[i]++;
				}
			}
		}

		int ntrans = numberOfTransmissions;
		if ( numberOfTransmissions < 1) ntrans = 1;
		
		currentTransmitSpeed =
			(int)Math.floor((double)((ParanetAdaptableFuzzySprayAndWaitRouter)this.getHost().getRouter()).get_availableBandwaidth() /ntrans );
		boolean display=false;
		/*if (num_per_interface[0]>0 || num_per_interface[1]>0 || num_per_interface[2]>0)
			display=true;*/
		if (display)
			System.out.print(currentTransmitSpeed);
		//int averge_TS=currentTransmitSpeed;
		int [] TS=new int [host_interfaces.size()];
		boolean [] bottleneck=new boolean [host_interfaces.size()];
		for (int i=0; i<host_interfaces.size();i++)
		{
			TS[i]=(num_per_interface[i]>0?currentTransmitSpeed:0);
			bottleneck[i]=false;
		}
		if (display)
			System.out.print("("+num_per_interface[0]+","+num_per_interface[1]+","+num_per_interface[2]+")");
		int freedTS=0;
		//int total_TS=currentTransmitSpeed*ntrans;
		for (int i=0; i<host_interfaces.size();i++)
		{
			int t=i;
			if (TS[i]>((ParanetsInterface)host_interfaces.get(i)).transmitSpeed)
			{
				if (!bottleneck[i])
				{
					ntrans-=num_per_interface[i];
					bottleneck[i]=true;
				}
				freedTS=(TS[i]-((ParanetsInterface)host_interfaces.get(i)).transmitSpeed)*num_per_interface[i];
				TS[i]=((ParanetsInterface)host_interfaces.get(i)).transmitSpeed;
				//total_TS=0;
				for (int j=0; j<host_interfaces.size();j++)
				{
					if (!bottleneck[j])
					{
						if (ntrans>0)
							TS[j]+=(double)freedTS/ntrans;
						/*else
							TS[j]=host_interfaces.get(j).getTransmitSpeed();*/
					}
					if (display)
						System.out.print(" "+TS[j]);
				}
				i=-1; //in order to repeat for loop from beggining
			}
			if (this.getInterfaceType().equals(host_interfaces.get(t).getInterfaceType()) && TS[t]>0)
				currentTransmitSpeed=TS[t];
		}
		if (display)
			System.out.print("--> ("+getInterfaceType()+")"+currentTransmitSpeed+"\n");
		for (Connection con : getConnections())
			con.update();
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		return "ParanetsInterface " + super.toString();
	}
}
