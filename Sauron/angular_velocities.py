import numpy as np
import matplotlib.pyplot as plt

import sauron.io
from sauron.logevent import RotationVectorEvent
from sauron.quaternion_math import compute_angular_velocities

db = sauron.io.load('LoggingService.db')
session = db.get_session(1)

timestamps = []
angular_velocity_x = []
angular_velocity_y = []
angular_velocity_z = []

idx = session.get_nearest_event_index(0, RotationVectorEvent)
while idx is not None:
    prev_idx = session.get_previous_event_index(idx, RotationVectorEvent)
    next_idx = session.get_next_event_index(idx, RotationVectorEvent)

    if idx is not None and prev_idx is not None and next_idx is not None:
        q = session.events[idx].quaternion
        dq_dt = (session.events[next_idx].quaternion - session.events[prev_idx].quaternion) / (session.events[next_idx].session_time - session.events[prev_idx].session_time)
        v = compute_angular_velocities(q, dq_dt)

        timestamps.append(session.events[idx].session_time)
        angular_velocity_x.append(v.x)
        angular_velocity_y.append(v.y)
        angular_velocity_z.append(v.z)
        

    idx = next_idx


plt.figure(1)

plt.subplot(3, 1, 1)
plt.plot(timestamps, angular_velocity_x)
plt.ylabel('X')

plt.subplot(3, 1, 2)
plt.plot(timestamps, angular_velocity_y)
plt.ylabel('Y')

plt.subplot(3, 1, 3)
plt.plot(timestamps, angular_velocity_z)
plt.ylabel('Z')

plt.show()