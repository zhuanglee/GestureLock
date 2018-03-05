package cn.lzh.gesturelock.demo

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import cn.lzh.gesturelock.view.GestureLockPreview
import cn.lzh.gesturelock.view.GestureLockView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val gestureLockPreview = findViewById<GestureLockPreview>(R.id.gestureLockPreview)
        val gestureLockView = findViewById<GestureLockView>(R.id.gestureLockView)
        gestureLockView.setGestureLockViewListener(object : GestureLockView.GestureLockViewListener {
            override fun onReset() {
                gestureLockPreview.reset()
            }

            override fun onBlockSelected(position: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    mVibrator.vibrate(effect)
                } else {
                    mVibrator.vibrate(30)
                }
                gestureLockPreview.addSelectedPoint(position)
            }

            override fun validate(password: List<Int>): Boolean {
                return password.size == 9
            }
        })
    }
}
