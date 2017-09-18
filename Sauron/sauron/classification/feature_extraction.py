import numpy as np
import pandas as pd

from ..logevent import AccelerometerEvent, GyroscopeEvent, MagnetometerEvent, ProximitySensorEvent, LightSensorEvent, LinearAccelerationEvent, RotationVectorEvent, GameRotationVectorEvent

###########################################################
################### Feature Naming ########################
###########################################################
_SOURCE_PREFIXES = {
    'accelerometer': {'accel'},
    'gyroscope': {'gyro'},
    'magnetometer': {'magneto'},
    'proximity': {'proximity'},
    'brightness': {'brightness'},
    'linear_acceleration': {'lin_accel'},
    'rotation_vector': {'rot_vec'},
    'game_rotation_vector': {'game_rot_vec'},

    'all': {'accel', 'gyro', 'magneto', 'proximity', 'brightness', 'lin_accel', 'rot_vec', 'game_rot_vec'},
}

_METHOD_INFIXES = {
    'axes': {'x', 'y', 'z'},
    'quaternion': {'w', 'x', 'y', 'z'},
    'magnitude': {'magnitude'},
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
    'accelerometer': {
        'methods': {'axes', 'magnitude'},
        'features': {'all'},
    },
    'gyroscope': {
        'methods': {'axes', 'magnitude'},
        'features': {'all'},
    },
    'magnetometer': {
        'methods': {'axes', 'magnitude'},
        'features': {'all'},
    },
    #'proximity': {
    #    'methods': {'magnitude'},
    #    'features': {'all'},
    #},
    #'brightness': {
    #    'methods': {'magnitude'},
    #    'features': {'all'},
    #},
    'linear_acceleration': {
        'methods': {'axes', 'magnitude'},
        'features': {'all'},
    },
    'rotation_vector': {
        'methods': {'quaternion'},
        'features': {'all'},
    },
    'game_rotation_vector': {
        'methods': {'quaternion'},
        'features': {'all'},
    },
}

ALL_FEATURE_NAMES = get_feature_names(ALL_FEATURES)


###########################################################
################# Feature Extraction ######################
###########################################################
def _build_features(axes, compute_magnitude=True):
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
    if compute_magnitude and len(axes) > 1:
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
            values['accel'] = _build_features({'x': [e.vector.x for e in accel_events], 'y': [e.vector.y for e in accel_events], 'z': [e.vector.z for e in accel_events]}, compute_magnitude=True)
        else:
            values['accel'] = _build_features({'x': None, 'y': None, 'z': None})

        # Gyroscope
        gyro_events = [e for e in window['events'] if isinstance(e, GyroscopeEvent)]
        if gyro_events:
            values['gyro'] = _build_features({'x': [e.vector.x for e in gyro_events], 'y': [e.vector.y for e in gyro_events], 'z': [e.vector.z for e in gyro_events]}, compute_magnitude=True)
        else:
            values['gyro'] = _build_features({'x': None, 'y': None, 'z': None})

        # Magnetometer
        magneto_events = [e for e in window['events'] if isinstance(e, MagnetometerEvent)]
        if magneto_events:
            values['magneto'] = _build_features({'x': [e.vector.x for e in magneto_events], 'y': [e.vector.y for e in magneto_events], 'z': [e.vector.z for e in magneto_events]}, compute_magnitude=True)
        else:
            values['magneto'] = _build_features({'x': None, 'y': None, 'z': None})

        # Proximity
        #proximity_events = [e for e in window['events'] if isinstance(e, ProximitySensorEvent)]
        #if proximity_events:
        #    values['proximity'] = _build_features({'magnitude': [e.distance for e in proximity_events]}, compute_magnitude=False)
        #else:
        #    values['proximity'] = _build_features({'magnitude': None})
        # TODO: normalized proximity

        # Brightness
        #brightness_events = [e for e in window['events'] if isinstance(e, LightSensorEvent)]
        #if brightness_events:
        #    values['brightness'] = _build_features({'magnitude': [e.brightness for e in brightness_events]}, compute_magnitude=False)
        #else:
        #    values['brightness'] = _build_features({'magnitude': None})

        # Linear Acceleration
        lin_accel_events = [e for e in window['events'] if isinstance(e, LinearAccelerationEvent)]
        if lin_accel_events:
            values['lin_accel'] = _build_features({'x': [e.vector.x for e in lin_accel_events], 'y': [e.vector.y for e in lin_accel_events], 'z': [e.vector.z for e in lin_accel_events]}, compute_magnitude=True)
        else:
            values['lin_accel'] = _build_features({'x': None, 'y': None, 'z': None})
            
        # Rotation Vector
        rot_vec_events = [e for e in window['events'] if isinstance(e, RotationVectorEvent)]
        
        if rot_vec_events:
            values['rot_vec'] = _build_features({'w': [e.quaternion.w for e in rot_vec_events], 'x': [e.quaternion.x for e in rot_vec_events], 'y': [e.quaternion.y for e in rot_vec_events], 'z': [e.quaternion.z for e in rot_vec_events]}, compute_magnitude=False)
        else:
            values['rot_vec'] = _build_features({'w': None, 'x': None, 'y': None, 'z': None})
            
        # Game Rotation Vector
        game_rot_vec_events = [e for e in window['events'] if isinstance(e, GameRotationVectorEvent)]
        
        if game_rot_vec_events:
            values['game_rot_vec'] = _build_features({'w': [e.quaternion.w for e in game_rot_vec_events], 'x': [e.quaternion.x for e in game_rot_vec_events], 'y': [e.quaternion.y for e in game_rot_vec_events], 'z': [e.quaternion.z for e in game_rot_vec_events]}, compute_magnitude=False)
        else:
            values['game_rot_vec'] = _build_features({'w': None, 'x': None, 'y': None, 'z': None})

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
