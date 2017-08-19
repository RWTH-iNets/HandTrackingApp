import numpy as np
import pandas as pd

from ..logevent import AccelerometerEvent, GyroscopeEvent, MagnetometerEvent

###########################################################
################### Feature Naming ########################
###########################################################
_SOURCE_PREFIXES = {
    'accelerometer': {'accel'},
    'gyroscope': {'gyro'},
    'magnetometer': {'magneto'},

    'all': {'accel', 'gyro', 'magneto'},
}

_METHOD_INFIXES = {
    'axes': {'x', 'y', 'z'},
    'magnitude': {'magnitude'},

    'all': {'x', 'y', 'z', 'magnitude'},
}

_FEATURE_POSTFIXES = {
    'mean': {'mean'},
    'stddev': {'stddev'},
    'median': {'median'},
    'rms': {'rms'},

    'all': {'mean', 'stddev', 'median', 'rms'},
}

def _build_feature_name(prefix, infix, postfix):
    return '{}_{}_{}'.format(prefix, infix, postfix)

def get_feature_names(selection):
    res = []
    for source, config in selection.items():
        for method in config['methods']:
            for feature in config['features']:
                res.extend(_build_feature_name(prefix, infix, postfix) for prefix in _SOURCE_PREFIXES[source] for infix in _METHOD_INFIXES[method] for postfix in _FEATURE_POSTFIXES[feature])
    return res


ALL_FEATURES = {
    'all': {
        'methods': {'all'},
        'features': {'all'},
    }
}

ALL_FEATURE_NAMES = get_feature_names(ALL_FEATURES)


###########################################################
################# Feature Extraction ######################
###########################################################
def _build_features(axes):
    res = {}

    squared_axes = {k: np.square(v) for k, v in axes.items() if v is not None}

    # Extract per-axis features
    for axis_name, axis_values in axes.items():
        res[axis_name] = {
            'mean': np.mean(axis_values) if axis_values is not None else np.nan,
            'stddev': np.std(axis_values) if axis_values is not None else np.nan,
            'median': np.median(axis_values) if axis_values is not None else np.nan,
            'rms': np.sqrt(np.mean(squared_axes[axis_name])) if axis_values is not None else np.nan,
            }

    # Extract magnitude features
    if len(squared_axes) == len(axes):
        magnitudes = np.sqrt(np.sum(list(squared_axes.values()), axis=0))
        res['magnitude'] = {
            'mean': np.mean(magnitudes),
            'stddev': np.std(magnitudes),
            'median': np.median(magnitudes),
            'rms': np.sqrt(np.mean(np.square(magnitudes))),
            }
    else:
        res['magnitude'] = {
            'mean': np.nan,
            'stddev': np.nan,
            'median': np.nan,
            'rms': np.nan,
        }

    return res

def extract_features_from_windows(windows, feature_config=ALL_FEATURES):
    # Initialize list of features and DataFrame
    feature_names = get_feature_names(feature_config)
    df = pd.DataFrame(index=range(len(windows)), columns=feature_names)

    idx = 0
    for window in windows:
        values = {}

        # Accelerometer
        accel_events = [e for e in window['events'] if isinstance(e, AccelerometerEvent)]
        if accel_events:
            values['accel'] = _build_features({'x': [e.vector.x for e in accel_events], 'y': [e.vector.y for e in accel_events], 'z': [e.vector.z for e in accel_events]})
        else:
            values['accel'] = _build_features({'x': None, 'y': None, 'z': None})

        # Gyroscope
        gyro_events = [e for e in window['events'] if isinstance(e, GyroscopeEvent)]
        if gyro_events:
            values['gyro'] = _build_features({'x': [e.vector.x for e in gyro_events], 'y': [e.vector.y for e in gyro_events], 'z': [e.vector.z for e in gyro_events]})
        else:
            values['gyro'] = _build_features({'x': None, 'y': None, 'z': None})

        # Magnetometer
        magneto_events = [e for e in window['events'] if isinstance(e, MagnetometerEvent)]
        if magneto_events:
            values['magneto'] = _build_features({'x': [e.vector.x for e in magneto_events], 'y': [e.vector.y for e in magneto_events], 'z': [e.vector.z for e in magneto_events]})
        else:
            values['magneto'] = _build_features({'x': None, 'y': None, 'z': None})


        # Insert features to DataFrame
        # TODO: this is highly inefficient and should be optimized
        for prefix, prefix_data in values.items():
            for infix, infix_data in prefix_data.items():
                for postfix, postfix_data in infix_data.items():
                    feature_name = _build_feature_name(prefix, infix, postfix)
                    if feature_name in feature_names:
                        df.loc[idx, feature_name] = postfix_data

        idx = idx + 1

    return df

def extract_features_from_session(session, feature_config=ALL_FEATURES, window_size=1.5, window_overlap=0.5):
    return extract_features_from_windows(list(session.events.sliding_window(window_size, window_overlap)), feature_config=feature_config)

def load_features(filename):
    return pd.DataFrame.from_csv(filename)

def save_features(filename, df):
    df.to_csv(filename)

