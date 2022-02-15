package com.tonybuilder.trackpattern;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import androidx.core.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lixiang on 17-9-15.
 */

public class TouchPatternView extends View {

    private static final String TAG = "TouchPaternView";
    private static final int MAX_TOUCH_INDEX = 9;

    // Hold data for active touch pointer IDs
    private SparseArray<TouchTrack> mTouches = null;

    // paint for ref line
    private Paint mRefLinePaint = new Paint();
    // paint for text
    private Paint mTextPaint = new Paint();
    // paint for dot
    private Paint mDotPaint = new Paint();

    private boolean isTracking = false;

    private VelocityTracker mVelocityTracker = null;

    class TouchTrack {
        private List<TouchPoint> mTrackPointsList;

        public TouchTrack() {
            mTrackPointsList = new LinkedList<>();
        }

        public void addPoint(TouchPoint tp) {
            mTrackPointsList.add(tp);
        }
    }

    class TouchPoint extends PointF {
        int mAction;
        int mTrackId;
        float eventTime;

        TouchPoint(float x, float y, int action, int id, float time) {
            this.x = x;
            this.y = y;
            this.mAction = action;
            this.mTrackId = id;
            this.eventTime = time;
        }
    }

    public TouchPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialisePaint();
        mVelocityTracker = VelocityTracker.obtain();
    }

    private static final int[] COLORS = {
            0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444,
            0xFF0099CC, 0xFF9933CC, 0xFF669900, 0xFFFF8800, 0xFFCC0000
    };

    /**
     * Sets up the required {@link Paint} objects for the screen density of this
     * device.
     */
    private void initialisePaint() {
        mDotPaint.setStrokeWidth(10);
        mDotPaint.setColor(COLORS[0]);
        mDotPaint.setStyle(Paint.Style.STROKE);

        mRefLinePaint.setColor(COLORS[9]);
        mRefLinePaint.setStyle(Paint.Style.STROKE);
        mRefLinePaint.setStrokeWidth(5);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(event.getActionIndex());

        String string = "id ActionIndex=" + event.getActionIndex()
                + ", PointerId=" + pointerId
                + ", maskedAction: " + action
                + ", x=" + event.getX(event.getActionIndex()) + ", y=" + event.getY(event.getActionIndex())
                + ", count=" + event.getPointerCount();
        Log.e("onTouchEvent ACTION", string);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isTracking = true;
                initTouches();
                TouchTrack track = new TouchTrack();
                TouchPoint tp = new TouchPoint(event.getX(event.getActionIndex()),
                        event.getY(event.getActionIndex()),
                        action,
                        pointerId,
                        event.getEventTime());
                track.addPoint(tp);
                mTouches.put(pointerId, track);

                // track velocity
                if(mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the
                    // velocity of a motion.
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker back to its initial state.
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                TouchTrack pointerTrack = new TouchTrack();
                TouchPoint pointerTp = new TouchPoint(event.getX(event.getActionIndex()),
                        event.getY(event.getActionIndex()),
                        action,
                        pointerId,
                        event.getEventTime());
                pointerTrack.addPoint(pointerTp);
                mTouches.put(pointerId, pointerTrack);

                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_UP:
                isTracking = false;
                TouchPoint upTp = new TouchPoint(event.getX(event.getActionIndex()),
                        event.getY(event.getActionIndex()),
                        action,
                        pointerId,
                        event.getEventTime());
                TouchTrack upTrack = mTouches.get(pointerId);
                if (upTrack != null) {
                    upTrack.addPoint(upTp);
                } else {
                    Log.e(TAG, "upTrack == null");
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                TouchPoint ptTp = new TouchPoint(event.getX(event.getActionIndex()),
                        event.getY(event.getActionIndex()),
                        action,
                        pointerId,
                        event.getEventTime());
                TouchTrack ptTrack = mTouches.get(pointerId);
                if (ptTrack != null) {
                    ptTrack.addPoint(ptTp);
                } else {
                    Log.e(TAG, "ptTrack == null");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // handle batched motion events.
                int historySize = event.getHistorySize();
                int pointerCount = event.getPointerCount();
                TouchTrack moveTrack;
                TouchPoint moveTp;

                //Log.e(TAG, "historySize = " + historySize);
                for (int h = 0; h < historySize; h++) {
                    for (int p = 0; p < pointerCount; p++) {
                        moveTrack = mTouches.get(event.getPointerId(p));
                        if (moveTrack != null) {
                            moveTp = new TouchPoint(event.getHistoricalX(p, h),
                                    event.getHistoricalY(p, h),
                                    MotionEvent.ACTION_MOVE,
                                    event.getPointerId(p),
                                    event.getHistoricalEventTime(h));
                            moveTrack.addPoint(moveTp);
                        } else {
                            Log.e(TAG, "move track == null, track id = " + p);
                        }
                    }
                }

                for (int p = 0; p < pointerCount; p++) {
                    moveTrack = mTouches.get(event.getPointerId(p));
                    if (moveTrack != null) {
                        moveTp = new TouchPoint(event.getX(p),
                                event.getY(p),
                                MotionEvent.ACTION_MOVE,
                                event.getPointerId(p),
                                event.getEventTime());
                        moveTrack.addPoint(moveTp);
                    } else {
                        Log.e(TAG, "move track == null, track id = " + p);
                    }
                }

                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);

                Log.d(TAG, "show velocity of every track");
                for (int p = 0; p < pointerCount; p++) {
                    // Log velocity of pixels per second
                    Log.e(TAG, "X velocity\t" + mVelocityTracker.getXVelocity(event.getPointerId(p)) + "\tY velocity\t" + mVelocityTracker.getYVelocity(event.getPointerId(p)));
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.recycle();
                break;
            default:
                break;
        }

        // trigger redraw on UI thread
        this.postInvalidate();
        return true;
    }

    private void initTouches() {
        // SparseArray for touch events, indexed by touch id
        mTouches = new SparseArray<TouchTrack>(MAX_TOUCH_INDEX + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isTracking) {
            canvas.drawColor(Color.WHITE);
        }

        if (mTouches == null) {
            return;
        }

        for (int i = 0; i < mTouches.size(); i++) {
            int id = mTouches.keyAt(i);
            TouchTrack track = mTouches.valueAt(i);
            if (track == null) {
                Log.e(TAG, "error track == null");
                return;
            }
            for (int j = 0; j < track.mTrackPointsList.size(); j++) {
                TouchPoint p = track.mTrackPointsList.get(j);
                mDotPaint.setColor(COLORS[p.mTrackId % 10]);
                canvas.drawPoint(p.x, p.y, mDotPaint);
            }
        }

        if (Build.MODEL.contains("DUK-")) {
            //draw ref line
            canvas.drawLine(1395,0,1395,2560,mRefLinePaint);
            canvas.drawLine(55,0, 55,2560, mRefLinePaint);
            mDotPaint.setStrokeWidth(10);
            for (int j=0; j < 2560; j++) {
                canvas.drawPoint(1395, j, mDotPaint);
                j += 50;
            }
            mDotPaint.setStrokeWidth(5);
        } else if (Build.MODEL.contains("MIX 2")) {
            canvas.drawLine(1395,0,1395,2560,mRefLinePaint);
        }

        Log.e(TAG, "Build.MODEL" + Build.MODEL);
    }
}
