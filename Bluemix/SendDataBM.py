import time
import sys
print sys.path
import pprint
import uuid
import ibmiotf
try:
	import ibmiotf.application
	import ibmiotf.device

except ImportError:
	# This part is only required to run the sample from within the samples
	# directory when the module itself is not installed.
	#
	# If you have the module installed, just use "import ibmiotf.application" & "import ibmiotf.device"
	import os
	import inspect
	cmd_subfolder = os.path.realpath(os.path.abspath(os.path.join(os.path.split(inspect.getfile( inspect.currentframe() ))[0],"../../src")))
	if cmd_subfolder not in sys.path:
		sys.path.insert(0, cmd_subfolder)
	import ibmiotf.application
	import ibmiotf.device


organization_Id = str("4quwpf")
device_Type = "Andriod"
device_Id = "testDatabase"
app_Id = deviceId + "_receiver"
auth_Method = None
auth_Token = str("Fjyob93*AD9-n4Ta7e")

def AppCallBack(event):
    print("Received live data from %s (%s) sent at %s: hello=%s x=%s" % (event.device_Id, event.device_Type, event.timestamp.strftime("%H:%M:%S"), data['hello'], data['x']))

# Initialize the application client.
try:
    appOptions = {
            "org": organization_Id,
            "id": app_Id,
            "auth-method": auth_Method,
            "auth-token": auth_Token
            }
    appCli = ibmiotf.application.Client(appOptions)
except Exception as e:
    print(str(e))
    sys.exit()

# Connect and configuration the application
# - subscribe to live data from the device we created, specifically to "greeting" events
# - use the myAppEventCallback method to process events
appCli.connect()
appCli.subscribeToDeviceEvents(deviceType, deviceId, "greeting")
appCli.deviceEventCallback = myAppEventCallback

# Initialize the device client.
try:
    deviceOptions = {
            "org": organization_Id,
            "type": device_Type,
            "id": device_Id,
            "auth-method": auth_Method,
            "auth-token": auth_Token
            }
    deviceCli = ibmiotf.device.Client(deviceOptions)
except Exception as e:
    print("Caught exception connecting device: %s" % str(e))
    sys.exit()

# Connect and send a datapoint "hello" with value "world" into the cloud as an event of type "greeting" 10 times
deviceCli.connect()
for x in range (0,10):
    data = { 'hello' : 'world', 'x' : x}
    
def myOnPublishCallback():
    print("Confirmed event %s received by IoTF\n" % x)
        
    success = deviceCli.publishEvent("greeting", "json", data, qos=0, on_publish=myOnPublishCallback)
    if not success:
        print("Not connected to IoTF")
        time.sleep(1)


# Disconnect the device and application from the cloud
deviceCli.disconnect()


