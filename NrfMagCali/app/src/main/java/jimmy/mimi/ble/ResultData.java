package jimmy.mimi.ble;

/**
 * Created by jimmy on 3/21/16.
 */
public class ResultData {
    public ToothData innerSide;
    public ToothData outSide;
    public ToothData upSide;
    public ToothData downSide;

    public ResultData() {
        innerSide = new ToothData();
        outSide = new ToothData();
        upSide = new ToothData();
        downSide = new ToothData();
    }

    class ToothData {
        public int time;
        public int xcount;
        public int ycount;
    }
}

