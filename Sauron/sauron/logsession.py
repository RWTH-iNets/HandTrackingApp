from .logeventlist import LogEventList

class LogSession:
    def __init__(self, session_id, description, start_time, sampling_behavior, sampling_interval):
        self.session_id = session_id
        self.description = description
        self.start_time = start_time
        self.sampling_behavior = sampling_behavior
        self.sampling_interval = sampling_interval
        self.events = LogEventList()
