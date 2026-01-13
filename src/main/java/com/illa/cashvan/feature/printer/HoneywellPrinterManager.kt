package com.illa.cashvan.feature.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Manager class for handling Honeywell MPD31D printer communication via Bluetooth
 */
class HoneywellPrinterManager(private val context: Context) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    companion object {
        // Standard UUID for Serial Port Profile (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PRINTER_NAME_PREFIX = "MPD31D" // Honeywell MPD31D prefix
    }

    /**
     * Connect to the Honeywell printer via Bluetooth
     * @param deviceAddress Bluetooth MAC address of the printer (optional, will auto-discover if null)
     * @return Result indicating success or failure with message
     */
    suspend fun connect(deviceAddress: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PrinterManager", "connect() called with address: $deviceAddress")

            if (bluetoothAdapter == null) {
                return@withContext Result.failure(Exception("Bluetooth not supported on this device"))
            }

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth is not enabled. Please enable Bluetooth."))
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
            bluetoothAdapter.cancelDiscovery()

            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            Log.d("PrinterManager", "Successfully connected to printer: ${device.name}")
            Timber.d("Successfully connected to printer: ${device.name}")
            Result.success("Connected to ${device.name}")
        } catch (e: IOException) {
            Log.e("PrinterManager", "IOException connecting to printer: ${e.message}", e)
            Timber.e(e, "Error connecting to printer")
            disconnect()
            Result.failure(Exception("Failed to connect to printer: ${e.message}"))
        } catch (e: SecurityException) {
            Log.e("PrinterManager", "SecurityException: ${e.message}", e)
            Timber.e(e, "Bluetooth permission denied")
            Result.failure(Exception("Bluetooth permission required: ${e.message}"))
        } catch (e: Exception) {
            Log.e("PrinterManager", "Exception connecting to printer: ${e.message}", e)
            Timber.e(e, "Unexpected error connecting to printer")
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
            Timber.e(e, "Bluetooth permission denied while searching for printer")
            null
        }
    }

    /**
     * Send raw bytes to the printer (recommended for CPCL commands)
     */
    suspend fun printBytes(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                return@withContext Result.failure(Exception("Printer not connected. Please connect first."))
            }

            outputStream?.write(bytes)
            outputStream?.flush()

            Log.d("PrinterManager", "Raw bytes sent to printer (${bytes.size} bytes)")
            Timber.d("Raw bytes print job sent successfully")
            Result.success("Print job sent (${bytes.size} bytes)")
        } catch (e: IOException) {
            Log.e("PrinterManager", "IOException during printBytes: ${e.message}", e)
            Timber.e(e, "Error printing bytes")
            Result.failure(Exception("Print error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("PrinterManager", "Exception during printBytes: ${e.message}", e)
            Timber.e(e, "Unexpected error during printing bytes")
            Result.failure(Exception("Print failed: ${e.message}"))
        }
    }

    /**
     * Legacy method - Print plain text (UTF-8 encoded)
     * → Use only for simple text, not recommended for CPCL + Arabic
     */
    @Deprecated("Use printBytes(ByteArray) for CPCL commands and proper Arabic support")
    suspend fun print(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                return@withContext Result.failure(Exception("Printer not connected"))
            }

            val bytes = text.toByteArray(Charsets.UTF_8)
            outputStream?.write(bytes)
            outputStream?.flush()

            // Extra feeds
            outputStream?.write("\n\n\n".toByteArray(Charsets.UTF_8))
            outputStream?.flush()

            Result.success("Text print completed")
        } catch (e: Exception) {
            Result.failure(Exception("Text print failed: ${e.message}"))
        }
    }

    /**
     * Main method to print formatted CPCL invoice
     * Now accepts raw ByteArray (recommended way)
     */
    suspend fun printInvoice(cpclBytes: ByteArray): Result<String> {
        Log.d("PrinterManager", "printInvoice(ByteArray) called - ${cpclBytes.size} bytes")

        val connectResult = if (!isConnected()) {
            Log.d("PrinterManager", "Not connected, attempting to connect...")
            connect()
        } else {
            Result.success("Already connected")
        }

        return if (connectResult.isSuccess) {
            printBytes(cpclBytes)
        } else {
            Log.e("PrinterManager", "Connection failed before printing invoice")
            connectResult
        }
    }

    /**
     * Legacy method - only for backward compatibility
     * Will convert String → ByteArray internally (not recommended for CPCL)
     */
    @Deprecated("Use printInvoice(ByteArray) instead")
    suspend fun printInvoice(invoiceContent: String): Result<String> {
        Log.w("PrinterManager", "Deprecated: printInvoice(String) called - converting to bytes")
        val bytes = invoiceContent.toByteArray(Charsets.UTF_8)
        return printInvoice(bytes)
    }

    /**
     * Disconnect from the printer
     */
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
            Timber.d("Disconnected from printer")
            Log.d("PrinterManager", "Printer disconnected")
        } catch (e: IOException) {
            Timber.e(e, "Error disconnecting from printer")
            Log.e("PrinterManager", "Error during disconnect: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    fun getPairedDevices(): List<Pair<String, String>> {
        return try {
            bluetoothAdapter?.bondedDevices?.map { device ->
                Pair(device.name ?: "Unknown", device.address)
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.e(e, "Bluetooth permission denied")
            emptyList()
        }
    }
}