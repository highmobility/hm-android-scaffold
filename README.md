# Android Scaffold

This app provides a simple scaffold to start building an app with HMKit on Android. These things have been done:

1. New Gradle project created in Android Studio
1. SDK imported and added to dependencies
1. SDK initialised with snippet from the Developer Center
1. One example included to send a telematics command to the car
1. One example included to send a Bluetooth command to the car

# Configuration

Before running the app, make sure to configure the following in `MainActivity.java`:

1. Initialise HMKit with a valid `Device Certiticate` from the Developer Center https://developers.high-mobility.com/
2. Create an `Access Token` in the car emulator by following the steps at https://high-mobility.com/learn/tutorials/sdk/android/ and paste it in the source code to download an `Access Certificate` from the server.

# Run the app

In the Developer Center, start the linked vehicle's Emulator. Run the app on an Android Virtual Device to send a few commands to the car. The app will display "HMKit Scaffold" on a grey screen; responses to the telematics commands can be found in the logs in Android Studio.


Note that Bluetooth only works on a real device.

# Questions or Comments ?

If you have questions or if you would like to send us feedback, join our [Slack Channel](https://slack.high-mobility.com/) or email us at [support@high-mobility.com](mailto:support@high-mobility.com).
