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
	private static double Tmin=0;
	private static double Tmax=100;
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
	//	 System.out.println(fuzzyH.mapBackToThroughput(fuzzyH.mapThroughputToFCL(12, 40), 40));
	//	 System.out.println(fuzzyH.mapBackToTime(fuzzyH.mapTimeToFCL(60)));
		 fuzzyH.getNewEstimateOriginal(10, 90, 10,100);
	 }
	public double mapThroughputToFCL(double throughput,double nominal_TH){
			double low_TH=nominal_TH/5;
			return 10*(throughput-low_TH)/(nominal_TH-low_TH)+2;
		}
	public double mapBackToThroughput(double throughput,double nominal_TH){
			double low_TH=nominal_TH/5;
			return (throughput-2)*(nominal_TH-low_TH)/10+low_TH;
		}
	public double mapTimeToFCL(double time){
			
			return 6*(time-Tmin)/(Tmax-Tmin)+2;
		}
	public double mapBackToTime(double time){

			return (time-2)*(Tmax-Tmin)/6+Tmin;
		}
	double getNewEstimateOriginal(double old_estimate, double new_value, double time_diff,double nominal_TH)
	{
			old_estimate=mapThroughputToFCL(old_estimate, nominal_TH);
			new_value=mapThroughputToFCL(new_value, nominal_TH);
			time_diff=mapTimeToFCL(time_diff);

			return getNewEstimate(old_estimate, new_value, time_diff);

	}
	double getNewEstimate(double old_estimate, double new_value, double time_diff) {
		
			fis.setVariable("oldTH", old_estimate);
	        fis.setVariable("newTH", new_value);
			fis.setVariable("timeDiff", time_diff);
			// Show
			fis.chart();

			// Evaluate
			fis.evaluate();

			// Show output variable's chart
			//fis.getVariable("estimatedTH").defuzzify();//chartDefuzzifier(true);
		//	fis.getVariable("estimatedTH").chartDefuzzifier(true);
			// Print ruleSet
			//System.out.println(fis);
			
			double fuzzyVal=fis.getVariable("estimatedTH").defuzzify();
			System.out.println(fuzzyVal);
			double x=mapBackToThroughput( fuzzyVal,100);
			System.out.println(x);
			return x;
		}

}
