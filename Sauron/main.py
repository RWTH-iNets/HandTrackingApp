import sauron.io

#db = sauron.io.load('LoggingService.db')
db = sauron.io.load('LoggingService.json')
for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)

    print('Session', session.session_id, '--', session.description)
    for event in session.events:
        print(str(type(event)) + '@' + str(event.session_time))
