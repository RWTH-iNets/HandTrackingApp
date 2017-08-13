import numpy as np
import pandas as pd

from ..logevent import AccelerometerEvent, GyroscopeEvent

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

def extract_features_from_windows(windows):
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
        accel_events = [e for e in window['events'] if isinstance(e, AccelerometerEvent)]
        if accel_events:
            accel = build_features({'x': [e.vector.x for e in accel_events], 'y': [e.vector.y for e in accel_events], 'z': [e.vector.z for e in accel_events]})
        else:
            accel = build_features({'x': None, 'y': None, 'z': None})

        gyro_events = [e for e in window['events'] if isinstance(e, GyroscopeEvent)]
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

def extract_features_from_session(session, window_size=1.5, window_overlap=0.5):
    return extract_features_from_windows(list(session.events.sliding_window(window_size, window_overlap)))

def load_features(filename):
    return pd.DataFrame.from_csv(filename)

def save_features(filename, df):
    df.to_csv(filename)

