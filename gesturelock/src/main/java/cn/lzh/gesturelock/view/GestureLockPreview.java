package cn.lzh.gesturelock.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

import cn.lzh.gesturelock.R;

import static cn.lzh.gesturelock.view.GestureLockView.dip2px;

/**
 * 手势锁图案预览控件
 *
 * @author lzh
 */
public class GestureLockPreview extends View {

    /**
     * 每个边上圆点的个数
     */
    private int mCircleCount = 3;
    /**
     * 圆点的间距
     */
    private float mCircleSpace = 6;
    /**
     * 圆点的半径
     */
    private float mCircleRadius;
    private int mColorNormal = 0xff7f7f7f;
    private int mColorSelected = 0xff209b54;
    private float mStrokeWidth = 2;
    private Paint mPaint;
    /**
     * 绘制的实心点索引列表
     */
    private ArrayList<Integer> mPoints;

    public GestureLockPreview(Context context) {
        this(context, null);
    }

    public GestureLockPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockPreview(Context context, AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GestureLockPreview);
        mCircleCount = ta.getInteger(R.styleable.GestureLockPreview_circleCount, mCircleCount);
        mCircleSpace = ta.getDimension(R.styleable.GestureLockPreview_circleSpace, dip2px(getContext(), mCircleSpace));
        mCircleRadius = ta.getDimension(R.styleable.GestureLockPreview_circleRadius, dip2px(getContext(), mCircleRadius));
        mStrokeWidth = ta.getDimension(R.styleable.GestureLockPreview_strokeWidth, mStrokeWidth);
        mColorNormal = ta.getColor(R.styleable.GestureLockPreview_colorNormal, mColorNormal);
        mColorSelected = ta.getColor(R.styleable.GestureLockPreview_colorSelected, mColorSelected);
        ta.recycle();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(mColorNormal);
        mPoints = new ArrayList<Integer>();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        float space = mCircleSpace * (mCircleCount + 1);
        mCircleRadius = ((Math.min(width, height) - space) / mCircleCount) * 0.5f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = mCircleRadius + mCircleSpace, cy = mCircleRadius + mCircleSpace;
        for (int i = 0; i < mCircleCount * mCircleCount; i++) {
            if (mPoints.contains(i)) {
                mPaint.setStyle(Style.FILL);
                mPaint.setColor(mColorSelected);
            } else {
                mPaint.setStyle(Style.STROKE);
                mPaint.setColor(mColorNormal);
            }
            canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
            //计算下一个圆的圆心坐标
            cx += mCircleRadius * 2 + mCircleSpace;//两圆圆心距=半径之和+两圆间距
            if ((i + 1) % mCircleCount == 0) {
                cx = mCircleRadius + mCircleSpace;//每行第一个
                cy += mCircleRadius * 2 + mCircleSpace;//换行，两圆圆心距=半径之和+两圆间距
            }
        }
        super.onDraw(canvas);
    }

    /**
     * 添加选中的点的序号,并重绘
     *
     * @param position 选中的点的序号
     */
    public void addSelectedPoint(int position) {
        if (!mPoints.contains(position)) {
            this.mPoints.add(position);
            invalidate();
        }
    }

    /**
     * 清空实心圆点序号列表,并重绘
     */
    public void reset() {
        this.mPoints.clear();
        invalidate();
    }
}
