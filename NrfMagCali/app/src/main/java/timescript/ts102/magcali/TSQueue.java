package timescript.ts102.magcali;

import android.util.Log;

/**
 * Created by jimmy on 3/21/16.
 */
public class TSQueue {
    private String TAG = "TsQueue";
    private int head = 0;
    private int tail = 0;
    private float[] elements;
    private int blockSize;
    private int count = 0;
    private static final int DEFAULT_QUEUE_SIZE = 40;

    public TSQueue() {
        elements = new float[DEFAULT_QUEUE_SIZE];
        blockSize = DEFAULT_QUEUE_SIZE;
        tail = blockSize -1;
    }
    public TSQueue(int size) {
        if(size<=0) {
            blockSize = DEFAULT_QUEUE_SIZE;
        } else {
            blockSize = size;
        }
        elements = new float[blockSize];
        tail = blockSize - 1;
    }

    public void clean() {
        head = 0;
        count = 0;
        tail = blockSize - 1;
    }

    public boolean add(float e) {
        if(count == blockSize) {
            Log.d(TAG, "elements is full");
            return false;
        }
        tail = (tail + 1) % blockSize;
        elements[tail] = e;
        count++;
        return true;
    }

    public float pop() {
        if(count<=0) {
            Log.d(TAG, "elements is empty");
            return 0;
        }
        float result = elements[head];
        head = (head+1)%blockSize;
        count--;
        return result;
    }

    public float getLast() {
        if(count<=0) {
            Log.d(TAG, "elements is empty");
            return 0;
        }
        return elements[tail];
    }

    public float getSum() {
        float sum = 0;
        for(int i=0; i<count; i++) {
            sum = sum + (float)elements[(head + i) % blockSize];
        }
        return sum;
    }

    public int getLenth() {
        return count;
    }
}
