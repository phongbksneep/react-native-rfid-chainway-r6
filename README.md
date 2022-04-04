# react-native-rfid-chainway-r6

handle rfid for module chainway r6

## Installation

```sh
npm install react-native-rfid-chainway-r6
```

## Usage

```js
import RfidChainwayR6 from "react-native-rfid-chainway-r6";
```
### Scan devices BLE ###

```javascript
  RfidChainwayR6.ScanBLE()
```

### Stop scan devices BLE ###

```javascript
  RfidChainwayR6.stopScanBLE()
```

### Connect devices BLE ###

```javascript
   RfidChainwayR6.connectAddress('address').then(res => {
     // res is divices infomation connected
    });
```

### Start read RFID ###

```javascript
  RfidChainwayR6.startScanRFID()
```

### Stop read RFID ###

```javascript
  RfidChainwayR6.stop()
```

### Clear RFID ###

```javascript
  RfidChainwayR6.clearData().then(res => {
    //res == true =>  done
    //res != true =>  fail
  })
```

## EVENT LISTENER ##

### Listen devices infomation BLE ##

```javascript
  DeviceEventEmitter.addListener('ScanBLEListenner', res => {
    // res is the information of a device when found
  })
```

### Listen RFID ##

```javascript
  DeviceEventEmitter.addListener('ReadRFIDListenner', res => {
    // res is the information of a RFID tag when found
  })
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
