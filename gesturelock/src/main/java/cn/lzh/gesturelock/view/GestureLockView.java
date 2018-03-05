package cn.lzh.gesturelock.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.lzh.gesturelock.R;

/**
 * 手势锁控件
 *
 * @author lzh
 */
public class GestureLockView extends View {

    /**
     * 延时重置，单位毫秒
     */
    private int mDelayReset = 1000;
    /**
     * 每个边上圆的个数
     */
    private int mCircleCount = 3;
    /**
     * 圆的总个数
     */
    private int mCircleSize;
    /**
     * 圆的间距
     */
    private float mCircleSpace = 12;
    /**
     * 正常颜色
     */
    private int mColorNormal = 0xff7f7f7f;
    /**
     * 选中颜色
     */
    private int mColorSelected = 0xff209b54;
    /**
     * 正确颜色
     */
    private int mColorSelectedRight = mColorSelected;
    /**
     * 密码错误时显示的颜色
     */
    private int mColorError = 0xFFFF0000;
    /**
     * 圆的半径
     */
    private float mCircleRadius = 29;
    /**
     * 内圆的半径
     */
    private float mInnerCircleRadius;
    /**
     * 画笔宽度
     */
    private float mStrokeWidth = 2;
    /**
     * 是否显示绘制的手势路径(默认为true)
     */
    private boolean mGesturePathVisible = true;
    /**
     * 画笔
     */
    private Paint mPaint;
    /**
     * 连线路径
     */
    private Path mPath;
    /**
     * 圆心坐标
     */
    private float cx, cy, lastCx, lastCy;
    /**
     * 手势移动的位置
     */
    private float mMoveX = -1, mMoveY = -1;
    /**
     * 圆心坐标
     */
    private PointF mCircleCenterPoint;
    /**
     * 圆心坐标集合
     */
    private ArrayList<PointF> mCircleCenterPoints;
    /**
     * 绘制的实心圆索引列表
     */
    private ArrayList<Integer> mSelectedCircleIds;
    /**
     * 绘制的密码（密码索引=实心圆索引+1）
     */
    private ArrayList<Integer> mPasswords;
    /**
     * 绘制的密码是否正确
     */
    private boolean mIsRight = true;
    private int mMotionEventAction = MotionEvent.ACTION_DOWN;
    /**
     * 手势锁监听
     */
    private GestureLockViewListener mGestureLockViewListener;

    /**
     * 重置任务
     */
    private Runnable mResetRunnable = new Runnable() {
        @Override
        public void run() {
            reset();
        }
    };

    public GestureLockView(Context context) {
        this(context, null);
    }

    public GestureLockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GestureLockView);
        mCircleCount = ta.getInteger(R.styleable.GestureLockView_circleCount, mCircleCount);
        mCircleSpace = ta.getDimension(R.styleable.GestureLockView_circleSpace, dip2px(getContext(), mCircleSpace));
        mCircleRadius = ta.getDimension(R.styleable.GestureLockView_circleRadius, dip2px(getContext(), mCircleRadius));
        mInnerCircleRadius = ta.getDimension(R.styleable.GestureLockView_innerCircleRadius, 0);
        mStrokeWidth = ta.getDimension(R.styleable.GestureLockView_strokeWidth, mStrokeWidth);
        mColorNormal = ta.getColor(R.styleable.GestureLockView_colorNormal, mColorNormal);
        mColorSelected = ta.getColor(R.styleable.GestureLockView_colorSelected, mColorSelected);
        mColorSelectedRight = ta.getColor(R.styleable.GestureLockView_colorSelectedRight, mColorSelectedRight);
        mColorError = ta.getColor(R.styleable.GestureLockView_colorError, mColorError);
        mDelayReset = ta.getInteger(R.styleable.GestureLockView_delayReset, mDelayReset);
        mGesturePathVisible = ta.getBoolean(R.styleable.GestureLockView_gesturePathVisible, mGesturePathVisible);
        ta.recycle();
        mCircleSize = mCircleCount * mCircleCount;
        if (mInnerCircleRadius == 0) {
            mInnerCircleRadius = getInnerCircleRadius();
        }
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(mColorNormal);
        mPath = new Path();
        mSelectedCircleIds = new ArrayList<Integer>();
        mPasswords = new ArrayList<Integer>();
        // 初始化圆心坐标集合
        mCircleCenterPoints = new ArrayList<PointF>();
        for (int i = 0; i < mCircleSize; i++) {
            mCircleCenterPoint = new PointF(0, 0);
            mCircleCenterPoints.add(mCircleCenterPoint);
        }
    }

    /**
     * 获取内圆的半径
     */
    private float getInnerCircleRadius() {
        return mCircleRadius / 3;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float space = mCircleSpace * (mCircleCount - 1);
        int circleSize = (int) (mCircleRadius * 2 * mCircleCount);
        int size = (int) (space + circleSize + mStrokeWidth);
        int measureWidth = MeasureSpec.makeMeasureSpec(
                size + getPaddingLeft() + getPaddingRight(), MeasureSpec.UNSPECIFIED);
        int measureHeight = MeasureSpec.makeMeasureSpec(
                size + getPaddingTop() + getPaddingBottom(), MeasureSpec.UNSPECIFIED);
        setMeasuredDimension(measureWidth, measureHeight);//重设控件尺寸
        float paddingLeft = getPaddingLeft();
        // 重新计算圆心坐标集合
        cx = cy = mCircleRadius + paddingLeft;
        for (int i = 0; i < mCircleSize; i++) {
            mCircleCenterPoints.get(i).set(cx, cy);
            // 计算下一个圆的圆心坐标
            cx += mCircleRadius * 2 + mCircleSpace;// 两圆圆心距=半径之和+两圆间距
            if ((i + 1) % mCircleCount == 0) {
                cx = mCircleRadius + paddingLeft;// 每行第一个
                cy += mCircleRadius * 2 + mCircleSpace;// 换行，两圆圆心距=半径之和+两圆间距
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int colorSelected = mColorSelected;
        if(mMotionEventAction == MotionEvent.ACTION_UP){
            colorSelected = mIsRight ? mColorSelectedRight : mColorError;
        }
        mPaint.setStrokeWidth(mStrokeWidth);
        for (int i = 0; i < mCircleSize; i++) {
            mCircleCenterPoint = mCircleCenterPoints.get(i);
            cx = mCircleCenterPoint.x;
            cy = mCircleCenterPoint.y;
            if (mSelectedCircleIds.contains(i)
                    && (mGesturePathVisible || !mIsRight)) {
                mPaint.setColor(colorSelected);
                mPaint.setStyle(Style.STROKE);
                mPaint.setAlpha(125);
                canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
                mPaint.setStyle(Style.FILL);
                mPaint.setAlpha(50);
                canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
                mPaint.setAlpha(255);
                canvas.drawCircle(cx, cy, mInnerCircleRadius, mPaint);
            } else {
                mPaint.setStyle(Style.STROKE);
                mPaint.setColor(mColorNormal);
                mPaint.setAlpha(255);
                canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
            }
        }
        mPaint.setStyle(Style.STROKE);
        mPaint.setColor(colorSelected);
        mPaint.setStrokeWidth(mStrokeWidth * 2);
        if (mGesturePathVisible || !mIsRight) {
            if (!mPath.isEmpty()) {
                canvas.drawPath(mPath, mPaint);
            }
            if (lastCx != - 1 && lastCy != -1 && mMoveX != -1 && mMoveY != -1) {
                canvas.drawLine(lastCx, lastCy, mMoveX, mMoveY, mPaint);
            }
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        mMotionEventAction = action;
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                super.performClick();
                // 移除延时重置任务
                removeCallbacks(mResetRunnable);
                reset();
                computeSelectedCircleIndex(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                computeSelectedCircleIndex(x, y);
                mMoveX = x;
                mMoveY = y;
                break;
            case MotionEvent.ACTION_UP:
                if (mGestureLockViewListener != null) {
                    mIsRight = mGestureLockViewListener.validate(mPasswords);
                }
                mMoveX = -1;
                mMoveY = -1;
                // 延时重置
                postDelayed(mResetRunnable, mDelayReset);
                break;

        }
        invalidate();
        return true;
    }

    /**
     * 根据坐标计算选中某个手势圆
     *
     * @param x
     * @param y
     */
    private void computeSelectedCircleIndex(int x, int y) {
        for (int i = 0; i < mCircleSize; i++) {
            mCircleCenterPoint = mCircleCenterPoints.get(i);
            if (x > mCircleCenterPoint.x - mCircleRadius
                    && x < mCircleCenterPoint.x + mCircleRadius
                    && y > mCircleCenterPoint.y - mCircleRadius
                    && y < mCircleCenterPoint.y + mCircleRadius) {
                if (!mSelectedCircleIds.contains(i)) {
                    this.mSelectedCircleIds.add(i);
                    this.mPasswords.add(i + 1);// 密码索引=实心圆索引+1
                    if (mPath.isEmpty()) {
                        mPath.moveTo(mCircleCenterPoint.x,
                                mCircleCenterPoint.y);
                    } else {
                        mPath.lineTo(mCircleCenterPoint.x,
                                mCircleCenterPoint.y);
                    }
                    lastCx = mCircleCenterPoint.x;
                    lastCy = mCircleCenterPoint.y;
                    if (mGestureLockViewListener != null)
                        mGestureLockViewListener.onBlockSelected(i);
                }
                break;
            }
        }
    }

    /**
     * 清空实心圆点序号列表,并重绘
     */
    public void reset() {
        mSelectedCircleIds.clear();
        mPasswords.clear();
        mPath.reset();
        mIsRight = true;
        mMoveX = -1;
        mMoveY = -1;
        lastCx = -1;
        lastCy = -1;
        invalidate();
        if (mGestureLockViewListener != null)
            mGestureLockViewListener.onReset();
    }

    /**
     * 设置是否显示绘制的手势路径(默认为true)
     *
     * @param visible
     */
    public void setGesturePathVisible(boolean visible) {
        this.mGesturePathVisible = visible;
    }

    /**
     * 设置手势锁监听
     *
     * @param listener
     */
    public void setGestureLockViewListener(GestureLockViewListener listener) {
        mGestureLockViewListener = listener;
    }

    public static interface GestureLockViewListener {
        /**
         * 重置
         */
        void onReset();

        /**
         * 单独选中元素的Id(通常在设置密码时会用到)
         *
         * @param position
         */
        void onBlockSelected(int position);

        /**
         * 手势绘制结束，验证密码是否正确
         *
         * @param password 绘制的密码序列
         * @return 密码对错
         */
        boolean validate(List<Integer> password);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
