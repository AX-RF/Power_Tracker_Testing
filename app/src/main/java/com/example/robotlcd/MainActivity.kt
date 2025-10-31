package com.example.robotlcd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    // HC-05 UUID
    private val HC05_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // UI Elements
    private lateinit var statusText: TextView
    private lateinit var lcdDataText: TextView
    private lateinit var deviceIdText: TextView
    private lateinit var consumptionText: TextView
    private lateinit var lastUpdateText: TextView
    private lateinit var blockchainStatusText: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button

    // Data Storage
    private val dataHistory = mutableListOf<ConsumptionData>()

    // Hedera Configuration (optional)
    private val hederaAccountId = "0.0.7136804"
    private val hederaPrivateKey = "0xbf7d3780a04d3cad561e11479f468d36c6070641f5d11e0ee7de8eb35bade31b"
    private val hederaTopicId = "0xb1b522ad7838ad25e541d575645b221fafb7ec1d"

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    data class ConsumptionData(
        val deviceId: String,
        val consumption: String,
        val timestamp: Long,
        val blockchainTxId: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        initializeViews()

        // Initialize Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Check if Bluetooth is available
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible sur cet appareil",
                Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Request permissions
        requestBluetoothPermissions()

        // Button listeners
        connectButton.setOnClickListener { connectToHC05() }
        disconnectButton.setOnClickListener { disconnect() }

        updateUIStatus("Déconnecté", false)
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        lcdDataText = findViewById(R.id.lcdDataText)
        deviceIdText = findViewById(R.id.deviceIdText)
        consumptionText = findViewById(R.id.consumptionText)
        lastUpdateText = findViewById(R.id.lastUpdateText)
        blockchainStatusText = findViewById(R.id.blockchainStatusText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions accordées!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions Bluetooth requises pour l'application",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToHC05() {
        if (isConnected) {
            Toast.makeText(this, "Déjà connecté!", Toast.LENGTH_SHORT).show()
            return
        }

        // Check permissions first
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "Permission Bluetooth requise!", Toast.LENGTH_LONG).show()
            requestBluetoothPermissions()
            return
        }

        updateUIStatus("Connexion...", false)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Find paired HC-05
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                var hc05Device: BluetoothDevice? = null

                pairedDevices?.forEach { device ->
                    if (device.name?.contains("Equipe Robotech 2") == true ||
                        device.name?.contains("Equipe Robotech 2") == true) {
                        hc05Device = device
                    }
                }

                if (hc05Device == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity,
                            "HC-05 non trouvé! Vérifiez le couplage",
                            Toast.LENGTH_LONG).show()
                        updateUIStatus("HC-05 non trouvé", false)
                    }
                    return@launch
                }

                // Connect with permission check
                bluetoothSocket = hc05Device?.createRfcommSocketToServiceRecord(HC05_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                isConnected = true

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Connecté à ${hc05Device?.name}!",
                        Toast.LENGTH_SHORT).show()
                    updateUIStatus("Connecté", true)
                }

                // Start reading data
                startReadingData()

            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Permission Bluetooth refusée",
                        Toast.LENGTH_LONG).show()
                    updateUIStatus("Permission refusée", false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Erreur: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    updateUIStatus("Erreur de connexion", false)
                }
            }
        }
    }

    private fun startReadingData() {
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            var readBufferPosition = 0

            while (isConnected) {
                try {
                    val bytesAvailable = inputStream?.available() ?: 0

                    if (bytesAvailable > 0) {
                        val bytes = inputStream?.read(buffer)

                        if (bytes != null && bytes > 0) {
                            for (i in 0 until bytes) {
                                val b = buffer[i]

                                if (b.toInt() == 10) { // Newline character
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(buffer, 0, encodedBytes, 0, readBufferPosition)

                                    val data = String(encodedBytes, Charsets.UTF_8).trim()

                                    if (data.isNotEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            processReceivedData(data)
                                        }
                                    }

                                    readBufferPosition = 0
                                } else {
                                    buffer[readBufferPosition++] = b
                                }
                            }
                        }
                    }

                    delay(100)

                } catch (e: Exception) {
                    isConnected = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity,
                            "Déconnecté",
                            Toast.LENGTH_SHORT).show()
                        updateUIStatus("Déconnecté", false)
                    }
                    break
                }
            }
        }
    }

    private fun processReceivedData(data: String) {
        // Data format: "154358 kWh"
        lcdDataText.text = data

        // Parse consumption value
        val parts = data.split(" ")
        if (parts.isNotEmpty()) {
            val consumptionValue = parts[0]
            consumptionText.text = "$consumptionValue kWh"

            // Update timestamp
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date())
            lastUpdateText.text = "Dernière MAJ: $currentTime"

            // Create data object
            val consumptionData = ConsumptionData(
                deviceId = "ID:#453687",
                consumption = data,
                timestamp = System.currentTimeMillis()
            )

            // Add to history
            dataHistory.add(consumptionData)

            // Keep only last 100 records
            if (dataHistory.size > 100) {
                dataHistory.removeAt(0)
            }

            // Send to Hedera blockchain (optional)
            sendToHedera(consumptionData)
        }
    }


    private fun sendToHedera(data: ConsumptionData) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Hedera SDK implementation
                // Uncomment when you have configured Hedera credentials

//
//                val client = Client.forTestnet()
//                client.setOperator(
//                    AccountId.fromString(hederaAccountId),
//                    PrivateKey.fromString(hederaPrivateKey)
//                )
//
//                val message = """
//                    {
//                        "deviceId": "${data.deviceId}",
//                        "consumption": "${data.consumption}",
//                        "timestamp": ${data.timestamp}
//                    }
//                """.trimIndent()
//
//                val transaction = TopicMessageSubmitTransaction()
//                    .setTopicId(TopicId.fromString(hederaTopicId))
//                    .setMessage(message)
//
//                val receipt = transaction.execute(client).getReceipt(client)
//
//                withContext(Dispatchers.Main) {
//                    blockchainStatusText.text = "✓ Envoyé à Hedera"
//                }
//

                // Simulation for now
                withContext(Dispatchers.Main) {
                    blockchainStatusText.text = "✓ Prêt pour Hedera"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    blockchainStatusText.text = "⚠ Erreur blockchain"
                }
            }
        }
    }

    private fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

            updateUIStatus("Déconnecté", false)
            Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIStatus(status: String, connected: Boolean) {
        statusText.text = "Statut: $status"
        statusText.setTextColor(
            if (connected) ContextCompat.getColor(this, android.R.color.holo_green_light)
            else ContextCompat.getColor(this, android.R.color.holo_orange_light)
        )

        connectButton.isEnabled = !connected
        disconnectButton.isEnabled = connected

        if (!connected) {
            lcdDataText.text = "En attente de données..."
            consumptionText.text = "-- kWh"
        }

        deviceIdText.text = "ID:#453687"
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}