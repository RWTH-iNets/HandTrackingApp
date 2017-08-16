import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier

from sauron.io import load
from sauron.classification.feature_extraction import load_features, extract_features_from_windows
from sauron.classification.utils import filter_windows

training_features = load_features('../Data/training/features.csv')
used_feature_indices = training_features.columns[:-2]
print('Using features:', used_feature_indices)
class_ids, class_names = pd.factorize(training_features['type'])

clf = RandomForestClassifier(n_jobs=-1)
clf.fit(training_features[used_feature_indices], class_ids)

db = load('../Data/test/session.json')
for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)
    print('Analyzing session:', session.description)
    print('  -', len(session.events), 'logged events')

    # Load windows
    windows = list(session.events.sliding_window(1.5, 0.5))
    print('  -', len(windows), 'windows in total')
    #windows = list(filter_windows(windows, active_usage=True))
    #windows = list(filter_windows(windows, active_usage=True, foreground_app={"com.google.android.youtube", "de.rwth_aachen.inets.gollum"}))
    #print('  -', len(windows), 'windows after filtering')

    # Extract features
    features = extract_features_from_windows(windows)

    # Classify windows
    predicted_class_ids = clf.predict(features[used_feature_indices])
    predicted_class_names = class_names[predicted_class_ids]

    print(predicted_class_names)

