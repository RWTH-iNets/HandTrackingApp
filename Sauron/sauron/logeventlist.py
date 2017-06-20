class LogEventList:
    def __init__(self, events=None):
        self.events = events if events is not None else []

    def __iter__(self):
        return self.events.__iter__()

    def __getitem__(self, key):
        return self.events.__getitem__(key)

    def get_nearest_index(self, session_time, type_filter=None):
        min_delta_t = None
        min_delta_t_index = None

        for idx, log_event in enumerate(self.events):
            if type_filter is None or isinstance(log_event, type_filter):
                delta_t = abs(log_event.session_time - session_time)

                if min_delta_t is None or delta_t < min_delta_t:
                    min_delta_t = delta_t
                    min_delta_t_index = idx
        
        return min_delta_t_index

    def get_previous_index(self, current_index, type_filter=None):
        for idx, log_event in reversed(list(enumerate(self.events[:current_index]))):
            if type_filter is None or isinstance(log_event, type_filter):
                return idx
        return None

    def get_next_index(self, current_index, type_filter=None):
        for idx, log_event in enumerate(self.events[current_index+1:]):
            if type_filter is None or isinstance(log_event, type_filter):
                return current_index + 1 + idx
        return None

    def sliding_window(self, width, overlap_percentage, type_filter=None):
        cur_start = 0
        cur_end = cur_start + width
        cur_events = []

        overlap = width * overlap_percentage

        for log_event in self.events:
            if type_filter is None or isinstance(log_event, type_filter):
                if log_event.session_time >= cur_start and log_event.session_time < cur_end:
                    cur_events.append(log_event)
                else:
                    if cur_events:
                        yield cur_events

                    cur_start = cur_end - overlap
                    cur_end = cur_start + width
                    cur_events = [x for x in cur_events if x.session_time >= cur_start and x.session_time < cur_end]

        if cur_events:
            yield cur_events
