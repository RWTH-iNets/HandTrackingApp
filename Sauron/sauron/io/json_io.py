import json

from ..logevent import LogStartedEvent, LogStoppedEvent, RotationVectorEvent, ScreenOnOffEvent, GameRotationVectorEvent, GyroscopeEvent, AccelerometerEvent, MagnetometerEvent, ProximitySensorEvent, LightSensorEvent, PressureSensorEvent, AmbientTemperatureSensorEvent, TrafficStatsEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent, PhoneCallEvent, SMSReceivedEvent
from ..logsession import LogSession
from .utils import fix_session_event_timestamps


class JSONDatabase:
    def __init__(self, filename):
        with open(filename) as in_file:
            data = json.load(in_file)
            
            session_list = [self._logsession_from_json(session_data) for session_data in data]
            self.sessions = {session.session_id: session for session in session_list}
                            
    @staticmethod
    def _logsession_from_json(session_data):
        session = LogSession(int(session_data['id']), session_data['description'], session_data['start_time'], session_data['sampling_behavior'], session_data['sampling_interval'] / 1000)
        session.events.events = [JSONDatabase._logevent_from_json(event_data) for event_data in session_data['events']]

        # Validate session
        if not isinstance(session.events[0], LogStartedEvent) or not isinstance(session.events[-1], LogStoppedEvent):
            raise ValueError('First/last event in session must be LogStarted and LogStopped, respectively!')

        # Adjust session_time
        fix_session_event_timestamps(session)

        return session

    @staticmethod
    def _logevent_from_json(event_data):
        session_time = event_data['session_time'] / 1000000000

        handler_map = {
            'LOG_STARTED': lambda: LogStartedEvent(session_time),
            'LOG_STOPPED': lambda: LogStoppedEvent(session_time),
            'ROTATION_VECTOR': lambda: RotationVectorEvent(session_time, *event_data['quaternion']),
            'SCREEN_ON_OFF': lambda: ScreenOnOffEvent(session_time, event_data['is_on']),
            'GAME_ROTATION_VECTOR': lambda: GameRotationVectorEvent(session_time, *event_data['quaternion']),
            'GYROSCOPE': lambda: GyroscopeEvent(session_time, *event_data['vector']),
            'ACCELEROMETER': lambda: AccelerometerEvent(session_time, *event_data['vector']),
            'MAGNETOMETER': lambda: MagnetometerEvent(session_time, *event_data['vector']),
            'PROXIMITY': lambda: ProximitySensorEvent(session_time, event_data['value'] / 100), # distance stored as cm
            'LIGHT': lambda: LightSensorEvent(session_time, event_data['value']),
            'PRESSURE': lambda: PressureSensorEvent(session_time, event_data['value'] * 100), # pressure stored as hPa
            'AMBIENT_TEMPERATURE': lambda: AmbientTemperatureSensorEvent(session_time, event_data['value']),
            'TRAFFIC_STATS': lambda: TrafficStatsEvent(session_time, event_data['mobile_rx_bytes'], event_data['mobile_tx_bytes'], event_data['total_rx_bytes'], event_data['total_tx_bytes']),
            'FOREGROUND_APPLICATION': lambda: ForegroundApplicationEvent(session_time, event_data['package_name']),
            'POWER_CONNECTED': lambda: PowerConnectedEvent(session_time, event_data['is_connected']),
            'DAYDREAM_ACTIVE': lambda: DaydreamActiveEvent(session_time, event_data['is_active']),
            'PHONE_CALL': lambda: PhoneCallEvent(session_time, event_data['state'], event_data['number'] if 'number' in event_data else None),
            'SMS_RECEIVED': lambda: SMSReceivedEvent(session_time),
        }

        return handler_map[event_data['type']]()


    def get_all_session_ids(self):
        return [int(k) for k in self.sessions.keys()]

    def get_session(self, session_id):
        return self.sessions[session_id] if session_id in self.sessions else None

