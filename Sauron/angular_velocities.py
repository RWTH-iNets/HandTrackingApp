import numpy as np
import matplotlib.pyplot as plt

import sauron.io
from sauron.logevent import RotationVectorEvent
from sauron.quaternion_math import compute_angular_velocities

db = sauron.io.load('rotate_phone.json')

def moving_average(a, n):
    ret = np.cumsum(a, dtype=float)
    ret[n:] = ret[n:] - ret[:-n]
    return ret[n - 1:] / n

for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)

    timestamps = []
    angular_velocity_x = []
    angular_velocity_y = []
    angular_velocity_z = []

    idx = session.events.get_nearest_index(0, RotationVectorEvent)
    while idx is not None:
        prev_idx = session.events.get_previous_index(idx, RotationVectorEvent)
        next_idx = session.events.get_next_index(idx, RotationVectorEvent)

        if idx is not None and prev_idx is not None and next_idx is not None:
            q = session.events[idx].quaternion
            dq_dt = (session.events[next_idx].quaternion - session.events[prev_idx].quaternion) / (session.events[next_idx].session_time - session.events[prev_idx].session_time)
            v = compute_angular_velocities(q, dq_dt)

            timestamps.append(session.events[idx].session_time)
            angular_velocity_x.append(v.x)
            angular_velocity_y.append(v.y)
            angular_velocity_z.append(v.z)
        

        idx = next_idx


    fig = plt.figure()
    fig.suptitle(session.description)

    plt.subplot(3, 1, 1)
    plt.ylim([-5, 5])
    plt.plot(timestamps[29:], moving_average(angular_velocity_y, 30))
    plt.ylabel('X [rad/s]')

    plt.subplot(3, 1, 2)
    plt.ylim([-5, 5])
    plt.plot(timestamps[29:], moving_average(angular_velocity_x, 30))
    plt.ylabel('Y [rad/s]')

    plt.subplot(3, 1, 3)
    plt.ylim([-5, 5])
    plt.plot(timestamps[29:], moving_average(angular_velocity_z, 30))
    plt.ylabel('Z [rad/s]')

plt.show()