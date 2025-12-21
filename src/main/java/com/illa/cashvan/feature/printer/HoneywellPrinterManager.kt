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
                Log.e("PrinterManager", "Bluetooth not supported")
                return@withContext Result.failure(Exception("Bluetooth not supported on this device"))
            }

            if (!bluetoothAdapter.isEnabled) {
                Log.e("PrinterManager", "Bluetooth not enabled")
                return@withContext Result.failure(Exception("Bluetooth is not enabled. Please enable Bluetooth."))
            }

            Log.d("PrinterManager", "Finding printer device...")
            // Find the printer device
            val device = if (deviceAddress != null) {
                bluetoothAdapter.getRemoteDevice(deviceAddress)
            } else {
                findHoneywellPrinter()
            }

            if (device == null) {
                Log.e("PrinterManager", "Printer device not found")
                return@withContext Result.failure(Exception("Honeywell printer not found. Please pair the printer first."))
            }

            Log.d("PrinterManager", "Found device: ${device.name}, creating socket...")
            // Create a Bluetooth socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // Cancel discovery to improve connection speed
            bluetoothAdapter.cancelDiscovery()

            Log.d("PrinterManager", "Connecting to device...")
            // Connect to the device
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

    /**
     * Find paired Honeywell printer
     * @return BluetoothDevice of the Honeywell printer or null if not found
     */
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
     * Print text content to the Honeywell printer
     * @param content Text content to print
     * @return Result indicating success or failure
     */
    suspend fun print(content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PrinterManager", "print() called with content length: ${content.length}")

            if (outputStream == null) {
                Log.e("PrinterManager", "outputStream is null, printer not connected")
                return@withContext Result.failure(Exception("Printer not connected. Please connect first."))
            }

            Log.d("PrinterManager", "Sending content to printer...")
            // Send the content to printer
            outputStream?.write(content.toByteArray(Charsets.UTF_8))
            outputStream?.flush()

            Log.d("PrinterManager", "Content sent, adding line feeds...")
            // Add line feeds to ensure content is printed
            outputStream?.write("\n\n\n".toByteArray(Charsets.UTF_8))
            outputStream?.flush()

            Log.d("PrinterManager", "Print job sent successfully")
            Timber.d("Print job sent successfully")
            Result.success("Print completed")
        } catch (e: IOException) {
            Log.e("PrinterManager", "IOException during print: ${e.message}", e)
            Timber.e(e, "Error printing")
            Result.failure(Exception("Print error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("PrinterManager", "Exception during print: ${e.message}", e)
            Timber.e(e, "Unexpected error during printing")
            Result.failure(Exception("Print failed: ${e.message}"))
        }
    }

    /**
     * Print byte array content to the Honeywell printer (for ESC/POS commands)
     * @param content Byte array content to print
     * @return Result indicating success or failure
     */
    suspend fun printBytes(content: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (outputStream == null) {
                return@withContext Result.failure(Exception("Printer not connected. Please connect first."))
            }

            // Send the byte content to printer
            outputStream?.write(content)
            outputStream?.flush()

            Timber.d("ESC/POS print job sent successfully")
            Result.success("Print completed")
        } catch (e: IOException) {
            Timber.e(e, "Error printing")
            Result.failure(Exception("Print error: ${e.message}"))
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during printing")
            Result.failure(Exception("Print failed: ${e.message}"))
        }
    }

    /**
     * Print invoice content
     * @param invoiceContent Formatted invoice text
     * @return Result indicating success or failure
     */
    suspend fun printInvoice(invoiceContent: String): Result<String> {
        Log.d("PrinterManager", "printInvoice() called")
        Log.d("PrinterManager", "Checking connection: socket=${bluetoothSocket != null}, connected=${bluetoothSocket?.isConnected}")

        val connectResult = if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            Log.d("PrinterManager", "Not connected, attempting to connect...")
            connect()
        } else {
            Log.d("PrinterManager", "Already connected")
            Result.success("Already connected")
        }

        Log.d("PrinterManager", "Connection result: ${connectResult.isSuccess}")

        return if (connectResult.isSuccess) {
            Log.d("PrinterManager", "Connection successful, calling print()")
            print(invoiceContent)
        } else {
            Log.e("PrinterManager", "Connection failed: ${connectResult.exceptionOrNull()?.message}")
            connectResult
        }
    }

    /**
     * Print invoice with ESC/POS commands
     * @param invoiceBytes ESC/POS formatted invoice bytes
     * @return Result indicating success or failure
     */
    suspend fun printInvoiceEscPos(invoiceBytes: ByteArray): Result<String> {
        val connectResult = if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            connect()
        } else {
            Result.success("Already connected")
        }

        return if (connectResult.isSuccess) {
            printBytes(invoiceBytes)
        } else {
            connectResult
        }
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
        } catch (e: IOException) {
            Timber.e(e, "Error disconnecting from printer")
        }
    }

    /**
     * Check if printer is connected
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Get list of paired Bluetooth devices
     * @return List of device names and addresses
     */
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
