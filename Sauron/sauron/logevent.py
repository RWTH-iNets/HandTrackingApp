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


class MagnetometerEvent(LogEventBase):
    def __init__(self, session_time, x, y, z):
        super().__init__(session_time)
        self.vector = Vector3(x, y, z)


class ProximitySensorEvent(LogEventBase):
    def __init__(self, session_time, distance):
        super().__init__(session_time)
        self.distance = distance;


class LightSensorEvent(LogEventBase):
    def __init__(self, session_time, brightness):
        super().__init__(session_time)
        self.brightness = brightness;


class PressureSensorEvent(LogEventBase):
    def __init__(self, session_time, pressure):
        super().__init__(session_time)
        self.pressure = pressure;


class AmbientTemperatureSensorEvent(LogEventBase):
    def __init__(self, session_time, temperature):
        super().__init__(session_time)
        self.temperature = temperature;


class TrafficStatsEvent(LogEventBase):
    def __init__(self, session_time, mobile_rx_bytes, mobile_tx_bytes, total_rx_bytes, total_tx_bytes):
        super().__init__(session_time)
        self.mobile_rx_bytes = mobile_rx_bytes;
        self.mobile_tx_bytes = mobile_tx_bytes;
        self.total_rx_bytes = total_rx_bytes;
        self.total_tx_bytes = total_tx_bytes;


class ForegroundApplicationEvent(LogEventBase):
    def __init__(self, session_time, package_name):
        super().__init__(session_time)
        self.package_name = package_name


class PowerConnectedEvent(LogEventBase):
    def __init__(self, session_time, is_connected):
        super().__init__(session_time)
        self.is_connected = is_connected


class DaydreamActiveEvent(LogEventBase):
    def __init__(self, session_time, is_active):
        super().__init__(session_time)
        self.is_connected = is_active


class PhoneCallEvent(LogEventBase):
    def __init__(self, session_time, state, number):
        super().__init__(session_time)
        self.state = state
        self.number = number

