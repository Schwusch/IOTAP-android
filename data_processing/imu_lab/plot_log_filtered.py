import numpy as np
import matplotlib.pyplot as plt
import argparse
import matplotlib.ticker as plticker

parser = argparse.ArgumentParser(description='Plot csv data from Processing.')
parser.add_argument("filename")
args = parser.parse_args()


loc = plticker.MultipleLocator(base=16)
data2 = np.genfromtxt(args.filename, delimiter=',', skip_header=1, dtype=None)
(stamp, xacc, yacc, zacc, xgyr, ygyr, zgyr) = zip(*data2)
f, (ax, ay, az, gx, gy, gz) = plt.subplots(6)

ax.plot(stamp, xacc, 'g.-', markevery=16)
ax.grid(True)
ax.set_title('Acceleration X')
ax.xaxis.set_major_locator(loc)

ay.plot(stamp, yacc, 'g.-', markevery=16)
ay.grid(True, markevery=16)
ay.set_title('Acceleration Y')
ay.xaxis.set_major_locator(loc)

az.plot(stamp, zacc, 'g.-', markevery=16)
az.grid(True)
az.set_title('Acceleration Z')
az.xaxis.set_major_locator(loc)

gx.plot(stamp, xgyr, 'g.-', markevery=16)
gx.grid(True)
gx.set_title('Gyro X')
gx.xaxis.set_major_locator(loc)

gy.plot(stamp, ygyr, 'g.-', markevery=16)
gy.grid(True)
gy.set_title('Gyro Y')
gy.xaxis.set_major_locator(loc)

gz.plot(stamp, zgyr, 'g.-', markevery=16)
gz.grid(True)
gz.set_title('Gyro Z')
gz.xaxis.set_major_locator(loc)

plt.show()
