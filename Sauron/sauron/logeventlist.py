from .logevent import ScreenOnOffEvent, DeviceLockedEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent

class LogEventList:
    def __init__(self, events=None):
        self.events = events if events is not None else []

    def __iter__(self):
        return self.events.__iter__()

    def __getitem__(self, key):
        return self.events.__getitem__(key)

    def __len__(self):
        return self.events.__len__()

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
        def _build_window(cur_start, cur_end, cur_events, cur_screen_on, cur_device_locked, cur_foreground_app, cur_power_connected, cur_daydream_active):
            return {
                'events': cur_events,
                'screen_on': cur_screen_on,
                'device_locked': cur_device_locked,
                'foreground_app': cur_foreground_app,
                'power_connected': cur_power_connected,
                'daydream_active': cur_daydream_active,
            }

        cur_start = 0
        cur_end = cur_start + width
        cur_events = []
        cur_screen_on = True
        cur_device_locked = False
        cur_foreground_app = None
        cur_power_connected = False
        cur_daydream_active = False

        overlap = width * overlap_percentage

        for log_event in self.events:
            if isinstance(log_event, ScreenOnOffEvent):
                cur_screen_on = log_event.is_on
            elif isinstance(log_event, DeviceLockedEvent):
                cur_device_locked = log_event.is_locked
            elif isinstance(log_event, ForegroundApplicationEvent):
                cur_foreground_app = log_event.package_name
            elif isinstance(log_event, PowerConnectedEvent):
                cur_power_connected = log_event.is_connected
            elif isinstance(log_event, DaydreamActiveEvent):
                cur_daydream_active = log_event.is_active

            if type_filter is None or isinstance(log_event, type_filter):
                if log_event.session_time >= cur_start and log_event.session_time < cur_end:
                    cur_events.append(log_event)
                else:
                    if cur_events:
                        yield _build_window(cur_start, cur_end, cur_events, cur_screen_on, cur_device_locked, cur_foreground_app, cur_power_connected, cur_daydream_active)

                    cur_start = cur_end - overlap
                    cur_end = cur_start + width
                    cur_events = [x for x in cur_events if x.session_time >= cur_start and x.session_time < cur_end]

        if cur_events:
            yield _build_window(cur_start, cur_end, cur_events, cur_screen_on, cur_device_locked, cur_foreground_app, cur_power_connected, cur_daydream_active)

