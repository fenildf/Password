/*
 * Copyright (c) 2015 [1076559197@qq.com | tchen0707@gmail.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ruolan.com.uselibrary.swipeback

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout

import java.util.ArrayList

import ruolan.com.uselibrary.R


class SwipeBackLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = R.attr.SwipeBackLayoutStyle) : FrameLayout(context, attrs) {

    private var mEdgeFlag: Int = 0

    /**
     * Threshold of scroll, we will close the activity, when scrollPercent over
     * this value;
     */
    private var mScrollThreshold = DEFAULT_SCROLL_THRESHOLD

    private var mActivity: Activity? = null

    private var mEnable = true

    private var mContentView: View? = null

    private val mDragHelper: ViewDragHelper

    private var mScrollPercent: Float = 0.toFloat()

    private var mContentLeft: Int = 0

    private var mContentTop: Int = 0

    /**
     * The set of listeners to be sent events through.
     */
    private var mListeners: MutableList<SwipeListener>? = null

    private var mScrimOpacity: Float = 0.toFloat()

    private var mScrimColor = DEFAULT_SCRIM_COLOR

    private var mInLayout: Boolean = false

    private val mTmpRect = Rect()

    /**
     * Edge being dragged
     */
    private var mTrackingEdge: Int = 0

    init {
        //创建ViewDragHelper辅助类  通过create创建
        mDragHelper = ViewDragHelper.create(this, ViewDragCallback())

        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeBackLayout, defStyle,
                R.style.SwipeBackLayout)

        val edgeSize = a.getDimensionPixelSize(R.styleable.SwipeBackLayout_edge_size, -1)
        if (edgeSize > 0) setEdgeSize(edgeSize)
        val mode = EDGE_FLAGS[a.getInt(R.styleable.SwipeBackLayout_edge_flag, 0)]
        setEdgeTrackingEnabled(mode)

        a.recycle()
        val density = resources.displayMetrics.density
        val minVel = MIN_FLING_VELOCITY * density
        mDragHelper.minVelocity = minVel
        mDragHelper.setMaxVelocity(minVel * 2f)
    }

    /**
     * Sets the sensitivity of the NavigationLayout.
     *
     * @param context The application context.
     * @param sensitivity value between 0 and 1, the final value for touchSlop =
     * ViewConfiguration.getScaledTouchSlop * (1 / s);
     */
    fun setSensitivity(context: Context, sensitivity: Float) {
        mDragHelper.setSensitivity(context, sensitivity)
    }

    /**
     * Set up contentView which will be moved by user gesture
     */
    private fun setContentView(view: View) {
        mContentView = view
    }

    fun setEnableGesture(enable: Boolean) {
        mEnable = enable
    }

    fun setEdgeTrackingEnabled(edgeFlags: Int) {
        mEdgeFlag = edgeFlags
        mDragHelper.setEdgeTrackingEnabled(mEdgeFlag)
    }

    /**
     * Set a color to use for the scrim that obscures primary content while a
     * drawer is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     */
    fun setScrimColor(color: Int) {
        mScrimColor = color
        invalidate()
    }

    /**
     * Set the size of an edge. This is the range in pixels along the edges of
     * this view that will actively detect edge touches or drags if edge
     * tracking is enabled.
     *
     * @param size The size of an edge in pixels
     */
    fun setEdgeSize(size: Int) {
        mDragHelper.edgeSize = size
    }

    /**
     * Register a callback to be invoked when a swipe event is sent to this
     * view.
     *
     * @param listener the swipe listener to attach to this view
     */
    @Deprecated("use {@link #addSwipeListener} instead")
    fun setSwipeListener(listener: SwipeListener) {
        addSwipeListener(listener)
    }

    /**
     * Add a callback to be invoked when a swipe event is sent to this view.
     *
     * @param listener the swipe listener to attach to this view
     */
    fun addSwipeListener(listener: SwipeListener) {
        if (mListeners == null) {
            mListeners = ArrayList()
        }
        mListeners!!.add(listener)
    }

    /**
     * Removes a listener from the set of listeners
     */
    fun removeSwipeListener(listener: SwipeListener) {
        if (mListeners == null) {
            return
        }
        mListeners!!.remove(listener)
    }

    interface SwipeListener {
        /**
         * Invoke when state change
         *
         * @param state flag to describe scroll state
         * @param scrollPercent scroll percent of this view
         * @see .STATE_IDLE
         *
         * @see .STATE_DRAGGING
         *
         * @see .STATE_SETTLING
         */
        fun onScrollStateChange(state: Int, scrollPercent: Float)

        /**
         * Invoke when edge touched
         *
         * @param edgeFlag edge flag describing the edge being touched
         * @see .EDGE_LEFT
         *
         * @see .EDGE_RIGHT
         *
         * @see .EDGE_BOTTOM
         */
        fun onEdgeTouch(edgeFlag: Int)

        /**
         * Invoke when scroll percent over the threshold for the first time
         */
        fun onScrollOverThreshold()
    }

    /**
     * Set scroll threshold, we will close the activity, when scrollPercent over
     * this value
     */
    fun setScrollThresHold(threshold: Float) {
        if (threshold >= 1.0f || threshold <= 0) {
            throw IllegalArgumentException("Threshold value should be between 0 and 1.0")
        }
        mScrollThreshold = threshold
    }

    /**
     * Scroll out contentView and finish the activity
     */
    fun scrollToFinishActivity() {
        val childWidth = mContentView!!.width
        val childHeight = mContentView!!.height

        var left = 0
        var top = 0
        if (mEdgeFlag and EDGE_LEFT != 0) {
            left = childWidth + OVERSCROLL_DISTANCE
            mTrackingEdge = EDGE_LEFT
        } else if (mEdgeFlag and EDGE_RIGHT != 0) {
            left = -childWidth - OVERSCROLL_DISTANCE
            mTrackingEdge = EDGE_RIGHT
        } else if (mEdgeFlag and EDGE_BOTTOM != 0) {
            top = -childHeight - OVERSCROLL_DISTANCE
            mTrackingEdge = EDGE_BOTTOM
        }

        mDragHelper.smoothSlideViewTo(mContentView!!, left, top)
        invalidate()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!mEnable) {
            return false
        }
        try {
            return mDragHelper.shouldInterceptTouchEvent(event)
        } catch (e: ArrayIndexOutOfBoundsException) {
            // FIXME: handle exception
            // issues #9
            return false
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mEnable) {
            return false
        }
        mDragHelper.processTouchEvent(event)
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mInLayout = true
        if (mContentView != null) {
            mContentView!!.layout(mContentLeft, mContentTop, mContentLeft + mContentView!!.measuredWidth,
                    mContentTop + mContentView!!.measuredHeight)
        }
        mInLayout = false
    }

    override fun requestLayout() {
        if (!mInLayout) {
            super.requestLayout()
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val drawContent = child === mContentView

        val ret = super.drawChild(canvas, child, drawingTime)
        if (mScrimOpacity > 0
                && drawContent
                && mDragHelper.viewDragState != ViewDragHelper.STATE_IDLE) {
            drawScrim(canvas, child)
        }
        return ret
    }

    private fun drawScrim(canvas: Canvas, child: View) {
        val baseAlpha = (mScrimColor and -0x1000000).ushr(24)
        val alpha = (baseAlpha * mScrimOpacity).toInt()
        val color = alpha shl 24 or (mScrimColor and 0xffffff)

        if (mTrackingEdge and EDGE_LEFT != 0) {
            canvas.clipRect(0, 0, child.left, height)
        } else if (mTrackingEdge and EDGE_RIGHT != 0) {
            canvas.clipRect(child.right, 0, right, height)
        } else if (mTrackingEdge and EDGE_BOTTOM != 0) {
            canvas.clipRect(child.left, child.bottom, right, height)
        }
        canvas.drawColor(color)
    }

    fun attachToActivity(activity: Activity) {
        mActivity = activity
        val a = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val background = a.getResourceId(0, 0)
        a.recycle()

        val decor = activity.window.decorView.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup
        val decorChild = decor.getChildAt(0) as ViewGroup
        decorChild.setBackgroundResource(background)
        decor.removeView(decorChild)
        addView(decorChild)
        setContentView(decorChild)
        decor.addView(this)
    }

    override fun computeScroll() {
        mScrimOpacity = 1 - mScrollPercent
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private inner class ViewDragCallback : ViewDragHelper.Callback() {
        private var mIsScrollOverValid: Boolean = false

        override fun tryCaptureView(view: View, i: Int): Boolean {
            val ret = mDragHelper.isEdgeTouched(mEdgeFlag, i)
            if (ret) {
                if (mDragHelper.isEdgeTouched(EDGE_LEFT, i)) {
                    mTrackingEdge = EDGE_LEFT
                } else if (mDragHelper.isEdgeTouched(EDGE_RIGHT, i)) {
                    mTrackingEdge = EDGE_RIGHT
                } else if (mDragHelper.isEdgeTouched(EDGE_BOTTOM, i)) {
                    mTrackingEdge = EDGE_BOTTOM
                }
                if (mListeners != null && !mListeners!!.isEmpty()) {
                    for (listener in mListeners!!) {
                        listener.onEdgeTouch(mTrackingEdge)
                    }
                }
                mIsScrollOverValid = true
            }
            return ret
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return mEdgeFlag and (EDGE_LEFT or EDGE_RIGHT)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return mEdgeFlag and EDGE_BOTTOM
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (mTrackingEdge and EDGE_LEFT != 0) {
                mScrollPercent = Math.abs(left.toFloat() / mContentView!!.width)
            } else if (mTrackingEdge and EDGE_RIGHT != 0) {
                mScrollPercent = Math.abs(left.toFloat() / mContentView!!.width)
            } else if (mTrackingEdge and EDGE_BOTTOM != 0) {
                mScrollPercent = Math.abs(top.toFloat() / mContentView!!.height)
            }
            mContentLeft = left
            mContentTop = top
            invalidate()
            if (mScrollPercent < mScrollThreshold && !mIsScrollOverValid) {
                mIsScrollOverValid = true
            }
            if (mListeners != null
                    && !mListeners!!.isEmpty()
                    && mDragHelper.viewDragState == STATE_DRAGGING
                    && mScrollPercent >= mScrollThreshold
                    && mIsScrollOverValid) {
                mIsScrollOverValid = false
                for (listener in mListeners!!) {
                    listener.onScrollOverThreshold()
                }
            }

            if (mScrollPercent >= 1) {
                if (!mActivity!!.isFinishing) mActivity!!.finish()
            }
        }

        override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
            val childWidth = releasedChild!!.width
            val childHeight = releasedChild!!.height

            var left = 0
            var top = 0
            if (mTrackingEdge and EDGE_LEFT != 0) {
                left = if (xvel > 0 || xvel == 0f && mScrollPercent > mScrollThreshold)
                    childWidth + OVERSCROLL_DISTANCE
                else
                    0
            } else if (mTrackingEdge and EDGE_RIGHT != 0) {
                left = if (xvel < 0 || xvel == 0f && mScrollPercent > mScrollThreshold)
                    -(childWidth + OVERSCROLL_DISTANCE)
                else
                    0
            } else if (mTrackingEdge and EDGE_BOTTOM != 0) {
                top = if (yvel < 0 || yvel == 0f && mScrollPercent > mScrollThreshold)
                    -(childHeight + OVERSCROLL_DISTANCE)
                else
                    0
            }

            mDragHelper.settleCapturedViewAt(left, top)
            invalidate()
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            var ret = 0
            if (mTrackingEdge and EDGE_LEFT != 0) {
                ret = Math.min(child.width, Math.max(left, 0))
            } else if (mTrackingEdge and EDGE_RIGHT != 0) {
                ret = Math.min(0, Math.max(left, -child.width))
            }
            return ret
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            var ret = 0
            if (mTrackingEdge and EDGE_BOTTOM != 0) {
                ret = Math.min(0, Math.max(top, -child.height))
            }
            return ret
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            if (mListeners != null && !mListeners!!.isEmpty()) {
                for (listener in mListeners!!) {
                    listener.onScrollStateChange(state, mScrollPercent)
                }
            }
        }
    }

    companion object {
        /**
         * Minimum velocity(速度) that will be detected as a fling
         */
        private val MIN_FLING_VELOCITY = 400 // dips per second

        private val DEFAULT_SCRIM_COLOR = -0x67000000

        private val FULL_ALPHA = 255

        /**
         * Edge flag indicating that the left edge should be affected.
         */
        val EDGE_LEFT = ViewDragHelper.EDGE_LEFT

        /**
         * Edge flag indicating that the right edge should be affected.
         */
        val EDGE_RIGHT = ViewDragHelper.EDGE_RIGHT

        /**
         * Edge flag indicating that the bottom edge should be affected.
         */
        val EDGE_BOTTOM = ViewDragHelper.EDGE_BOTTOM

        /**
         * Edge flag set indicating all edges should be affected.
         */
        val EDGE_ALL = EDGE_LEFT or EDGE_RIGHT or EDGE_BOTTOM

        /**
         * A view is not currently being dragged or animating as a result of a
         * fling/snap.
         */
        val STATE_IDLE = ViewDragHelper.STATE_IDLE

        /**
         * A view is currently being dragged. The position is currently changing as
         * a result of user input or simulated user input.
         */
        val STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING

        /**
         * A view is currently settling into place as a result of a fling or
         * predefined non-interactive motion.
         */
        val STATE_SETTLING = ViewDragHelper.STATE_SETTLING

        /**
         * Default threshold of scroll
         */
        private val DEFAULT_SCROLL_THRESHOLD = 0.3f

        private val OVERSCROLL_DISTANCE = 10

        private val EDGE_FLAGS = intArrayOf(EDGE_LEFT, EDGE_RIGHT, EDGE_BOTTOM, EDGE_ALL)
    }
}
