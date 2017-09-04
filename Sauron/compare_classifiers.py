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
    'data_crossvalidation_percentage': 25,

    ###################################################################
    'num_rums': 100,
    'classifiers': {'RF', 'KNN', 'SVM', 'LR'},

    ###################################################################
    #'features': ALL_FEATURES,
    'features': {
        'accelerometer': {
            'methods': {'axes'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        'gyroscope': {
            'methods': {'axes'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
    },
}

#######################################################################
# Load features
if config['data_source'] == 'training_folder':
    training_data = extract_training_data_features_from_folder('../Data/training/')
    training_data = training_data.dropna()
elif config['data_source'] == 'stored_features':
    training_data = load_features('../Data/features.csv')
else:
    raise ValueError('Invalid data source specified!')

# Run!
motions = set(training_data['type'])
total_crosstabs = {clf_type: None for clf_type in config['classifiers']}

for i in range(config['num_rums']):
    # Randomly split data for training and cross-validation
    training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= (1 - (config['data_crossvalidation_percentage'] / 100))
    training_data.head()

    train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

    # Train classifiers
    classifiers = {clf_type: Classifier(clf_type=clf_type, feature_config=config['features']) for clf_type in config['classifiers']}
    for clf_type, clf in classifiers.items():
        clf.train(train)
        
        crosstab = pd.crosstab(test['type'], clf.classify(test), rownames=['actual'], colnames=['preds'])
        total_crosstabs[clf_type] = crosstab if total_crosstabs[clf_type] is None else total_crosstabs[clf_type] + crosstab

# Compute predicition accuracies
accuracies = pd.DataFrame(0, index=sorted(config['classifiers']), columns=sorted(motions))

for clf_type in config['classifiers']:
    sums = total_crosstabs[clf_type].sum(axis=0)
    for motion in motions:
        accuracies.loc[clf_type, motion] = 100 * total_crosstabs[clf_type].loc[motion, motion] / sums[motion]

#######################################################################
# Print info
print(config)

print('-------------------------------------------------------------------')
print('-------------------------------------------------------------------')

# Print crosstabs
for clf_type, crosstab in total_crosstabs.items():
    print(clf_type)
    print(crosstab)

print('-------------------------------------------------------------------')
print('-------------------------------------------------------------------')

# Print accuracies
print(accuracies)

