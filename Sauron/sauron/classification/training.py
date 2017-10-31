import os
import re

import numpy as np
import pandas as pd

from ..io import load
from .feature_extraction import extract_features_from_session

def extract_training_data_features_from_file(filename):
    print('Loading training data from', filename)
    db = load(filename)

    description_re = re.compile('^training_(?P<name>[A-Za-z0-9]+)_(?P<mode>[A-Za-z]+)_(?P<type>[A-Za-z]+)$')

    feature_set = []
    for session_id in db.get_all_session_ids():
        # Extract session information
        session = db.get_session(session_id)
        print(' - Session:', session.description)

        match = description_re.match(session.description)
        if match is None:
            raise RuntimeError('Invalid session description format!')
        
        session_name = match.group('name')
        session_mode = match.group('mode')
        session_type = match.group('type')

        if session_mode not in {'standing', 'walking'}:
            raise RuntimeError('Invalid session mode!')

        if session_type not in {'bothhandslandscape', 'righthand', 'lefthand', 'rightpocket', 'leftpocket', 'rightear', 'leftear'}:
            raise RuntimeError('Invalid session type!')

        print(' - Name:', session_name)
        print(' - Mode:', session_mode)
        print(' - Type:', session_type)

        # Extract features
        features = extract_features_from_session(session)
        features['name'] = session_name
        #features['mode'] = session_mode
        features['type'] = session_mode + '_' + session_type

        feature_set.append(features)

    return pd.concat(feature_set)

def extract_training_data_features_from_folder(foldername):
    feature_sets = []
    for root, dirs, files in os.walk(foldername):
        for file in (f for f in files if f.endswith('.json')):
            feature_sets.append(extract_training_data_features_from_file(os.path.join(root, file)))

    return pd.concat(feature_sets)


