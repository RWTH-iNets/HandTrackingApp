class LogSession:
    def __init__(self, session_id, description, start_time, sampling_behavior, sampling_interval):
        self.session_id = session_id
        self.description = description
        self.start_time = start_time
        self.sampling_behavior = sampling_behavior
        self.sampling_interval = sampling_interval
        self.events = []

    def get_nearest_event_index(self, session_time, type_filter=None):
        min_delta_t = None
        min_delta_t_index = None

        for idx, log_event in enumerate(self.events):
            if type_filter is None or isinstance(log_event, type_filter):
                delta_t = abs(log_event.session_time - session_time)

                if min_delta_t is None or delta_t < min_delta_t:
                    min_delta_t = delta_t
                    min_delta_t_index = idx
        
        return min_delta_t_index

    def get_previous_event_index(self, current_event_index, type_filter=None):
        for idx, log_event in reversed(list(enumerate(self.events[:current_event_index]))):
            if type_filter is None or isinstance(log_event, type_filter):
                return idx
        return None

    def get_next_event_index(self, current_event_index, type_filter=None):
        for idx, log_event in enumerate(self.events[current_event_index+1:]):
            if type_filter is None or isinstance(log_event, type_filter):
                return current_event_index + 1 + idx
        return None
