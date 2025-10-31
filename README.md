# Robot LCD - Android Bluetooth Data Monitor

An Android application for receiving and displaying energy consumption data from an HC-05 Bluetooth module connected to a robot or IoT device. The app includes optional integration with the Hedera blockchain for data logging.

## Features

- **Bluetooth Communication**: Connect to HC-05 Bluetooth modules
- **Real-time Data Display**: View energy consumption data in real-time
- **Data History**: Maintains the last 100 consumption records
- **Blockchain Ready**: Prepared for Hedera Hashgraph integration
- **French UI**: User interface in French

## Prerequisites

- Android device with Bluetooth capability
- Android API Level 21+ (Android 5.0 Lollipop or higher)
- HC-05 Bluetooth module paired with your device
- The HC-05 should be named "Equipe Robotech 2"

## Permissions

The app requires the following permissions:

### Android 12+ (API 31+)
- `BLUETOOTH_CONNECT`
- `BLUETOOTH_SCAN`

### Android 11 and below
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `ACCESS_FINE_LOCATION`

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Build and run on your Android device
4. Grant Bluetooth permissions when prompted

## Setup

### 1. Pair HC-05 Module
Before using the app:
1. Go to your device's Bluetooth settings
2. Pair with your HC-05 module
3. Ensure the device name contains "Equipe Robotech 2"

### 2. Data Format
The app expects data in the following format from the HC-05:
```
154358 kWh\n
```
Each reading should end with a newline character (`\n`).

### 3. Hedera Configuration (Optional)
To enable blockchain logging:

1. Add the Hedera SDK to `build.gradle`:
```gradle
implementation 'com.hedera.hashgraph:sdk:2.x.x'
```

2. Update credentials in `MainActivity.kt`:
```kotlin
private val hederaAccountId = "YOUR_ACCOUNT_ID"
private val hederaPrivateKey = "YOUR_PRIVATE_KEY"
private val hederaTopicId = "YOUR_TOPIC_ID"
```

3. Uncomment the Hedera implementation code in the `sendToHedera()` function

## Usage

1. **Launch the app**
2. **Tap "Connect"** to establish Bluetooth connection
3. **View real-time data**:
   - Status indicator shows connection state
   - LCD data displays raw received data
   - Consumption value shows parsed energy reading
   - Last update timestamp shows when data was received
   - Blockchain status indicates if data was logged

4. **Tap "Disconnect"** to close the connection

## UI Components

- **Status**: Connection state (Connected/Disconnected)
- **LCD Data**: Raw data from the device
- **Device ID**: Fixed identifier (#453687)
- **Consumption**: Parsed energy value in kWh
- **Last Update**: Timestamp of last received data
- **Blockchain Status**: Hedera submission status

## Data Structure

```kotlin
data class ConsumptionData(
    val deviceId: String,
    val consumption: String,
    val timestamp: Long,
    val blockchainTxId: String? = null
)
```

## Project Structure

```
com.example.robotlcd
├── MainActivity.kt          # Main activity with Bluetooth logic
├── res/
│   └── layout/
│       └── activity_main.xml   # UI layout
└── AndroidManifest.xml      # Permissions and app config
```

## Troubleshooting

### HC-05 Not Found
- Ensure the device is paired in system Bluetooth settings
- Verify the device name matches "Equipe Robotech 2"
- Check that Bluetooth is enabled on your phone

### Connection Fails
- Verify HC-05 is powered on
- Check that no other app is using the Bluetooth connection
- Try unpairing and re-pairing the device

### No Data Received
- Check HC-05 wiring and power supply
- Verify the Arduino/microcontroller is sending data
- Ensure data format matches expected format (value + " kWh")

### Permission Denied
- Go to Settings → Apps → Robot LCD → Permissions
- Grant all required Bluetooth permissions

## Technical Details

- **UUID**: Uses standard SPP UUID (00001101-0000-1000-8000-00805F9B34FB)
- **Connection**: RFCOMM socket connection
- **Threading**: Uses Kotlin Coroutines for async operations
- **Buffer**: 1024-byte read buffer with newline delimiter parsing

## Future Enhancements

- [ ] Multiple device support
- [ ] Data export (CSV, JSON)
- [ ] Historical data visualization
- [ ] Configurable device ID
- [ ] Automatic reconnection
- [ ] Full Hedera blockchain integration
- [ ] Notification support for abnormal readings

## License

This project is provided as-is for educational and development purposes.

## Support

For issues or questions, please check:
- HC-05 Bluetooth module documentation
- Android Bluetooth API documentation
- Hedera SDK documentation (for blockchain features)

## Authors

HC-05 

---

**Note**: This app is designed for monitoring energy consumption from IoT devices via Bluetooth. Always ensure proper security measures when transmitting sensitive data.