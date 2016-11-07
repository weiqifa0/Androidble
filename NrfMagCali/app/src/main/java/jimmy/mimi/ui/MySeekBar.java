package jimmy.mimi.ui;

import android.content.Context;
import android.util.AttributeSet;

public class MySeekBar extends android.widget.SeekBar implements
    ProgressHintDelegate.SeekBarHintDelegateHolder {

  private ProgressHintDelegate hintDelegate;

  public MySeekBar(Context context) {
    super(context);
    init(null, 0);
  }

  public MySeekBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public MySeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs, defStyleAttr);
  }

  private void init(AttributeSet attrs, int defStyle) {
    if (!isInEditMode()) {
      hintDelegate = new HorizontalProgressHintDelegate(this, attrs, defStyle);
    }
  }

  @Override public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
    super.setOnSeekBarChangeListener(hintDelegate.setOnSeekBarChangeListener(l));
  }

  @Override
  public ProgressHintDelegate getHintDelegate() {
    return hintDelegate;
  }

}
