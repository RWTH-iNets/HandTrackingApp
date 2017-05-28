import os
import re

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier

import sauron.io
from sauron.logevent import AccelerometerEvent, GyroscopeEvent

def build_features(axes):
    res = {}

    for axis_name, axis_values in axes.items():
        res[axis_name] = {
            'mean': np.mean(axis_values) if axis_values is not None else np.nan,
            'stddev': np.std(axis_values) if axis_values is not None else np.nan,
            'median': np.median(axis_values) if axis_values is not None else np.nan,
            'rms': np.sqrt(np.mean(np.square(axis_values))) if axis_values is not None else np.nan,
            }

    return res

def extract_features(session):
    windows = list(session.events.sliding_window(1.5, 0.5))
    df = pd.DataFrame(index=range(len(windows)), columns=[
        'accel_x_mean', 'accel_y_mean', 'accel_z_mean', 
        'accel_x_stddev', 'accel_y_stddev', 'accel_z_stddev',
        'accel_x_median', 'accel_y_median', 'accel_z_median',
        'accel_x_rms', 'accel_y_rms', 'accel_z_rms',
        'gyro_x_mean', 'gyro_y_mean', 'gyro_z_mean', 
        'gyro_x_stddev', 'gyro_y_stddev', 'gyro_z_stddev',
        'gyro_x_median', 'gyro_y_median', 'gyro_z_median',
        'gyro_x_rms', 'gyro_y_rms', 'gyro_z_rms',
        ])

    idx = 0
    for window in windows:
        accel_events = [e for e in window if isinstance(e, AccelerometerEvent)]
        if accel_events:
            accel = build_features({'x': [e.vector.x for e in accel_events], 'y': [e.vector.y for e in accel_events], 'z': [e.vector.z for e in accel_events]})
        else:
            accel = build_features({'x': None, 'y': None, 'z': None})

        gyro_events = [e for e in window if isinstance(e, GyroscopeEvent)]
        if gyro_events:
            gyro = build_features({'x': [e.vector.x for e in gyro_events], 'y': [e.vector.y for e in gyro_events], 'z': [e.vector.z for e in gyro_events]})
        else:
            gyro = build_features({'x': None, 'y': None, 'z': None})

        df.loc[idx] = [
            accel['x']['mean'], accel['y']['mean'], accel['z']['mean'], 
            accel['x']['stddev'], accel['y']['stddev'], accel['z']['stddev'],
            accel['x']['median'], accel['y']['median'], accel['z']['median'],
            accel['x']['rms'], accel['y']['rms'], accel['z']['rms'],
            gyro['x']['mean'], gyro['y']['mean'], gyro['z']['mean'], 
            gyro['x']['stddev'], gyro['y']['stddev'], gyro['z']['stddev'],
            gyro['x']['median'], gyro['y']['median'], gyro['z']['median'],
            gyro['x']['rms'], gyro['y']['rms'], gyro['z']['rms'],
            ]

        idx = idx + 1

    return df

def load_training_data(filename):
    print('Loading training data from', filename)
    db = sauron.io.load(filename)

    description_re = re.compile('^training_(?P<name>[A-Za-z]+)_(?P<type>[A-Za-z]+)$')

    feature_set = []
    for session_id in db.get_all_session_ids():
        # Extract session information
        session = db.get_session(session_id)
        print(' - Session:', session.description)

        match = description_re.match(session.description)
        if match is None:
            raise RuntimeError('Invalid session description!')
        
        session_name = match.group('name')
        session_type = match.group('type')

        print(' - Name:', session_name)
        print(' - Type:', session_type)

        # Extract features
        features = extract_features(session)
        features['name'] = session_name
        features['type'] = session_type

        feature_set.append(features)

    return pd.concat(feature_set)

def load_training_data_folder(foldername):
    feature_sets = []
    for root, dirs, files in os.walk(foldername):
        for file in files:
            feature_sets.append(load_training_data(os.path.join(root, file)))

    return pd.concat(feature_sets)


#######################################
training_data = load_training_data_folder('../Data/training/')
training_data = training_data.dropna()


training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= .75
training_data.head()

train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

features = training_data.columns[:4]
clf = RandomForestClassifier(n_jobs=2)
y, _ = pd.factorize(train['type'])
clf.fit(train[features], y)

target_names = np.array(['righthand', 'rightear', 'rightpocket'])
preds = target_names[clf.predict(test[features])]
res = pd.crosstab(test['type'], preds, rownames=['actual'], colnames=['preds'])

print(res)