class LogSession:
    def __init__(self, session_id, description, start_time):
        self.session_id = session_id
        self.description = description
        self.start_time = start_time
        self.events = []
