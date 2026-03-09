package com.illa.cashvan.feature.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class HoneywellPrinterManager(private val context: Context) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PRINTER_NAME_PREFIX = "MPD31D"
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val hasScan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            hasConnect && hasScan
        } else {
            true
        }
    }

    suspend fun connect(deviceAddress: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (bluetoothAdapter == null) {
                return@withContext Result.failure(Exception("Bluetooth not supported on this device"))
            }

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth is not enabled. Please enable Bluetooth."))
            }

            if (!hasBluetoothPermission()) {
                return@withContext Result.failure(Exception("Bluetooth permission required. Please grant Bluetooth permission in app settings."))
            }

            val device = if (deviceAddress != null) {
                bluetoothAdapter.getRemoteDevice(deviceAddress)
            } else {
                findHoneywellPrinter()
            }

            if (device == null) {
                return@withContext Result.failure(Exception("Honeywell printer not found. Please pair the printer first."))
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
            }

            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            Result.success("Connected to ${device.name}")
        } catch (e: IOException) {
            disconnect()
            Result.failure(Exception("Failed to connect to printer: ${e.message}"))
        } catch (e: SecurityException) {
            Result.failure(Exception("Bluetooth permission required: ${e.message}"))
        } catch (e: Exception) {
            disconnect()
            Result.failure(Exception("Connection error: ${e.message}"))
        }
    }

    private fun findHoneywellPrinter(): BluetoothDevice? {
        return try {
            bluetoothAdapter?.bondedDevices?.find { device ->
                device.name?.contains(PRINTER_NAME_PREFIX, ignoreCase = true) == true
            }
        } catch (e: SecurityException) {
            null
        }
    }

    suspend fun printBytes(bytes: ByteArray, retryCount: Int = 0): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                return@withContext Result.failure(Exception("Printer not connected. Please connect first."))
            }

            outputStream?.write(bytes)
            outputStream?.flush()

            Result.success("Print job sent (${bytes.size} bytes)")
        } catch (e: IOException) {
            if (e.message?.contains("Broken pipe", ignoreCase = true) == true && retryCount < 1) {
                disconnect()

                val reconnectResult = connect()
                if (reconnectResult.isSuccess) {
                    return@withContext printBytes(bytes, retryCount + 1)
                } else {
                    return@withContext Result.failure(Exception("فشل إعادة الاتصال بالطابعة. يرجى إعادة المحاولة."))
                }
            }

            Result.failure(Exception("Print error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Print failed: ${e.message}"))
        }
    }

    suspend fun printInvoice(cpclBytes: ByteArray): Result<String> {
        val connectResult = if (!isConnected()) {
            connect()
        } else {
            Result.success("Already connected")
        }

        return if (connectResult.isSuccess) {
            printBytes(cpclBytes)
        } else {
            connectResult
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
        } catch (e: IOException) {
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true && outputStream != null
    }

    fun getPairedDevices(): List<Pair<String, String>> {
        return try {
            if (!hasBluetoothPermission()) {
                return emptyList()
            }
            bluetoothAdapter?.bondedDevices?.map { device ->
                Pair(device.name ?: "Unknown", device.address)
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun isBluetoothPermissionGranted(): Boolean = hasBluetoothPermission()

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}
