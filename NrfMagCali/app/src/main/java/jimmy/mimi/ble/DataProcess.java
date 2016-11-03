package jimmy.mimi.ble;

import com.androidplot.xy.XYPlot;

/**
 * Created by jimmy on 3/21/16.
 */
public class DataProcess {
    private String TAG = "TS102Device";
    public static final int BLOCK_SIZE = 40; //this must at least contain one period
    private final float COUNT_MIN_RANGE = 10;
    private double ALP = 0.3;
    private int head = 0;
    private int tail = 0;
    private float[] xdata = new float[BLOCK_SIZE];
    private float[] ydata = new float[BLOCK_SIZE];
    private float[] zdata = new float[BLOCK_SIZE];
    private float xPre = 0;
    private float yPre = 0;
    private float zPre = 0;
    private float max = 0;
    private float min = 0;
    private boolean skipStep = false;
    private ResultData result = new ResultData();

    //AXES_X: brush in portaint mode
    //AXES_Y: brush in landscape mode
    private final int AXES_NO = 0x0;
    private final int AXES_X = 0x01;
    private final int AXES_Y = 0x02;
    private int curAxesStatus = AXES_NO;

    //brush side status
    private final int INNER_SIDE = 0x0;
    private final int OUT_SIDE = 0x01;
    private final int UP_SIDE = 0x02;
    private final int DOWN_SIDE = 0x03;
    private int curSide;

    /**
     * Add the data to block and use LPF
     * @param data
     */
    public void addData(float[] data) {
        if(tail == 0) {
            xdata[tail] = (float) (xPre*(1-ALP) + data[0]*ALP);
            ydata[tail] = (float) (yPre*(1-ALP) + data[1]*ALP);
            zdata[tail] = (float) (zPre*(1-ALP) + data[2]*ALP);
        } else {
            xdata[tail] = (float) (xdata[tail-1]*(1-ALP) + data[0]*ALP);
            ydata[tail] = (float) (ydata[tail-1]*(1-ALP) + data[1]*ALP);
            zdata[tail] = (float) (zdata[tail-1]*(1-ALP) + data[2]*ALP);
        }
        tsDraw.add(xdata[tail], ydata[tail], zdata[tail]);
        tail++;

        //start calculate when data is full
        if(tail == BLOCK_SIZE) {
            calculate();
            xPre = xdata[tail-1];
            yPre = ydata[tail-1];
            zPre = zdata[tail-1];
            tail = 0;
        }
    }

    /**
    * find out which axes data should use to count
    * and start get the count
    **/
    private void calculate() {
        int status = findAxes();
        findSide();
        if(curAxesStatus != status || status == AXES_NO) {
            max = min = 0;
        }
        curAxesStatus = status;
        if(curAxesStatus == AXES_X) {
            counter(AXES_X, xdata);
        }else if(curAxesStatus == AXES_Y){
            counter(AXES_Y, ydata);
        }

        if(curAxesStatus == AXES_NO) {
            return;
        } else {
            onResult(result);
            //Log.d(TAG, "xcount=" + result.downSide.xcount + "; yount=" + result.downSide.ycount);
        }
    }

    /**
     * get the circle count in data block
     * @param axes signed which axes this data is
     * @param datas X-axes or Y-axes block datas
     */
    private void counter(int axes, float[] datas) {
        float pre_data = 0;
        boolean firstCnt = true;
        if(axes == AXES_X) {
            pre_data = xPre;
        } else {
            pre_data = yPre;
        }
        curSide = findSide();
        for(int i=0; i<datas.length-1; i++) {
            if(i==0) {
                if(datas[i]>=pre_data && datas[i]>datas[i+1]) {
                    if(Math.abs(datas[i]-min) > COUNT_MIN_RANGE) {
                        max = datas[i];
                        if(!skipStep) {
                            //catch the one circle
                            firstCnt = false;
                            switch(curSide) {
                                case INNER_SIDE:
                                    if(axes==AXES_X) {
                                        result.innerSide.xcount++;
                                    } else {
                                        result.innerSide.ycount++;
                                    }
                                    break;
                                case OUT_SIDE:
                                    if(axes==AXES_X) {
                                        result.outSide.xcount++;
                                    } else {
                                        result.outSide.ycount++;
                                    }
                                    break;
                                case UP_SIDE:
                                    if(axes==AXES_X) {
                                        result.upSide.xcount++;
                                    } else {
                                        result.upSide.ycount++;
                                    }
                                    break;
                                case DOWN_SIDE:
                                    if(axes==AXES_X) {
                                        result.downSide.xcount++;
                                    } else {
                                        result.downSide.ycount++;
                                    }
                                    break;
                            }
                        } else {
                            skipStep = false;
                        }
                    }else {
                        skipStep = false;
                    }
                } else if(datas[i]<=pre_data && datas[i]<datas[i+1]) {
                    min = datas[i];
                    if(Math.abs(max-datas[i]) < COUNT_MIN_RANGE) {
                        if(!firstCnt) {
                            skipStep = true;
                        }
                    }
                }
            } else {
                if(datas[i]>=datas[i-1] && datas[i]>datas[i+1]) {
                    if(Math.abs(datas[i]-min) > COUNT_MIN_RANGE) {
                        max = datas[i];
                        if(!skipStep) {
                            //catch the one circle
                            firstCnt = false;
                            switch(curSide) {
                                case INNER_SIDE:
                                    if(axes==AXES_X) {
                                        result.innerSide.xcount++;
                                    } else {
                                        result.innerSide.ycount++;
                                    }
                                    break;
                                case OUT_SIDE:
                                    if(axes==AXES_X) {
                                        result.outSide.xcount++;
                                    } else {
                                        result.outSide.ycount++;
                                    }
                                    break;
                                case UP_SIDE:
                                    if(axes==AXES_X) {
                                        result.upSide.xcount++;
                                    } else {
                                        result.upSide.ycount++;
                                    }
                                    break;
                                case DOWN_SIDE:
                                    if(axes==AXES_X) {
                                        result.downSide.xcount++;
                                    } else {
                                        result.downSide.ycount++;
                                    }
                                    break;
                            }
                        } else {
                            skipStep = false;
                        }
                    }else {
                        skipStep = false;
                    }
                } else if(datas[i]<=datas[i-1] && datas[i]<datas[i+1]) {
                    min = datas[i];
                    if(Math.abs(max-datas[i]) < COUNT_MIN_RANGE) {
                        if(!firstCnt) {
                            skipStep = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * find out which axes data should use
     * @return AXES_X means we should use X-axes data, AXES_Y means we should use Y-axes data,
     * AXES_NO means this data is useless.
     */
    private int findAxes() {
        float var_x = variance(xdata);
        float var_y = variance(ydata);
        //Log.d(TAG, "var_x=" + var_x + "; var_y=" + var_y);
        if(var_x<10 && var_y<10) {
            return AXES_NO;
        }else if(var_x>var_y) {
            return AXES_X;
        }else {
            return AXES_Y;
        }
    }

    /**
     * Find out which side current brush
     */
    private int findSide() {
        double ay = getSum(ydata)/ydata.length;
        double az = getSum(zdata)/zdata.length;
        double gxz = Math.sqrt(ay*ay + az*az);
        double cos = az/gxz;
        double rad = Math.acos(cos);
        int ret = DOWN_SIDE;
        if(ay<0) {
            rad = 2*Math.PI - rad;
        }
        rad = rad*180/Math.PI;
        if(rad<30 || rad>=330) {
            ret = DOWN_SIDE;
        }else if(rad>=30 && rad<140) {
            ret = OUT_SIDE;
        }else if(rad>=140 && rad<220) {
            ret = UP_SIDE;
        }else if(rad>=220 && rad<330) {
            ret = INNER_SIDE;
        }
        return ret;
    }

    /**
     * Get the variance of this data block
     * @param datas X-axes/Y-axes block data
     * @return variance value
     */
    private float variance(float[] datas) {
        float var = 0;
        for(int i=0; i<datas.length; i++) {
            var += datas[i]*datas[i];
        }
        var = var/datas.length - (getSum(datas)/datas.length)*(getSum(datas)/datas.length);
        return var;
    }

    private float getSum(float[] datas) {
        float sum = 0;
        for(int i=0; i<datas.length; i++) {
            sum += datas[i];
        }
        return sum;
    }

    public void clearData(){
        result.innerSide.xcount = 0;
        result.innerSide.ycount = 0;
        result.outSide.xcount = 0;
        result.outSide.ycount = 0;
        result.upSide.xcount = 0;
        result.upSide.ycount = 0;
        result.downSide.xcount = 0;
        result.downSide.ycount = 0;
    }

    public void onResult(ResultData resultData) {

    }

    public void start() {
        tsDraw.start(true);
    }

    public void stop() {
        tsDraw.start(false);
        clearData();
    }

    private TSDraw tsDraw= null;

    /**
     * TSDraw use here just show data for test
     * @param plot
     */
    public void addPlot(XYPlot plot) {
        tsDraw = new TSDraw(plot);
    }
}