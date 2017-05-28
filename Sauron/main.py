import sauron.io

#db = sauron.io.load('LoggingService.db')
db = sauron.io.load('rotate_phone.json')
for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)

    print('Session', session.session_id, '--', session.description, '--', session.sampling_behavior, '@', str(session.sampling_interval) + 's')
    for event in session.events:
        print(str(type(event)) + '@' + str(event.session_time))

    for window in session.events.sliding_window(1, 0.5):
        print('----------------------------------------------')
        for event in window:
            print(str(type(event)) + '@' + str(event.session_time))
