package ca.rplnet.btopwidget

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.net.NetworkInterface
import java.util.Collections

data class NetInfo(
    val connType: String,       // "wifi" / "mobile" / "aucune"
    val ipAddress: String?,
    val carrier: String?,
    val airplaneMode: Boolean,
    val wifiSsid: String?,      // null si permission localisation pas accordee
    val wifiRssi: Int?,         // dBm, null si pas dispo
    val btConnectedCount: Int?  // null si permission bluetooth pas accordee
)

object NetworkInfo {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBluetoothPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true // pas requis avant API 31
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun collect(context: Context): NetInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val connType = when {
            caps == null -> "aucune"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            else -> "autre"
        }

        val ip = getLocalIp()

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val carrier = tm?.networkOperatorName?.ifBlank { null }

        val airplane = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON) != 0
        } catch (e: Exception) {
            false
        }

        var ssid: String? = null
        var rssi: Int? = null
        if (hasLocationPermission(context)) {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wm.connectionInfo
                val rawSsid = info.ssid?.trim('"')
                if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid
                    rssi = info.rssi
                }
            } catch (e: Exception) {
                // pas dispo sur ce device/etat
            }
        }

        var btCount: Int? = null
        if (hasBluetoothPermission(context)) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    val bondedDevices = adapter.bondedDevices
                    btCount = bondedDevices?.size ?: 0
                }
            } catch (e: SecurityException) {
                // permission refusee entre temps, ignore
            }
        }

        return NetInfo(connType, ip, carrier, airplane, ssid, rssi, btCount)
    }

    private fun getLocalIp(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
