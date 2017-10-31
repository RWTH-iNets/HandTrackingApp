import os
import os.path
import numpy as np
import pandas as pd

import cherrypy
from cherrypy.lib import static

from sauron.io import load, loads
from sauron.classification.feature_extraction import load_features, extract_features_from_windows, ALL_FEATURES
from sauron.classification.classification import Classifier
from sauron.classification.utils import is_active_usage, filter_windows

localDir = os.path.dirname(__file__)
absDir = os.path.join(os.getcwd(), localDir)

#######################################################################
#feature_config = ALL_FEATURES

feature_config = {
    'accelerometer': {
        'methods': {'axes'},
        'features': {'mean', 'stddev', 'median', 'rms'},
    },
    'gyroscope': {
        'methods': {'axes'},
        'features': {'mean', 'stddev', 'median', 'rms'},
    },
}

#######################################################################
current_class = None
active_usage = None
foreground_app = None

class Status:
    exposed = True
    
    def GET(self):
        global current_class
        global active_usage
        global foreground_app

        nice_names = {
            None: '[no data]',
            'standing_lefthand': 'held in the left hand (standing still)',
            'standing_righthand': 'held in the right hand (standing still)',
            'standing_bothhandslandscape': 'held in both hands (landscape mode; standing still)',
            'standing_leftpocket': 'stored in the left pocket (standing still)',
            'standing_rightpocket': 'stored in the right pocket (standing still)',
            'standing_leftear': 'held at the left ear (standing still)',
            'standing_rightear': 'held at the right ear (standing still)',
            'walking_lefthand': 'held in the left hand (walking)',
            'walking_righthand': 'held in the right hand (walking)',
            'walking_bothhandslandscape': 'held in both hands (landscape mode; walking)',
            'walking_leftpocket': 'stored in the left pocket (walking)',
            'walking_rightpocket': 'stored in the right pocket (walking)',
            'walking_leftear': 'held at the left ear (walking)',
            'walking_rightear': 'held at the right ear (walking)',
        }
        
        return """
            <html>
                <head>
                    <title>Live Demo</title>
                </head>
                <body OnLoad="setTimeout(function() { location.reload(); }, 100)">
                    <center>
                    <font size="100">
                        The device is currently... <br/>
                        %s <br/><br/>
                        The foreground app is %s
                    </font>
                    </center>
                </body>
            <html>
        """ % (nice_names[current_class], foreground_app)
      
      
class UploadHandler:
    exposed = True
    
    def __init__(self, classifier):
        self.clf = classifier

    def POST(self):
        cl = cherrypy.request.headers['Content-Length']
        rawbody = cherrypy.request.body.read(int(cl))
        
        db = loads(rawbody, 'json')
        for session_id in db.get_all_session_ids():
            session = db.get_session(session_id)
            #print('Analyzing session:', session.description)
            #print('  - Duration:', session.events[-1].session_time - session.events[0].session_time, 'seconds')
            #print('  -', len(session.events), 'logged events')

            # Load & filter windows
            windows = list(session.events.sliding_window(0.5, 0.5))
            #print('  -', len(windows), 'windows in total')
            #windows = list(filter_windows(windows, active_usage=True))
            #windows = list(filter_windows(windows, active_usage=True, foreground_app={"com.google.android.youtube", "de.rwth_aachen.inets.gollum"}))
            #print('  -', len(windows), 'windows after filtering')

            # Extract features
            features = extract_features_from_windows(windows, feature_config=feature_config)

            # Classify windows
            predicted_class_names = clf.classify(features)

            # Output results                
            #for window, predicted_class_name in zip(windows, predicted_class_names):
            #    print('{:5.2f}s - {:5.2f}s:'.format(window['start'], window['end']),
            #          predicted_class_name,
            #          '/ {} usage'.format('active' if is_active_usage(window) else 'inactive'),
            #          '/ foreground app:', window['foreground_app'])
                      
            if len(windows) > 0 and not predicted_class_names.empty:
                global current_class
                global active_usage
                global foreground_app
                
                current_class = predicted_class_names[0]
                active_usage = is_active_usage(windows[0])
                foreground_app = windows[0]['foreground_app']
                
                #print(current_class)

        return "OK"       

if __name__ == '__main__':
    # Load features and train classifier
    print('Loading features...')
    training_data = load_features('../Data/training/features.csv')
    
    print('Training classifier...')
    clf = Classifier(clf_type='KNN', feature_config=feature_config)
    clf.train(training_data)
    
    print('Starting webserver...')
    # CherryPy always starts with app.root when trying to map request URIs
    # to objects, so we need to mount a request handler root. A request
    # to '/' will be mapped to HelloWorld().index().
    conf = {
        '/': {
            'request.dispatch': cherrypy.dispatch.MethodDispatcher(),
        },
        '/upload': {
            'tools.response_headers.on': True,
            'tools.response_headers.headers': [('Content-Type', 'text/plain')],
        },
        '/status': {
        
        }
    }
    
    app = Status()
    app.upload = UploadHandler(clf)
    app.status = Status()
    cherrypy.server.socket_host = '0.0.0.0'
    cherrypy.quickstart(app, '/', conf)
