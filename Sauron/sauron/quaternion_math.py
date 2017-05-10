import numpy as np
import quaternion

from .vector3 import Vector3

def rotate(q, v):
    p = q.conjugate() * np.quaternion(0, *v) * q
    return Vector3(p.x, p.y, p.z)

def compute_angular_velocities(q, dq_dt):
    # Based on quaternion differentiation
    # q: Quaternion
    # omega: Angular Velocities
    # d/dt( q(t) ) = 0.5 * (i*omega_x(t) + j*omega_y(t) + k*omega_z(t)) * q(t)

    A = np.matrix([
            [-q.x, -q.y, -q.z],
            [ q.w,  q.z, -q.y],
            [-q.z,  q.w,  q.x],
            [ q.y, -q.x,  q.w]
        ])
    b = np.array([2*dq_dt.w, 2*dq_dt.x, 2*dq_dt.y, 2*dq_dt.z])

    x = np.linalg.lstsq(A, b)

    return Vector3(*x[0])
