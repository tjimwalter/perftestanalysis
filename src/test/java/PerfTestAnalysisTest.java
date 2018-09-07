import com.perftestanalysis.PerfTestAnalysis;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import java.util.stream.*;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

//
// https://onlinecourses.science.psu.edu/stat500/node/50
//
public class PerfTestAnalysisTest {
    @Rule
    public TestName testName = new TestName();

    // known dataset where variant < control at confidence < 0.999
    double variant[]  = { 42.1, 41.3, 42.4, 43.2, 41.8, 41.0, 41.8, 42.8, 42.3, 42.7 };
    double control[]  = { 42.7, 43.8, 42.5, 43.1, 44.0, 43.6, 43.3, 43.5, 41.7, 44.1 };
    double variantSum = DoubleStream.of(variant).sum();
    double controlSum = DoubleStream.of(control).sum();

    @Test
    public void PerfTestAnalysis_InvokingDefaultConstructor_ReturnsZeroValues() {
        System.out.println("----> Starting: " + testName.getMethodName());

        // Create an empty pta object and confirm 0 values in SummaryStatistics objects
        PerfTestAnalysis pta = new PerfTestAnalysis();
        SummaryStatistics ss;
        ss = pta.getSummaryStatisticsVariant();
        assertEquals("There are ZERO measurements", 0, ss.getN());
        ss = pta.getSummaryStatisticsControl();
        assertEquals("There are ZERO measurements", 0, ss.getN());
    }
    @Test
    public void PerfTestAnalysis_InvokingBulkAddConstructor_ReturnsKnownValues() {
        System.out.println("----> Starting: " + testName.getMethodName());
        SummaryStatistics ss;

        // Create and load a pta object; verify the size and sum of SummaryStatistics objects
        PerfTestAnalysis pta = new PerfTestAnalysis(variant, control);
        ss = pta.getSummaryStatisticsVariant();
        assertEquals("There are 10 measurements", variant.length, ss.getN());
        assertEquals("The sum of all measurements==testValue", variantSum, ss.getSum(), .001);
        ss = pta.getSummaryStatisticsControl();
        assertEquals("There are 10 measurements", control.length, ss.getN());
        assertEquals("The sum of all measurements==testValue", controlSum, ss.getSum(), .001);
    }
    @Test
    public void isVariantLTControl_AsConfidenceIncreases_MethodEventuallyReturnsFalse() {
        System.out.println("----> Starting: " + testName.getMethodName());

        PerfTestAnalysis pta = new PerfTestAnalysis(variant, control);

        // gradually increase the confidence until we hit false
        double confidence = 0.95;
        assertTrue ("Variant IS  < control", pta.isVariantLTControl(confidence));
        confidence = 0.99;
        assertTrue ("Variant IS  < control", pta.isVariantLTControl(confidence));
        confidence = 0.999;
        assertFalse("Variant NOT < control", pta.isVariantLTControl(confidence));
    }
    @Test
    public void isVariantGTControl_AsConfidenceIncreases_MethodEventuallyReturnsFalse() {
        System.out.println("----> Starting: " + testName.getMethodName());

        PerfTestAnalysis pta = new PerfTestAnalysis(control, variant);

        // gradually increase the confidence until we hit false
        double confidence = 0.95;
        assertTrue ("Variant IS  > control", pta.isVariantGTControl(confidence));
        confidence = 0.99;
        assertTrue ("Variant IS  > control", pta.isVariantGTControl(confidence));
        confidence = 0.999;
        assertFalse("Variant NOT > control", pta.isVariantGTControl(confidence));
    }
    @Test
    public void printConfidenceInterval_WhenRequested_PrintOccurs(){
        System.out.println("----> Starting: " + testName.getMethodName());
        PerfTestAnalysis pta = new PerfTestAnalysis(variant, control);
        pta.printConfidenceInterval(0.90);
    }
}