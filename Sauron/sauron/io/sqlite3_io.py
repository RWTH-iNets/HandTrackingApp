import sqlite3

from ..logevent import LogStartedEvent, LogStoppedEvent, RotationVectorEvent, ScreenOnOffEvent, GameRotationVectorEvent, GyroscopeEvent, AccelerometerEvent, MagnetometerEvent, ProximitySensorEvent, LightSensorEvent, PressureSensorEvent, AmbientTemperatureSensorEvent, TrafficStatsEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent, PhoneCallEvent
from ..logsession import LogSession


class SQLiteDatabase:
    def __init__(self, filename):
        self.database = sqlite3.connect(filename)
        self.cursor = self.database.cursor()
        
    def get_all_session_ids(self):
        rows = self.cursor.execute('SELECT id FROM log_sessions')
        return [int(row[0]) for row in rows]

    @staticmethod
    def _logsession_from_db(session_id, description, start_time, sampling_behavior, sampling_interval):
        sampling_behaviors = {
            0: 'ALWAYS_ON',
            1: 'SCREEN_ON',
        }
        return LogSession(session_id, description, start_time, sampling_behaviors[sampling_behavior], sampling_interval / 1000)

    def get_session(self, session_id):
        rows = self.cursor.execute('SELECT description, start_time, sampling_behavior, sampling_interval FROM log_sessions WHERE id={}'.format(session_id))
        
        if rows:
            session = self._logsession_from_db(session_id, *rows.fetchone())
            session.events.events = self.get_all_events(session.session_id)

            # Validate session
            if not isinstance(session.events[0], LogStartedEvent) or not isinstance(session.events[-1], LogStoppedEvent):
                raise ValueError('First/last event in session must be LogStarted and LogStopped, respectively!')

            # Adjust session_time
            # TODO: This is a dirty hack to circumvent the mess with android event timestamps. Find a better solution for this!
            start_session_time_clock = session.events[0].session_time
            start_session_time_sensor = session.events[1].session_time
            for event in session.events:
                if type(event) in (LogStartedEvent, LogStoppedEvent, ScreenOnOffEvent, TrafficStatsEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent, PhoneCallEvent):
                    event.session_time = event.session_time - start_session_time_clock
                else:
                    event.session_time = event.session_time - start_session_time_sensor
        else:
            session = None

        return session

    @staticmethod
    def _logevent_from_db(event_type, session_time, data_int_0, data_float_0, data_float_1, data_float_2, data_float_3, data_string_0):
        session_time /= 1000000000

        handler_map = {
            0: lambda: LogStartedEvent(session_time),
            1: lambda: LogStoppedEvent(session_time),
            2: lambda: RotationVectorEvent(session_time, data_float_0, data_float_1, data_float_2, data_float_3),
            3: lambda: ScreenOnOffEvent(session_time, data_int_0 == 1),
            4: lambda: GameRotationVectorEvent(session_time, data_float_0, data_float_1, data_float_2, data_float_3),
            5: lambda: GyroscopeEvent(session_time, data_float_0, data_float_1, data_float_2),
            6: lambda: AccelerometerEvent(session_time, data_float_0, data_float_1, data_float_2),
            7: lambda: MagnetometerEvent(session_time, data_float_0, data_float_1, data_float_2),
            8: lambda: ProximitySensorEvent(session_time, data_float_0 / 100), # distance stored as cm
            9: lambda: LightSensorEvent(session_time, data_float_0),
            10: lambda: PressureSensorEvent(session_time, data_float_0 * 100), # pressure stored as hPa
            11: lambda: AmbientTemperatureSensorEvent(session_time, data_float_0),
            12: lambda: TrafficStatsEvent(session_time, data_float_0, data_float_1, data_float_2, data_float_3),
            13: lambda: ForegroundApplicationEvent(session_time, data_string_0),
            14: lambda: PowerConnectedEvent(session_time, data_int_0 == 1),
            15: lambda: DaydreamActiveEvent(session_time, data_int_0 == 1),
            16: lambda: PhoneCallEvent(session_time, ["INCOMING_CALL", "INCOMING_CALL_ATTENDED", "INCOMING_CALL_MISSED", "OUTGOING_CALL_PLACED", "CALL_ENDED"][data_int_0], data_string_0 if data_string_0 else None),
        }

        return handler_map[event_type]()

    def _get_all_events(self, session_id):
        rows = self.cursor.execute('SELECT type, session_time, data_int_0, data_float_0, data_float_1, data_float_2, data_float_3, data_string_0 FROM log_entries WHERE session_id={} ORDER BY session_time ASC'.format(session_id))
        
        return [self._logevent_from_db(*row) for row in rows]

