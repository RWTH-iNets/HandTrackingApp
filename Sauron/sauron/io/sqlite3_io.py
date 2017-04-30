import sqlite3

from ..logevent import LogStartedEvent, LogStoppedEvent, RotationVectorEvent, ScreenOnOffEvent, GameRotationVectorEvent, GyroscopeEvent, AccelerometerEvent
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
            session.events = self.get_all_events(session.session_id)
        else:
            session = None

        return session

    @staticmethod
    def _logevent_from_db(event_type, session_time, data_int_0, data_float_0, data_float_1, data_float_2, data_float_3):
        session_time /= 1000000000

        handler_map = {
            0: lambda: LogStartedEvent(session_time),
            1: lambda: LogStoppedEvent(session_time),
            2: lambda: RotationVectorEvent(session_time, data_float_0, data_float_1, data_float_2, data_float_3),
            3: lambda: ScreenOnOffEvent(session_time, data_int_0 == 1),
            4: lambda: GameRotationVectorEvent(session_time, data_float_0, data_float_1, data_float_2, data_float_3),
            5: lambda: GyroscopeEvent(session_time, data_float_0, data_float_1, data_float_2),
            6: lambda: AccelerometerEvent(session_time, data_float_0, data_float_1, data_float_2),
        }

        return handler_map[event_type]()

    def get_all_events(self, session_id):
        rows = self.cursor.execute('SELECT type, session_time, data_int_0, data_float_0, data_float_1, data_float_2, data_float_3 FROM log_entries WHERE session_id={} ORDER BY session_time ASC'.format(session_id))
        
        return [self._logevent_from_db(*row) for row in rows]
