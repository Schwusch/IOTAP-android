import time
import sys
import pprint
import uuid
import ibmiotf.device
import ibmiof.application
import json

#Our device on IBM
organization = str("4quwpf")
deviceType = "Andriod"
deviceId = "testDatabase"
appId = deviceId + "_receiver"
authMethod = 'token'
authToken = str("Fjyob93*AD9-n4Ta7e")


#Prints out greeting "hello" if app and device has successfully been connected
def callBack(event):
	print ("Data has been sent from %s (%s) sent at %s: hello=%s x=%s" %(event.deviceId, event.deviceType, event.timestamp.strftime("%H:%M:%S"), data['hello'], data['x']))
	
#Tries to connect app to cloud
try: 
	appConnect = {
		"organization_Id": organization,
		"device_type": deviceType,
		"device_Id": deviceId,
		"app_Id": appId,
		"auth_Method": authMethod,
		"auth_Token": authToken
		}
	print device
	appClient = ibmiotf.application.Client(appConnect)
except Exception as e:
	print(str(e))
	sys.exit()
appClient.connect()
appClient.subscribeToDeviceEvents(deviceType, deviceId, "greeting")
appClient.deviceEventCallback = callBack

#Tries to connect device to cloud
try:
	deviceConnect = {
		"organization": organization,
		"deviceType": deviceType,
		"deviceID":deviceId,
		"appID": appId,
		"AuthMethod":authMethod,
		"AuthToken":authToken
		}
	deviceClient = ibmiotf.device.Client(deviceConnect)
except Exception as e:
	print("Tried to connect device: %s" % str(e))
	sys.exit()
#Connect and send a greeting 10 times as an event into the cloud		
deviceConnect.Connect()
for x in range(0,10):
	data = {'greeting': 'hello', 'x': x}
	def publishCallBack():
		print("Event %s is received" % x)
	
	success = deviceConnect.publishEvent("greeting", "json", data, qos=0, on_publish=publishCallBack)
	if not success:
		print("Device is not connected")
	time.sleep(1)
#Disconnect device and app from the cloud
deviceConnect.disconnect()
appConnect.disconnect()	
