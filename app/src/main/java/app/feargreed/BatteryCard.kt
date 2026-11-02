package app.feargreed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

/** Shows whether background refresh can be throttled and offers the system exemption dialog. */
object BatteryCard {

    fun bind(activity: Activity) {
        val exempt = activity.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(activity.packageName)
        activity.findViewById<TextView>(R.id.battery_status).setText(
            if (exempt) R.string.battery_exempt else R.string.battery_not_exempt,
        )
        activity.findViewById<Button>(R.id.battery_request).apply {
            visibility = if (exempt) Button.GONE else Button.VISIBLE
            setOnClickListener { requestExemption(activity) }
        }
        activity.findViewById<Button>(R.id.battery_details).setOnClickListener {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")),
            )
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestExemption(activity: Activity) {
        val request = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${activity.packageName}"),
        )
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val intent = if (request.resolveActivity(activity.packageManager) != null) request else fallback
        activity.startActivity(intent)
    }
}
