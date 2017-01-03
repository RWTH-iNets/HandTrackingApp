import json

from ..logevent import LogStartedEvent, LogStoppedEvent, RotationVectorEvent, ScreenOnOffEvent
from ..logsession import LogSession


class JSONDatabase:
    def __init__(self, filename):
        with open(filename) as in_file:
            data = json.load(in_file)
            
            session_list = [self._logsession_from_json(session_data) for session_data in data]
            self.sessions = {session.session_id: session for session in session_list}
                            
    @staticmethod
    def _logsession_from_json(session_data):
        session = LogSession(int(session_data['id']), session_data['description'], session_data['start_time'], session_data['sampling_behavior'], session_data['sampling_interval'] / 1000)
        session.events = [JSONDatabase._logevent_from_json(event_data) for event_data in session_data['events']]
        return session

    @staticmethod
    def _logevent_from_json(event_data):
        session_time = event_data['session_time'] / 1000000000

        handler_map = {
            'LOG_STARTED': lambda: LogStartedEvent(session_time),
            'LOG_STOPPED': lambda: LogStoppedEvent(session_time),
            'ROTATION_VECTOR': lambda: RotationVectorEvent(session_time, *event_data['quaternion']),
            'SCREEN_ON_OFF': lambda: ScreenOnOffEvent(session_time, event_data['is_on']),
        }

        return handler_map[event_data['type']]()


    def get_all_session_ids(self):
        return [int(k) for k in self.sessions.keys()]

    def get_session(self, session_id):
        return self.sessions[session_id] if session_id in self.sessions else None

    def get_all_events(self, session_id):
        session = get_session(session_id)
        return session.events if session is not None else None
