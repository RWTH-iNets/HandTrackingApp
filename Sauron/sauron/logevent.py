import numpy as np
import quaternion
from .vector3 import Vector3


class LogEventBase:
    def __init__(self, session_time):
        self.session_time = session_time


class LogStartedEvent(LogEventBase):
    def __init__(self, session_time):
        super().__init__(session_time)


class LogStoppedEvent(LogEventBase):
    def __init__(self, session_time):
        super().__init__(session_time)


class RotationVectorEvent(LogEventBase):
    def __init__(self, session_time, x, y, z, w):
        super().__init__(session_time)
        self.quaternion = np.quaternion(w, x, y, z)


class ScreenOnOffEvent(LogEventBase):
    def __init__(self, session_time, is_on):
        super().__init__(session_time)
        self.is_on = is_on
        

class GameRotationVectorEvent(LogEventBase):
    def __init__(self, session_time, x, y, z, w):
        super().__init__(session_time)
        self.quaternion = np.quaternion(w, x, y, z)


class GyroscopeEvent(LogEventBase):
    def __init__(self, session_time, x, y, z):
        super().__init__(session_time)
        self.vector = Vector3(x, y, z)


class AccelerometerEvent(LogEventBase):
    def __init__(self, session_time, x, y, z):
        super().__init__(session_time)
        self.vector = Vector3(x, y, z)
