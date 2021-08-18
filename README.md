# muZic
![ic_launcher-playstore](https://user-images.githubusercontent.com/47779194/129397976-154b9ac6-bd61-4610-ae0d-2c715d5ab250.png)

### ⚠️This repository is no longer up to date.<br/>
### 🧑‍🔧I am currently porting the existing code to Kotlin.<br/>
### 📱 You can check out the application on the Google Play store [HERE](https://play.google.com/store/apps/details?id=com.armaggheddon.muzic&gl=IT)<br/>
<br/>


The Android Developer site give a guide on how to set up the Android Auto DHU on 
the computer. However here I provide a quick summary with some troubleshooting i went into.
The link to the original documentation is here "https://developer.android.com/training/cars/testing"

In order to test the application in Android Auto UI there are 3 options:
	-Have a real car with support with Android Auto
	-Have a device with Android Auto application
	-Use the emulator in Android Studio and a compatible device

For the third step it is necessary to do:
	-Open the Android Auto application on the device, go to
	"Settings", scroll to "About and tap "Version" to display all the 
	version information. Then tap the "Version and permission info"
	10 times. Now developer options should be enabled. 

* Click on the overflow menu "Start head unit server" and make sure that 
  "Add new cars to Android Auto" is enabled in "Settings"->"Connected cars"

* Go in the directory install of Android Studio (if adb command is not in PATH), 
	for windows default install directory is in "C:\Users\<USER_NAME>\AppData\Local\Android\Sdk" 
	and navigate to "platform-tools" from there the "adb.exe" should be visible and can
	be launched by typing in the path box "cmd"
	
* To start the DHU on the computer navigate back from "platform-tools" and navigate to
	"extras" -> "google" -> "auto" and the file "desktop-head-unit.exe" should be there 
	if selected in the Android Studio install options.

Now just plug the phone to the computer and execute the "adb forward tcp:5277 tcp:5277", 
then launch "desktop-head-unit.exe". Now Android Auto should be running on the computer.

The application has been tested mainly on Android versions 9.0 (Galaxy S7) and 10.0 (Pixel 2 on the emulator). 

To import the songs in the Android emulator (device) it is just required 
to "drag and drop" the files on the emulator window and wait for the operation to complete. 
If the tracks are not immediately visible restarting the emulator 
(holding the power button and restarting) should fix the problem.
