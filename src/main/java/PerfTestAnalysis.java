package com.perftestanalysis;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import static org.apache.commons.math3.stat.descriptive.SummaryStatistics.copy;

// The problem: compare two population means
//    - the CONTROL (your existing solution)
//    - The VARIANT (your new, improved solution)
//
// You don't know the actual population means so you estimate with samples.
// This allows you to perform inferential statistics based on the
// sampling distribution of the differences between the two sample means.
//
// The solution for LT and GT methods is derived from this website
// https://onlinecourses.science.psu.edu/stat500/node/50/
// It is a one-tailed t-test and relies of these assumptions
//    - Samples are independent and nearly equal in size
//    - Samples are quasi-normal
//    - Sample variances are very similar
//
// The solution for confidence interval is derived from this website
// https://onlinecourses.science.psu.edu/stat500/node/34/
//
//

public class PerfTestAnalysis {

    private SummaryStatistics variant;
    private SummaryStatistics control;

    public PerfTestAnalysis() {
        variant = new SummaryStatistics();
        control = new SummaryStatistics();
    }
    public PerfTestAnalysis(double variantValues[], double controlValues[]) {
        this();
        for (double d : variantValues) {
            addValueVariant(d);
        }
        for (double d : controlValues) {
            addValueControl(d);
        }
    }
    public void addValueVariant(double value) {
        variant.addValue(value);
    }
    public void addValueControl(double value) {
        control.addValue(value);
    }
    public void printConfidenceInterval(double confidence) {
        ConfidenceInterval ci = new ConfidenceInterval(confidence);

        System.out.print("\n");
        System.out.println("Experiment |   Min   |   Lower |    Mean |   Upper |   Max   |   StDev |   Conf  |" );
        System.out.println("-----------+---------+---------+---------+---------+---------|---------|---------|" );
        System.out.println(String.format("VARIANT    |% 8.2f |% 8.2f |% 8.2f |% 8.2f |% 8.2f |% 8.4f |% 8.4f |",
                ci.vMin, ci.vLo, ci.vMu, ci.vHi, ci.vMax, ci.vSd, confidence));
        System.out.println(String.format("CONTROL    |% 8.2f |% 8.2f |% 8.2f |% 8.2f |% 8.2f |% 8.4f |% 8.4f |",
                ci.cMin, ci.cLo, ci.cMu, ci.cHi, ci.cMax, ci.cSd, confidence));
        System.out.println("");

        double range = ci.max - ci.min;
        int    steps = 50;
        double step  = range / steps;
        System.out.println(String.format("        % 5.2f    % 5.2f    % 5.2f    % 5.2f    % 5.2f    % 5.2f",
                ci.min, ci.min+(10*step), ci.min+(20*step), ci.min+(30*step), ci.min+(40*step), ci.min+(50*step) ));
        System.out.println("           +----+----+----+----+----+----+----+----+----+----+" );
        System.out.print("Variant: ");
        for (int i = 0; i < steps; i++){
            double loc = ci.min + (i * step);
            if ( (loc > ci.vMin) && (loc < ci.vMax) ) {
                if ( (loc > ci.vLo) && (loc < ci.vHi) ) {
                    if (loc < ci.vMu){
                        System.out.print("/");
                    } else {
                        System.out.print("\\");
                    }
                } else {
                    System.out.print(".");
                }
            } else {
                System.out.print(" ");
            }
        }
        System.out.print("\nControl: ");
        for (int i = 0; i < steps; i++){
            double loc = ci.min + (i * step);
            if ( (loc > ci.cMin) && (loc < ci.cMax) ) {
                if ( (loc > ci.cLo) && (loc < ci.cHi) ) {
                    if (loc < ci.cMu){
                        System.out.print("/");
                    } else {
                        System.out.print("\\");
                    }
                } else {
                    System.out.print(".");
                }
            } else {
                System.out.print(" ");
            }
        }
        System.out.print("\n");
        System.out.println("           +----+----+----+----+----+----+----+----+----+----+" );
        System.out.println(String.format("             % 5.2f    % 5.2f    % 5.2f    % 5.2f    % 5.2f",
                ci.min+(5*step), ci.min+(15*step), ci.min+(25*step), ci.min+(35*step), ci.min+(45*step) ));
        System.out.print("\n");
    }
    public SummaryStatistics getSummaryStatisticsVariant() {
        SummaryStatistics ssDest = new SummaryStatistics();
        copy(variant, ssDest);
        return ssDest;
    }
    public SummaryStatistics getSummaryStatisticsControl() {
        SummaryStatistics ssDest = new SummaryStatistics();
        copy(control, ssDest);
        return ssDest;
    }
    public double getConfidenceInterval(SummaryStatistics ss, double confidence) {
        try { // Create T Distribution with N-1 DoF, calc the critical value & confidence interval
            TDistribution tDist = new TDistribution(ss.getN() - 1);
            double critVal  = tDist.inverseCumulativeProbability(1.0 - (1 - confidence) / 2);
            double interval = critVal * ss.getStandardDeviation() / Math.sqrt(ss.getN());
            return interval;
        } catch (MathIllegalArgumentException e) {
            return Double.NaN;
        }
    }
    public boolean isVariantLTControl(double confidence) {
        // One tailed hypothesis test using pooled SD
        // h0: mu(variant) and mu(control) are same
        // hA: mu(variant) < mu(control)  i.e. the variant is less than the control

        double        dOfF           = variant.getN() + control.getN() - 2;
        TDistribution tDist          = new TDistribution(dOfF);
        double        alpha          = 1 - confidence;
        double        leftTailReject = tDist.inverseCumulativeProbability(alpha);
        double        sdPooled;
        double        tStatistic;

        sdPooled = Math.sqrt((((variant.getN() - 1) * Math.pow(variant.getStandardDeviation(), 2) ) +
                        ((control.getN() - 1) * Math.pow(control.getStandardDeviation(), 2))) / dOfF);

        tStatistic = (variant.getMean() - control.getMean()) /
                (Math.sqrt( (1.0 / variant.getN()) + (1.0 / control.getN()) ) * sdPooled);

        if (tStatistic < leftTailReject){
            return true;        //reject h0 and accept hA i.e. variant mu < control mu
        } else {
            return false;       // accept h0 and accept hA i.e. variant mu ~>= control mu
        }
    }
    public boolean isVariantGTControl(double confidence) {
        // One tailed hypothesis test using pooled SD
        // h0: mu(variant) and mu(control) are same
        // hA: mu(variant) < mu(control)  i.e. the new algorithm is better

        double        dOfF            = variant.getN() + control.getN() - 2;
        TDistribution tDist           = new TDistribution(dOfF);
        double        alpha           = 1 - confidence;
        double        rightTailReject = tDist.inverseCumulativeProbability(1.0-alpha);
        double        sdPooled;
        double        tStatistic;

        sdPooled = Math.sqrt((((variant.getN() - 1) * Math.pow(variant.getStandardDeviation(), 2) ) +
                ((control.getN() - 1) * Math.pow(control.getStandardDeviation(), 2))) / dOfF);

        tStatistic = (variant.getMean() - control.getMean()) /
                (Math.sqrt( (1.0 / variant.getN()) + (1.0 / control.getN()) ) * sdPooled);

        if (tStatistic > rightTailReject){
            return true;        //reject h0 and accept hA i.e. variant mu > control mu
        } else {
            return false;       // accept h0 and accept hA i.e. variant mu ~<= control mu
        }
    }


    private class ConfidenceInterval{
        double vCi,  cCi;
        double vMin, cMin, min;
        double vMu,  cMu;
        double vLo,  cLo;
        double vHi,  cHi;
        double vMax, cMax, max;
        double vSd,  cSd;
        double confidence;

        public ConfidenceInterval(double confidence){
            this.confidence = confidence;
            vCi  = getConfidenceInterval(variant, confidence);
            vMin = variant.getMin();
            vMu  = variant.getMean();
            vLo  = vMu - vCi;
            vHi  = vMu + vCi;
            vMax = variant.getMax();
            vSd  = variant.getStandardDeviation();

            cCi  = getConfidenceInterval(control, confidence);
            cMin = control.getMin();
            cMu  = control.getMean();
            cLo  = cMu - cCi;
            cHi  = cMu + cCi;
            cMax = control.getMax();
            cSd  = control.getStandardDeviation();

            min  = Math.min(vMin, cMin);
            max  = Math.max(vMax, cMax);

        }
    }
}

