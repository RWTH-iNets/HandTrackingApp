import numpy as np
import pandas as pd

from sauron.io import load
from sauron.classification.feature_extraction import load_features, extract_features_from_windows, ALL_FEATURES
from sauron.classification.classification import Classifier
from sauron.classification.utils import is_active_usage, filter_windows

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
# Load features and train classifier
training_data = load_features('../Data/features.csv')
clf = Classifier(clf_type='KNN', feature_config=feature_config)
clf.train(training_data)

# Load session
db = load('../Data/test/session.json')
for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)
    print('Analyzing session:', session.description)
    print('  - Duration:', session.events[-1].session_time - session.events[0].session_time, 'seconds')
    print('  -', len(session.events), 'logged events')

    # Load & filter windows
    windows = list(session.events.sliding_window(1.5, 0.5))
    print('  -', len(windows), 'windows in total')
    #windows = list(filter_windows(windows, active_usage=True))
    #windows = list(filter_windows(windows, active_usage=True, foreground_app={"com.google.android.youtube", "de.rwth_aachen.inets.gollum"}))
    print('  -', len(windows), 'windows after filtering')

    # Extract features
    features = extract_features_from_windows(windows, feature_config=feature_config)

    # Classify windows
    predicted_class_names = clf.classify(features)

    # Output results
    for window, predicted_class_name in zip(windows, predicted_class_names):
        print('{:5.2f}s - {:5.2f}s:'.format(window['start'], window['end']),
              predicted_class_name,
              '/ {} usage'.format('active' if is_active_usage(window) else 'inactive'),
              '/ foreground app:', window['foreground_app'])

