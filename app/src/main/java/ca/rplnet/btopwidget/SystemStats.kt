package ca.rplnet.btopwidget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Stats(
    val ramUsedPct: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val batteryPct: Int,
    val batteryVoltageV: Double,
    val batteryTempC: Double,
    val charging: Boolean,
    val storageUsedPct: Int,
    val storageUsedGb: Double,
    val storageTotalGb: Double,
    val cpuPct: Int?,
    val cpuSource: String,
    val uptimeStr: String,
    val netDownKbps: Long,
    val netUpKbps: Long,
    val clock: String,
    val date: String
)

object SystemStats {

    // dernière lecture, pour calculer le débit réseau delta entre deux appels
    private var lastRxBytes: Long = -1
    private var lastTxBytes: Long = -1
    private var lastSampleTime: Long = -1

    // dernière lecture /proc/stat pour delta CPU
    private var lastCpuTotal: Long = -1
    private var lastCpuIdle: Long = -1

    fun collect(context: Context): Stats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramTotalMb = memInfo.totalMem / (1024 * 1024)
        val ramUsedMb = ramTotalMb - (memInfo.availMem / (1024 * 1024))
        val ramUsedPct = if (ramTotalMb > 0) (ramUsedMb * 100 / ramTotalMb).toInt() else 0

        val batteryIntent = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val batteryVoltageV = if (voltageMv > 0) voltageMv / 1000.0 else 0.0
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTempC = if (tempTenths > 0) tempTenths / 10.0 else 0.0

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val availBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val usedBytes = totalBytes - availBytes
        val storageTotalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val storageUsedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val storageUsedPct = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0

        val (cpuPct, cpuSource) = readCpuPercent()

        val uptimeMs = SystemClock.elapsedRealtime()
        val uptimeStr = formatUptime(uptimeMs)

        val (downKbps, upKbps) = readNetworkDelta()

        val now = Date()
        val clock = SimpleDateFormat("HH:mm:ss", Locale.CANADA_FRENCH).format(now)
        val date = SimpleDateFormat("EEE dd MMM", Locale.CANADA_FRENCH).format(now)

        return Stats(
            ramUsedPct = ramUsedPct,
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            batteryPct = batteryPct,
            batteryVoltageV = batteryVoltageV,
            batteryTempC = batteryTempC,
            charging = charging,
            storageUsedPct = storageUsedPct,
            storageUsedGb = storageUsedGb,
            storageTotalGb = storageTotalGb,
            cpuPct = cpuPct,
            cpuSource = cpuSource,
            uptimeStr = uptimeStr,
            netDownKbps = downKbps,
            netUpKbps = upKbps,
            clock = clock,
            date = date
        )
    }

    // /proc/stat est bloqué (permission denied) sur beaucoup de devices depuis
    // Android 8+ (SELinux). On tente quand même, et si ça echoue on tombe sur
    // /proc/loadavg — la meme technique que KWGT et la plupart des apps de
    // monitoring non-root, car /proc/loadavg reste generalement lisible meme
    // quand /proc/stat est bloque.
    private fun readCpuPercent(): Pair<Int?, String> {
        readCpuStat()?.let { return Pair(it, "stat") }
        readLoadAvg()?.let { return Pair(it, "loadavg") }
        return Pair(null, "bloqué")
    }

    private fun readCpuStat(): Int? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split(" +".toRegex()).filter { it.isNotBlank() }
            // cpu user nice system idle iowait irq softirq
            val user = toks[1].toLong()
            val nice = toks[2].toLong()
            val system = toks[3].toLong()
            val idle = toks[4].toLong()
            val iowait = toks.getOrNull(5)?.toLong() ?: 0
            val irq = toks.getOrNull(6)?.toLong() ?: 0
            val softirq = toks.getOrNull(7)?.toLong() ?: 0

            val idleTotal = idle + iowait
            val total = user + nice + system + idleTotal + irq + softirq

            if (lastCpuTotal < 0) {
                lastCpuTotal = total
                lastCpuIdle = idleTotal
                return null // premier appel, pas de delta encore
            }
            val totalDelta = total - lastCpuTotal
            val idleDelta = idleTotal - lastCpuIdle
            lastCpuTotal = total
            lastCpuIdle = idleTotal

            if (totalDelta <= 0) return null
            (100 * (totalDelta - idleDelta) / totalDelta).toInt()
        } catch (e: Exception) {
            null // /proc/stat bloque sur ce device, pas une erreur a signaler
        }
    }

    // charge systeme (1min) normalisee par le nombre de coeurs, faute de mieux.
    // Pas un vrai % d'utilisation instantane comme /proc/stat, mais un proxy
    // raisonnable — c'est ce que la plupart des widgets non-root affichent.
    private fun readLoadAvg(): Int? {
        return try {
            val reader = RandomAccessFile("/proc/loadavg", "r")
            val line = reader.readLine()
            reader.close()
            val load1min = line.split(" ")[0].toFloat()
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            ((load1min / cores) * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            null // vraiment bloque sur ce device, la on affiche n/a honnetement
        }
    }

    private fun readNetworkDelta(): Pair<Long, Long> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        if (lastSampleTime < 0 || rx == TrafficStats.UNSUPPORTED.toLong()) {
            lastRxBytes = rx
            lastTxBytes = tx
            lastSampleTime = now
            return Pair(0, 0)
        }

        val elapsedSec = (now - lastSampleTime) / 1000.0
        val downKbps = if (elapsedSec > 0) ((rx - lastRxBytes) / 1024.0 / elapsedSec).toLong() else 0
        val upKbps = if (elapsedSec > 0) ((tx - lastTxBytes) / 1024.0 / elapsedSec).toLong() else 0

        lastRxBytes = rx
        lastTxBytes = tx
        lastSampleTime = now

        return Pair(maxOf(downKbps, 0), maxOf(upKbps, 0))
    }

    private fun formatUptime(ms: Long): String {
        val totalMin = ms / 60000
        val days = totalMin / 1440
        val hours = (totalMin % 1440) / 60
        val mins = totalMin % 60
        return when {
            days > 0 -> "${days}j ${hours}h${mins}m"
            hours > 0 -> "${hours}h${mins}m"
            else -> "${mins}m"
        }
    }
}
