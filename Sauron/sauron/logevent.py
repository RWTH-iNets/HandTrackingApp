from math3d import UnitQuaternion


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
        self.quaternion = UnitQuaternion(x, y, z, w)


class ScreenOnOffEvent(LogEventBase):
    def __init__(self, session_time, is_on):
        super().__init__(session_time)
        self.is_on = is_on
