/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package report;

import core.DTNHost;
import java.util.List;

/**
 *
 * @author Jad Makhlouta
 */
public class FuzzyEnergyReport extends EnergyLevelReport {

	private String header="", average="",empty="";

	@Override
	protected void createSnapshot(List<DTNHost> hosts) {
		header=header+ "\t" + (int)getSimTime(); /* simulation time stamp */
		double sum=0;
		int empty_count=0;
		for (DTNHost h : hosts) {
			if (this.reportedNodes != null &&
				!this.reportedNodes.contains(h.getAddress())) {
				continue; /* node not in the list */
			}
			double energy=(h.getRouter() instanceof routing.EnergyAwareRouter ? (Double)h.getComBus().getProperty(routing.EnergyAwareRouter.ENERGY_VALUE_ID):-1);
			sum+=energy;
			if (energy==0)
				empty_count++;
		}
		average=average+ "\t" +format(sum/hosts.size());
		empty=empty+"\t"	+ empty_count;

	}
	@Override
	public void done()
	{
		write("time:"+header);
		write("average:"+average);
		write("off-nodes:"+empty);
		super.done();
	}
}
