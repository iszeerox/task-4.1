package com.example.task41

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.CircularProgressIndicator


class MainActivity : AppCompatActivity() {

    private lateinit var remainingTimer: TextView
    private lateinit var notifyUser: TextView
    private lateinit var workoutTimer: EditText
    private lateinit var restTimer: EditText
    private lateinit var workoutProgress: CircularProgressIndicator
    private lateinit var startTimer: Button
    private lateinit var stopTimer: Button
    private lateinit var resetTimer: Button
    private var countDownTimer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var builder: NotificationCompat.Builder
    private val STOP_TIMER = "STOP_TIMER"
    private lateinit var notificationLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        remainingTimer = findViewById(R.id.remainingTimer)
        notifyUser = findViewById(R.id.notifyUser)
        workoutTimer = findViewById(R.id.workoutTimer)
        restTimer = findViewById(R.id.restTimer)
        workoutProgress = findViewById(R.id.workoutProgress)
        startTimer = findViewById(R.id.startTimer)
        stopTimer = findViewById(R.id.stopTimer)
        resetTimer = findViewById(R.id.resetTimer)

        if (intent?.action == STOP_TIMER) {
            countDownTimer?.cancel()
            notificationManager?.cancel(1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            notificationRequest()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        startTimer.setOnClickListener {
            if (workoutTimer.text.toString() == "" || restTimer.text.toString() == "") {
                if (workoutTimer.text.toString() == "")
                    Toast.makeText(this, "Set workout timer is empty", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "Set rest timer is empty", Toast.LENGTH_LONG).show()
            } else {
                workoutProgress.max =
                    ((workoutTimer.text.toString().toFloat() / 1.65) * 100).toInt()
                countDownTimer = createTimer(workoutTimer.text.toString().toLong())
                countDownTimer?.start()
                notifyUser.text = "Workout now"
            }
        }

        stopTimer.setOnClickListener {
            countDownTimer?.cancel()
        }

        resetTimer.setOnClickListener {
            countDownTimer?.cancel()
            remainingTimer.text = "00:00:00"
            notifyUser.text = ""
            workoutTimer.setText("")
            restTimer.setText("")
            workoutProgress.progress = 0
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun notificationRequest() {
        notificationLauncher = registerForActivityResult(RequestPermission()) {
            if (it) Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_DENIED
        )
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private var isWorkingOut = true

    private fun createTimer(time: Long): CountDownTimer {
        return object : CountDownTimer(time * 60 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingTime = (millisUntilFinished / 1000).toInt()
                val minutes = remainingTime / 60
                val seconds = remainingTime % 60
                remainingTimer.text = String.format("%02d:%02d", minutes, seconds)
                workoutProgress.progress = remainingTime
                Log.e("TAG", "onTick: $remainingTime")
            }

            override fun onFinish() {
                countDownTimer?.cancel()

                if (isWorkingOut) {
                    isWorkingOut = false
                    playRingtone()
                    workoutProgress.max =
                        ((restTimer.text.toString().toFloat() / 1.65) * 100).toInt()
                    countDownTimer = createTimer(restTimer.text.toString().toLong())
                    notifyUser.text = "Rest now"
                } else {
                    isWorkingOut = true
                    playRingtone()
                    workoutProgress.max =
                        ((workoutTimer.text.toString().toFloat() / 1.65) * 100).toInt()
                    countDownTimer = createTimer(workoutTimer.text.toString().toLong())
                    notifyUser.text = "Workout now"
                }

                workoutNotification()

                countDownTimer?.start()
            }
        }
    }

    private fun workoutNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(
                "workout_channel",
                "Workout Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val pendingIntent = PendingIntent.getService(
            this, 1, Intent(this, MainActivity::class.java).apply {
                action = STOP_TIMER
            }, PendingIntent.FLAG_MUTABLE
        )

        builder = NotificationCompat.Builder(this, "workout_channel")
            .setContentTitle(if (!isWorkingOut) "Workout Completed!" else "Rest Completed! work now")
            .setContentText(if (!isWorkingOut) "Rest now" else "Work Now")
            .setSmallIcon(if (!isWorkingOut) R.drawable.ic_rest else R.drawable.ic_work_out)
//            .addAction(
//                R.drawable.baseline_stop_24, "Stop Timer",
//                pendingIntent
//            )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }


    private fun playRingtone() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
        ringtone.play()

        Handler(Looper.getMainLooper()).postDelayed({
            ringtone.stop()
        }, 2000)
    }
}

