from ..logevent import is_sensor_event, LightSensorEvent

def fix_session_event_timestamps(session):
    start_session_time_clock = session.events[0].session_time
    for event in session.events:
        event.session_time = event.session_time - start_session_time_clock
    return


    # TODO: We no longer use SensorEvent.timestamp as session_time, so this function is no longer relevant
    # TODO: This is a dirty hack to circumvent the mess with android event timestamps. Find a better solution for this!
    start_session_time_clock = None
    start_session_time_sensor = None
    for event in session.events:
        if is_sensor_event(event):
            if start_session_time_sensor is None and not isinstance(event, LightSensorEvent): # Light sensor usually provides data that was logged before session was started, so ignore that
                start_session_time_sensor = event.session_time
        else:
            if start_session_time_clock is None:
                start_session_time_clock = event.session_time

        if start_session_time_clock is not None and start_session_time_sensor is not None:
            break

    for event in session.events:
        event.session_time = event.session_time - (start_session_time_sensor if is_sensor_event(event) else start_session_time_clock)

