/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package applications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 *
 * @author Hamza
 */
public class FCLInterface {
	private String text;
    public static void main(String[] args) throws Exception {
	FCLInterface trial=new FCLInterface();
	FileOutputStream fout=new  FileOutputStream ("./applications/file.fcl");
	new PrintStream(fout).println (trial.text);
	System.out.print(trial.text);
	}
	public FCLInterface()
	{
		text="// Block definition "
+				"FUNCTION_BLOCK fuzzyblock1"
+				"// Define input variables"
+				"VAR_INPUT"
+"\n"+"    newTH : REAL;"
+"\n"+"    oldTH : REAL;"
+"\n"+"    timeDiff : REAL;"
+"\n"+"END_VAR"
+"\n"+""
+"\n"+"// Define output variable"
+"\n"+"VAR_OUTPUT"
+"\n"+"    estimatedTH : REAL;"
+"\n"+"END_VAR"
+"\n"+""
+"\n"+"// Fuzzify input variable 'newTH'"
+"\n"+"FUZZIFY newTH"
+"\n"+"  TERM VL := (0, 1) (4, 0) ;"
+"\n"+"  TERM L := (0, 1) (4, 0) ;"
+"\n"+"  TERM M := (1, 0) (4,1) (6,1) (9,0);"
+"\n"+"    TERM H := (6, 0) (9, 1);"
+"\n"+"    TERM VH := (6, 0) (9, 1);"
+"\n"+"END_FUZZIFY"
+"\n"+""
+"\n"+"// Fuzzify input variable 'oldTH'"
+"\n"+"FUZZIFY oldTH"
+"\n"+"  TERM VL := (0, 1) (4, 0) ;"
+"\n"+"    TERM L := (0, 1) (4, 0) ;"
+"\n"+"    TERM M := (1, 0) (4,1) (6,1) (9,0);"
+"\n"+"    TERM H := (6, 0) (9, 1);"
+"\n"+"    TERM VH := (6, 0) (9, 1);"
+"\n"+"END_FUZZIFY"
+"\n"+""
+"\n"+"// Fuzzify input variable 'timeDiff' - Fix Tmax=20 (an empirical value for slow channel)"
+"\n"+"FUZZIFY oldTH"
+"\n"+"    TERM low := (0, 5) (7, 1) ; 		//recent"
+"\n"+"    TERM medium := (1, 0) (4,1) (6,1) (9,0);"
+"\n"+"    TERM high := (6, 0) (9, 1);			//old"
+"\n"+"END_FUZZIFY"
+"\n"+""
+"\n"+"// Defzzzify output variable 'estimatedTH' "
+"\n"+"DEFUZZIFY estimatedTH "
+"\n"+"  TERM VL := (0, 1) (4, 0) ;"
+"\n"+"    TERM L := (0, 1) (4, 0) ;"
+"\n"+"    TERM M := (1, 0) (4,1) (6,1) (9,0);"
+"\n"+"    TERM H := (6, 0) (9, 1);"
+"\n"+"    TERM VH := (6, 0) (9, 1);"
+"\n"+"    // Use 'Center Of Gravity' defuzzification method"
+"\n"+"    METHOD : COG;"
+"\n"+"    // Default value is 0 (if no rule activates defuzzifier)"
+"\n"+"    DEFAULT := 0;"
+"\n"+"END_DEFUZZIFY"
+"\n"+""
+"\n"+"RULEBLOCK No1"
+"\n"+"    // Use 'min' for 'and' (also implicit use 'max'"
+"\n"+"    // for 'or' to fulfill DeMorgan's Law)"
+"\n"+"    AND : MIN;"
+"\n"+"    // Use 'min' activation method"
+"\n"+"    ACT : MIN;"
+"\n"+"    // Use 'max' accumulation method"
+"\n"+"    ACCU : MAX;"
+"\n"+""
+"\n"+"    RULE 1 : IF timeDiff IS high AND newTH IS VL "
+"\n"+"                THEN estimatedTH IS VL;"
+"\n"+""
+"\n"+"    RULE 2 : IF timeDiff IS high AND newTH IS L "
+"\n"+"                THEN estimatedTH IS L;"
+"\n"+""
+"\n"+"    RULE 3 : IF timeDiff IS high AND newTH IS M "
+"\n"+"                THEN estimatedTH is M;"
+"\n"+""
+"\n"+"    RULE 4 : IF timeDiff IS high AND newTH IS H "
+"\n"+"                THEN estimatedTH IS H;"
+"\n"+""
+"\n"+"    RULE 5 : IF timeDiff IS high AND newTH IS VH "
+"\n"+"                THEN estimatedTH is VH;"
+"\n"+"END_RULEBLOCK"
+"\n"+""
+"\n"+"END_FUNCTION_BLOCK";

	}

}
