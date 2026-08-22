package com.qiblah.direction

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.*
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.core.app.ActivityCompat
import kotlin.math.*

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accel = FloatArray(3)
    private var magnet = FloatArray(3)
    private var haveAccel = false
    private var haveMagnet = false
    private lateinit var compass: QiblahView
    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compass = QiblahView(this)
        setContentView(compass)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        requestLocation()
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 7)
            return
        }
        val lm = locationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.accuracy < best!!.accuracy)) best = l
                lm.requestLocationUpdates(p, 3000L, 10f) { loc -> compass.setLocation(loc) }
            } catch (_: Exception) {}
        }
        if (best != null) compass.setLocation(best!!)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) requestLocation()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { accel = e.values.clone(); haveAccel = true }
            Sensor.TYPE_MAGNETIC_FIELD -> { magnet = e.values.clone(); haveMagnet = true }
        }
        if (haveAccel && haveMagnet) {
            val r = FloatArray(9); val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, accel, magnet)) {
                val o = FloatArray(3); SensorManager.getOrientation(r, o)
                var az = Math.toDegrees(o[0].toDouble()).toFloat()
                if (az < 0) az += 360f
                compass.setHeading(az)
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class QiblahView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var heading = 0f
    private var qiblah = 0f
    private var hasLocation = false
    private var lat = 0.0
    private var lon = 0.0
    private var aligned = false
    private val kaabaLat = 21.422487
    private val kaabaLon = 39.826206

    fun setLocation(l: Location) {
        lat = l.latitude; lon = l.longitude
        qiblah = bearingToKaaba(lat, lon).toFloat(); hasLocation = true; invalidate()
    }
    fun setHeading(h: Float) { heading = h; invalidate() }

    private fun bearingToKaaba(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(kaabaLat)
        val dl = Math.toRadians(kaabaLon - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Color.rgb(247,243,232))
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(11,61,46); paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = w * .075f; c.drawText("Qiblah Direction", w/2, h*.10f, paint)
        paint.typeface = Typeface.DEFAULT; paint.textSize = w*.038f
        paint.color = Color.rgb(90,85,72); c.drawText("Face the arrow toward the Kaaba", w/2, h*.15f, paint)

        val cx=w/2; val cy=h*.48f; val radius=min(w*.39f,h*.25f)
        paint.style=Paint.Style.FILL; paint.color=Color.WHITE; c.drawCircle(cx,cy,radius,paint)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=w*.012f; paint.color=Color.rgb(201,164,75); c.drawCircle(cx,cy,radius,paint)
        paint.style=Paint.Style.FILL
        val dirs=arrayOf("N","E","S","W")
        for(i in 0..3){ val a=Math.toRadians((i*90-90).toDouble()); paint.color=Color.rgb(11,61,46); paint.textSize=w*.045f; paint.typeface=Typeface.DEFAULT_BOLD; c.drawText(dirs[i],cx+cos(a).toFloat()*radius*.82f,cy+sin(a).toFloat()*radius*.82f+w*.015f,paint) }

        if(hasLocation){
            val relative=((qiblah-heading+540)%360)-180
            c.save(); c.rotate(relative,cx,cy)
            val p=Path(); p.moveTo(cx,cy-radius*.72f); p.lineTo(cx-radius*.09f,cy-radius*.38f); p.lineTo(cx-radius*.025f,cy-radius*.42f); p.lineTo(cx-radius*.025f,cy+radius*.42f); p.lineTo(cx+radius*.025f,cy+radius*.42f); p.lineTo(cx+radius*.025f,cy-radius*.42f); p.lineTo(cx+radius*.09f,cy-radius*.38f); p.close()
            paint.color=Color.rgb(201,164,75); c.drawPath(p,paint)
            paint.color=Color.BLACK; c.drawRoundRect(cx-radius*.12f,cy-radius*.25f,cx+radius*.12f,cy-radius*.08f,10f,10f,paint)
            paint.color=Color.rgb(201,164,75); c.drawRect(cx-radius*.12f,cy-radius*.19f,cx+radius*.12f,cy-radius*.16f,paint)
            c.restore()

            val diff=abs((((qiblah-heading+540)%360)-180))
            val nowAligned=diff<3f
            paint.textSize=w*.048f; paint.typeface=Typeface.DEFAULT_BOLD
            paint.color=if(nowAligned) Color.rgb(11,110,70) else Color.rgb(11,61,46)
            c.drawText(if(nowAligned) "✓ Qiblah aligned" else "Turn phone to follow the gold arrow",w/2,h*.78f,paint)
            paint.textSize=w*.040f; paint.typeface=Typeface.DEFAULT
            paint.color=Color.DKGRAY; c.drawText("Qiblah bearing: ${qiblah.roundToInt()}°",w/2,h*.84f,paint)
            paint.textSize=w*.031f; c.drawText("Location: %.4f, %.4f".format(lat,lon),w/2,h*.89f,paint)
            if(nowAligned && !aligned){
                aligned=true
                try { val v=context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator; if(android.os.Build.VERSION.SDK_INT>=26) v.vibrate(VibrationEffect.createOneShot(80,80)) else @Suppress("DEPRECATION") v.vibrate(80) } catch(_:Exception){}
            } else if(!nowAligned) aligned=false
        } else {
            paint.color=Color.DKGRAY; paint.textSize=w*.042f; paint.typeface=Typeface.DEFAULT
            c.drawText("Allow location access to calculate Qiblah",w/2,h*.80f,paint)
        }
        paint.textSize=w*.027f; paint.color=Color.GRAY
        c.drawText("For best accuracy, keep the phone flat and away from metal.",w/2,h*.96f,paint)
    }
}
