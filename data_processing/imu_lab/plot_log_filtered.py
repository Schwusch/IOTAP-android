import numpy as np
import matplotlib.pyplot as plt
data2 = np.genfromtxt('log_filtered.csv', delimiter=',', skip_header=1, dtype=None)
f, (ax, ay, az, gx, gy, gz) = plt.subplots(6, sharex=True, sharey=False)

ax.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 1], 'g.-')
ax.grid(True)
ax.set_title('Acceleration X')

ay.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 2], 'g.-')
ay.grid(True)
ay.set_title('Acceleration Y')

az.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 3], 'g.-')
az.grid(True)
az.set_title('Acceleration Z')

gx.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 4], 'g.-')
gx.grid(True)
gx.set_title('Gyro X')

gy.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 5], 'g.-')
gy.grid(True)
gy.set_title('Gyro Y')

gz.plot(data2[0:len(data2[:, 0]) - 1, 0], data2[0:len(data2[:, 0]) - 1, 6], 'g.-')
gz.grid(True)
gz.set_title('Gyro Z')
plt.show()
