import serial.tools.list_ports
import serial
import timeit

ports = list(serial.tools.list_ports.comports())
ser = None
for p in ports:
    print p.device
    if "rfcomm" in p[0] or "Arduino" in p[1]:
        try:
            ser = serial.Serial(p.device, 9600, timeout=10)
            print("Connected to " + p.device)
            break
        except serial.SerialException:
            print("Couldn't connect to " + p.device)

if ser is not None:
    ser.close()  # In case the port is already open this closes it.
    ser.open()  # Reopen the port.

    ser.flushInput()
    ser.flushOutput()

    str1 = ""
    str2 = ""
    count = 0
    start_time = timeit.default_timer()
    while True:
        if "\n" not in str1:  # concatenates string on one line till a line feed "\n"
            str2 = ser.readline()  # is found, then prints the line.
            str1 += str2
        str1 = ""
        count += 1
        if timeit.default_timer() - start_time > 1:
            print count
            count = 0
            start_time = timeit.default_timer()
else:
    print("No device found")
