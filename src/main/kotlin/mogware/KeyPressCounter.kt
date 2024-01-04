package mogware

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.lang.annotation.Native
import java.lang.reflect.TypeVariable
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.Popup
import kotlin.math.log
import kotlin.system.exitProcess



val startTime = LocalDateTime.now()
val countMap = ConcurrentHashMap<String, Int>()
var keyPresses = 0
var tray = false

var notificationService: TrayService? = null
private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())


// Configuration
const val logFrequency = 75
val logFileName = "KeystrokeCount_${startTime.dayOfMonth}-T-${startTime.hour}-${startTime.minute}-${startTime.second}.txt"
val logFileLocation = System.getProperty("user.home") + File.separator + "Desktop"




fun main() {

    // Save data before shutting down gracefully
    Runtime.getRuntime().addShutdownHook(Thread {
        saveToFile()
    })

    checkNotificationsAvailability() // Check if tray available

    notificationService = if (tray) TrayService() else null // Create tray service
    notificationService?.init() // Start tray service

    registerHook() // Register Native Hook
    registerListener() // Register key press listener and handler

}


fun registerListener() {
    val listener = object : NativeKeyListener {
        override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {

            coroutineScope.launch {
                nativeEvent.count()
            }
        }

    }
    GlobalScreen.addNativeKeyListener(listener)
}


// Handle a key press event
fun NativeKeyEvent?.count() {
    this ?: return
    val key = NativeKeyEvent.getKeyText(this.keyCode)
    countMap[key] = (countMap[key] ?: 0) + 1
    keyPresses++

    if (keyPresses % logFrequency == 0) saveToFile()
}


// Save data to file (Desktop)
fun saveToFile() {
    try {
        val desktopPath = logFileLocation
        val file = File(desktopPath, logFileName)

        PrintWriter(file).use { out ->
            out.println(dataToText())
        }

    } catch (e: Exception) {
        e.printStackTrace()
        notificationService?.notification("Failed to save key count data to a file!")
    }
}


// Serialize all data into log file format
fun dataToText(): String {
    val s = StringBuilder()
    s.append("Total key presses: $keyPresses").appendLine()

    val now = LocalDateTime.now()
    val difference = now.toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC)
    val minutes = Duration.ofSeconds(difference).toMinutes()
    val hours = Duration.ofSeconds(difference).toHours()

    s.append("Ran for ").append(minutes).append(" minutes (").append(hours).append("hr) - Started: ${startTime.year}-${startTime.month}-${startTime.dayOfMonth} ${startTime.hour}:${if (startTime.minute < 10) {"0"} else {""}}${startTime.minute}").appendLine().appendLine()

    countMap.entries.sortedByDescending { it.value }.forEach {
        s.append("'").append(it.key).append("' - ").append(it.value).appendLine()
    }

    return s.toString()
}


fun registerHook() {
    try {
        GlobalScreen.registerNativeHook()
    } catch (e: NativeHookException) {
        e.printStackTrace()
        println("\nFailed to register NativeHook.")
    }
}


// Check if tray is available
fun checkNotificationsAvailability() {
    tray = SystemTray.isSupported()
}


// Open current log file in default program
var logFileOpenedCounter = 0
fun openLogFile() {
    try {
        if (Desktop.isDesktopSupported()) {
            saveToFile()
            val desktop = Desktop.getDesktop()
            val file = File(logFileLocation + File.separator + logFileName)
            if (file.exists()) {
                desktop.open(file)
                logFileOpenedCounter = 0
            } else {
                if (logFileOpenedCounter > 5) {
                    notificationService?.notification("Failed to open log file after 10 tries.")
                    return
                }
                if (logFileOpenedCounter > 0) {
                    Thread.sleep(20)
                }
                logFileOpenedCounter++
                openLogFile()
                return
            }
        } else {
            notificationService?.notification("Unable to open log file! Open manually on desktop.")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        notificationService?.notification("There was an error opening the log file. Open manually on desktop.")
    }
}


// Tray service for icon and notifications
class TrayService() {

    private val systemTray = SystemTray.getSystemTray()
    private var initialized = false
    private var trayIcon: TrayIcon? = null

    fun init() {
        if (initialized) return

        val imageBytes = Base64.getDecoder().decode("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")
        val imageInputStream = ByteArrayInputStream(imageBytes)
        val image = ImageIO.read(imageInputStream)

        val popup = PopupMenu()
        val exitItem = MenuItem("Exit")
        val openLogItem = MenuItem("Open current log")

        exitItem.addActionListener {
            exitProcess(418)
        }

        openLogItem.addActionListener {
            openLogFile()
        }

        popup.add(openLogItem)
        popup.add(exitItem)

        trayIcon = TrayIcon(image, "Key press counter", popup)
        trayIcon?.isImageAutoSize = true
        trayIcon?.toolTip = "Key press counter"

        try {
            systemTray.add(trayIcon)
        } catch (e: AWTException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        initialized = true
        trayIcon?.displayMessage("Key press counter", "Running in the background.", TrayIcon.MessageType.INFO)
    }

    fun notification(message: String) {
        trayIcon?.displayMessage("Key press counter", message, TrayIcon.MessageType.INFO)
    }

}
