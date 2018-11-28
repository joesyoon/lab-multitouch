package edu.uw.animdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * An example SurfaceView for generating graphics on
 * @author Joel Ross
 * @version Spring 2017
 */
class DrawingSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defaultStyle: Int = 0) : SurfaceView(context, attrs, defaultStyle), SurfaceHolder.Callback {

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0 //size of the view

    private var bmp: Bitmap? = null //image to draw on

    private val mHolder: SurfaceHolder //the holder we're going to post updates to
    private val mRunnable: DrawingRunnable //the code that we'll want to run on a background thread
    private var mThread: Thread? = null //the background thread

    private val whitePaint: Paint //drawing variables (pre-defined for speed)
    private val goldPaint: Paint //drawing variables (pre-defined for speed)

    lateinit var ball: Ball //public for easy access

    var mMap:MutableMap<Float, Ball> = mutableMapOf()

    init {

        viewWidth = 1
        viewHeight = 1 //positive defaults; will be replaced when #surfaceChanged() is called

        // register our interest in hearing about changes to our surface
        mHolder = holder
        mHolder.addCallback(this)

        mRunnable = DrawingRunnable()

        //set up drawing variables ahead of time
        whitePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        whitePaint.color = Color.WHITE
        goldPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        goldPaint.color = Color.rgb(145, 123, 76)

        init()
    }

    /**
     * Initialize graphical drawing state
     */
    fun init() {
        //make ball
        ball = Ball((viewWidth / 2).toFloat(), (viewHeight / 2).toFloat(), 100f)
    }

    @Synchronized
    fun addTouch(pointerID:Float, x:Float, y:Float) {
        val mBall:Ball = Ball(x, y,100f)
        mMap.set(pointerID, mBall)
    }

    @Synchronized
    fun removeTouch(pointerID:Float) {
        mMap.remove(pointerID)
    }

    @Synchronized
    fun moveTouch(pointerID:Float, latestX:Float, latestY:Float) {
        mMap.get(pointerID)!!.x = latestX
        mMap.get(pointerID)!!.y = latestY
    }

    /**
     * Helper method for the "game loop"
     */
    fun update() {
        //update the "game state" here (move things around, etc.

        ball.cx += ball.dx //move
        ball.cy += ball.dy

        //slow down
        ball.dx *= 0.99f
        ball.dy *= 0.99f

        //        if(ball.dx < .1) ball.dx = 0;
        //        if(ball.dy < .1) ball.dy = 0;

        /* hit detection */
        if (ball.cx + ball.radius > viewWidth) { //left bound
            ball.cx = viewWidth - ball.radius
            ball.dx *= -1f
        } else if (ball.cx - ball.radius < 0) { //right bound
            ball.cx = ball.radius
            ball.dx *= -1f
        } else if (ball.cy + ball.radius > viewHeight) { //bottom bound
            ball.cy = viewHeight - ball.radius
            ball.dy *= -1f
        } else if (ball.cy - ball.radius < 0) { //top bound
            ball.cy = ball.radius
            ball.dy *= -1f
        }
    }


    /**
     * Helper method for the "render loop"
     * @param canvas The canvas to draw on
     */
    @Synchronized
    fun render(canvas: Canvas?) {
        if (canvas == null) return  //if we didn't get a valid canvas for whatever reason

        canvas.drawColor(Color.rgb(51, 10, 111)) //purple out the background

        canvas.drawCircle(ball.cx, ball.cy, ball.radius, whitePaint) //we can draw directly onto the canvas

        for(mBall in mMap.values) {
            canvas.drawCircle(mBall.x, mBall.y, mBall.radius, goldPaint)
        }

    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        //create and start the background updating thread
        Log.d(TAG, "Creating new drawing thread")
        mThread = Thread(mRunnable)
        mRunnable.setRunning(true) //turn on the runner
        mThread!!.start() //start up the thread when surface is created

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(mHolder) {
            //synchronized to keep this stuff atomic
            viewWidth = width
            viewHeight = height
            bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888) //new buffer to draw on

            init()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        mRunnable.setRunning(false) //turn off
        var retry = true
        while (retry) {
            try {
                mThread!!.join()
                retry = false
            } catch (e: InterruptedException) {
                //will try again...
            }

        }
        Log.d(TAG, "Drawing thread shut down")
    }

    /**
     * An inner class representing a runnable that does the drawing. Animation timing could go in here.
     * http://obviam.net/index.php/the-android-game-loop/ has some nice details about using timers to specify animation
     */
    inner class DrawingRunnable : Runnable {

        private var isRunning: Boolean = false //whether we're running or not (so we can "stop" the thread)

        fun setRunning(running: Boolean) {
            this.isRunning = running
        }

        override fun run() {
            var canvas: Canvas?
            while (isRunning) {
                canvas = null
                try {
                    canvas = mHolder.lockCanvas() //grab the current canvas
                    synchronized(mHolder) {
                        update() //update the game
                        render(canvas) //redraw the screen
                    }
                } finally { //no matter what (even if something goes wrong), make sure to push the drawing so isn't inconsistent
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }

    companion object {

        private val TAG = "SurfaceView"
    }
}
/**
 * We need to override all the constructors, since we don't know which will be called
 */