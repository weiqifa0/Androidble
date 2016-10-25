package timescript.ts102.magcali;

import android.graphics.Color;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;

/**
 * Created by jimmy on 3/23/16.
 */
public class TSDraw {
    private XYPlot historyPlot = null;
    private SimpleXYSeries xSeries = null;
    private SimpleXYSeries ySeries = null;
    private SimpleXYSeries zSeries = null;
    private Redrawer redrawer = null;
    private static final int HISTORY_SIZE = 300;

    public TSDraw(XYPlot plot) {
        historyPlot = plot;
        xSeries = new SimpleXYSeries("x");
        xSeries.useImplicitXVals();
        ySeries = new SimpleXYSeries("y");
        ySeries.useImplicitXVals();
        zSeries = new SimpleXYSeries("z");
        zSeries.useImplicitXVals();

        historyPlot.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);
        historyPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        historyPlot.addSeries(xSeries, new LineAndPointFormatter(Color.RED, null, null, null));
        historyPlot.addSeries(ySeries, new LineAndPointFormatter(Color.GREEN, null, null, null));
        historyPlot.addSeries(zSeries, new LineAndPointFormatter(Color.BLUE, null, null, null));
        historyPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        historyPlot.setDomainStepValue(HISTORY_SIZE / 10);
        historyPlot.setRangeValueFormat(new DecimalFormat("#"));
        historyPlot.setDomainValueFormat(new DecimalFormat("#"));

        redrawer = new Redrawer(historyPlot, 1000, false);
    }

    public void start(boolean start) {
        if(start) {
            redrawer.start();
        }else{
            redrawer.pause();
        }
    }

    public void add(float x, float y, float z) {
        if(xSeries.size()>HISTORY_SIZE) {
            xSeries.removeFirst();
            ySeries.removeFirst();
            zSeries.removeFirst();
        }
        xSeries.addLast(null, x);
        ySeries.addLast(null, y);
        zSeries.addLast(null, z);
        historyPlot.redraw();
    }
}
