/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package applications;

import net.sourceforge.jFuzzyLogic.FIS;

/**
 *
 * @author Hamza
 */
public class fuzzyHelper {
	
	private FIS fis;
	public fuzzyHelper(){
		String fileName = "applications\\file.fcl";
		 fis = new FIS();
		 fis=FIS.load(fileName,true);
		// Error while loading?
		if( fis == null ) {
			System.err.println("Can't load file: '"
					   + fileName + "'");
			return;
		}
	}
	 public static void main(String[] args) throws Exception {
		 fuzzyHelper fuzzyH=new fuzzyHelper();
		 fuzzyH.getNewEstimate(1, 8, 2);
	 }
	private double mapThroughputToFCL(double throughput,double nominal_TH){
			double low_TH=nominal_TH/5;
			return 10*(throughput-low_TH)/(nominal_TH-low_TH)+2;
		}
	private double mapTimeToFCL(double time){
			return 6*(time-50)/(100-50)+2;
		}

	double getNewEstimate(double old_estimate, double new_value, double time_diff) {
		
			fis.setVariable("newTH", old_estimate);
	        fis.setVariable("oldTH", new_value);
			fis.setVariable("timeDiff", time_diff);
			// Show
			fis.chart();

			// Evaluate
			fis.evaluate();

			// Show output variable's chart
			//fis.getVariable("estimatedTH").defuzzify();//chartDefuzzifier(true);
			fis.getVariable("estimatedTH").chartDefuzzifier(true);
			// Print ruleSet
			//System.out.println(fis);
			System.out.println(fis.getVariable("estimatedTH").defuzzify());

			return fis.getVariable("estimatedTH").defuzzify();
		}

}
