package edu.uw.animdemo

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.MotionEventCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {

    private var view: DrawingSurfaceView? = null

    private var radiusAnim: AnimatorSet? = null

    private var mDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view = findViewById(R.id.drawingView) as DrawingSurfaceView?

        radiusAnim = AnimatorInflater.loadAnimator(this, R.animator.animations) as AnimatorSet

        mDetector = GestureDetectorCompat(this, MyGestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //Log.v(TAG, event.toString());

        val gesture = mDetector!!.onTouchEvent(event) //ask the detector to handle instead
        //if(gesture) return true; //if we don't also want to handle

        val x = event.x
        val y = event.y - supportActionBar!!.height //closer to center...
        val index = MotionEventCompat.getActionIndex(event)
        val count = MotionEventCompat.getPointerCount(event)
        val pointerID:Float = MotionEventCompat.getPointerId(event, index) as Float
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN //put finger down
            -> {
                //Log.v(TAG, "finger down");
                view!!.addTouch(pointerID, x, y)
                val xAnim = ObjectAnimator.ofFloat(view!!.ball, "x", x)
                xAnim.duration = 1000
                val yAnim = ObjectAnimator.ofFloat(view!!.ball, "y", y)
                yAnim.duration = 1500 //y moves 1.5x slower

                val set = AnimatorSet()
                set.playTogether(yAnim, xAnim)
                set.start()

                //                view.ball.cx = x;
                //                view.ball.cy = y;
                //                view.ball.dx = (x - view.ball.cx)/Math.abs(x - view.ball.cx)*30;
                //                view.ball.dy = (y - view.ball.cy)/Math.abs(y - view.ball.cy)*30;
                return true
            }
            MotionEvent.ACTION_MOVE //move finger
            -> {
                for(i in 0..count) {
                    val movePID = MotionEventCompat.getPointerId(event, i) as Float
                    view!!.moveTouch(movePID, x, y)
                }
                //Log.v(TAG, "finger move");
                //                view.ball.cx = x;
                //                view.ball.cy = y;
                return true}
            MotionEvent.ACTION_UP //lift finger up
                , MotionEvent.ACTION_CANCEL //aborted gesture
                , MotionEvent.ACTION_OUTSIDE //outside bounds
            ->  {view!!.removeTouch(pointerID)
                return super.onTouchEvent(event)}
            else -> return super.onTouchEvent(event)
        }
    }

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true //recommended practice
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

            val scaleFactor = .03f

            //fling!
            Log.v(TAG, "Fling! $velocityX, $velocityY")
            view!!.ball.dx = -1f * velocityX * scaleFactor
            view!!.ball.dy = -1f * velocityY * scaleFactor

            return true //we got this
        }
    }


    /** Menus  */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_pulse -> {
                //make the ball change size!
                if (!radiusAnim!!.isRunning) {
                    radiusAnim!!.setTarget(view!!.ball)
                    radiusAnim!!.start()
                } else {
                    radiusAnim!!.end()
                }
                return true
            }
            R.id.menu_button -> {
                startActivity(Intent(this@MainActivity, ButtonActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {

        private val TAG = "Main"
    }
}
