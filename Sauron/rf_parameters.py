import numpy as np
import pandas as pd

from sauron.io import load
from sauron.classification.feature_extraction import load_features, extract_features_from_windows, ALL_FEATURES
from sauron.classification.classification import Classifier
from sauron.classification.training import extract_training_data_features_from_folder
from sauron.classification.utils import is_active_usage, filter_windows

#######################################################################
config = {
    #'data_source': 'training_folder',
    'data_source': 'stored_features',
    'data_crossvalidation_percentage': 20,

    ###################################################################
    'num_rums': 10,
    'params': [
        {'n_estimators': est, 'min_samples_leaf': leaf, 'max_features': max_features} for est in range(5, 15) for leaf in range(1, 5) for max_features in ['sqrt', 'log2', None]
    ],

    ###################################################################
    #'features': ALL_FEATURES,
    'features': {
        'accelerometer': {
            'methods': {'axes', 'magnitude'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        'gyroscope': {
            'methods': {'axes', 'magnitude'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        'magnetometer': {
            'methods': {'axes', 'magnitude'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        'linear_acceleration': {
            'methods': {'axes', 'magnitude'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        #'rotation_vector': {
        #    'methods': {'quaternion'},
        #    'features': {'mean', 'stddev', 'median', 'rms'},
        #},
        #'game_rotation_vector': {
        #    'methods': {'quaternion'},
        #    'features': {'mean', 'stddev', 'median', 'rms'},
        #},
    },
    
    ###################################################################
    #'classes': ['standing_lefthand', 'standing_righthand', 'standing_bothhandslandscape', 'standing_leftpocket', 'standing_rightpocket', 'standing_leftear', 'standing_rightear'],
    'classes': ['walking_lefthand', 'walking_righthand', 'walking_bothhandslandscape', 'walking_leftpocket', 'walking_rightpocket', 'walking_leftear', 'walking_rightear'],
    #'classes': ['standing_lefthand', 'standing_righthand', 'standing_bothhandslandscape', 'standing_leftpocket', 'standing_rightpocket', 'standing_leftear', 'standing_rightear', 'walking_lefthand', 'walking_righthand', 'walking_bothhandslandscape', 'walking_leftpocket', 'walking_rightpocket', 'walking_leftear', 'walking_rightear'],
}

#######################################################################
# Load features
if config['data_source'] == 'training_folder':
    print('Extracting features from folder...')
    training_data = extract_training_data_features_from_folder('../Data/training/')
    training_data = training_data.dropna()
elif config['data_source'] == 'stored_features':
    print('Loading stored features...')
    training_data = load_features('../Data/training/features.csv')
else:
    raise ValueError('Invalid data source specified!')
    
# Filter Classes
training_data = training_data[training_data['type'].isin(config['classes'])]
print('Features loaded!')

# Run!
print('Running RF...')
motions = set(training_data['type'])
total_crosstabs = {k: None for k, v in enumerate(config['params'])}

for i in range(config['num_rums']):
    if i % 10 == 1:
        print('Iteration', i)

    # Randomly split data for training and cross-validation
    training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= (1 - (config['data_crossvalidation_percentage'] / 100))
    training_data.head()

    train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

    # Train classifiers
    params = {k: Classifier(clf_type='RF', clf_args=v, feature_config=config['features']) for k, v in enumerate(config['params'])}
    for k, clf in params.items():
        clf.train(train)
        
        pred = clf.classify(test)
        crosstab = pd.crosstab(test['type'], pred, rownames=['actual'], colnames=['preds'])
        
        crosstab = crosstab.reindex_axis(config['classes'], axis=0)
        crosstab = crosstab.reindex_axis(config['classes'], axis=1)
        crosstab.fillna(0, inplace=True)
       
        total_crosstabs[k] = crosstab if total_crosstabs[k] is None else total_crosstabs[k] + crosstab
print('RF done!')

# Compute predicition accuracies
print('Computing prediction accuracies...')
accuracies = pd.DataFrame(0, index=range(len(config['params'])), columns=sorted(motions))

for k, v in enumerate(config['params']):
    sums = total_crosstabs[k].sum(axis=1)
    for motion in motions:
        accuracies.loc[k, motion] = 100 * total_crosstabs[k].loc[motion, motion] / sums[motion]
    for clf_k, clf_v in v.items():
        accuracies.loc[k, clf_k] = clf_v
print('Done computing accuracies!')

#######################################################################
# Print info
print(config)

print('-------------------------------------------------------------------')
print('-------------------------------------------------------------------')

# Print crosstabs
for k, crosstab in total_crosstabs.items():
    print(k)
    print(crosstab)
    
    sums = crosstab.sum(axis=1)
    for motion1 in motions:
        for motion2 in motions:
            crosstab.loc[motion1, motion2] = crosstab.loc[motion1, motion2] / sums[motion1]
        
    crosstab.to_csv('output/RF_Parameters/RF_' + str(k) + '.csv', sep='&', float_format='%.2f')
    print('-------------------------------------------------------------------')
    
print('-------------------------------------------------------------------')

# Print accuracies
print(accuracies)
accuracies = accuracies.reindex_axis(['n_estimators', 'min_samples_leaf', 'max_features'] + config['classes'], axis=1)
accuracies.to_csv('output/RF_Parameters/RF_accuracies.csv', sep='&', float_format='%.2f')
