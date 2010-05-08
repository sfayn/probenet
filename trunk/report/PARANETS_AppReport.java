package report;

import applications.PARANETS_application;
import core.Application;
import core.ApplicationListener;
import core.DTNHost;

/**
 * Reporter for the <code>PARANETS_application</code>.
 * 
 * @author Jad Makhlouta
 */
public class PARANETS_AppReport extends Report implements ApplicationListener {
	
	public void gotEvent(String event, Object params, Application app,
			DTNHost host) {
		// Check that the event is sent by correct application type
		if (!(app instanceof PARANETS_application)) return;
		
		write(event+ ":"+(String)params+" >> "+host);
	}

	@Override
	public void done() {
		super.done();
	}
}
