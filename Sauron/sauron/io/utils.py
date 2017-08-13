from ..logevent import LogStartedEvent, LogStoppedEvent, ScreenOnOffEvent, TrafficStatsEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent, PhoneCallEvent, SMSReceivedEvent

def fix_session_event_timestamps(session):
    # TODO: This is a dirty hack to circumvent the mess with android event timestamps. Find a better solution for this!
    start_session_time_clock = session.events[0].session_time
    start_session_time_sensor = session.events[1].session_time
    for event in session.events:
        if type(event) in (LogStartedEvent, LogStoppedEvent, ScreenOnOffEvent, TrafficStatsEvent, ForegroundApplicationEvent, PowerConnectedEvent, DaydreamActiveEvent, PhoneCallEvent, SMSReceivedEvent):
            event.session_time = event.session_time - start_session_time_clock
        else:
            event.session_time = event.session_time - start_session_time_sensor
        #event.session_time = event.session_time - start_session_time_clock

