import numpy as np
import pandas as pd
import itertools
from sklearn.metrics import confusion_matrix
import matplotlib.pyplot as plt

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
    'num_rums': 500,
    'classifiers': {'RF', 'KNN', 'SVM', 'LR'},

    ###################################################################
    #'features': ALL_FEATURES,
    'features': {
        'accelerometer': {
            'methods': {'axes', 'magnitude'},
            'features': {'mean', 'stddev', 'median', 'rms'},
        },
        #'gyroscope': {
        #    'methods': {'axes', 'magnitude'},
        #    'features': {'mean', 'stddev', 'median', 'rms'},
        #},
        #'magnetometer': {
        #    'methods': {'axes', 'magnitude'},
        #    'features': {'mean', 'stddev', 'median', 'rms'},
        #},
        #'linear_acceleration': {
        #    'methods': {'axes', 'magnitude'},
        #    'features': {'mean', 'stddev', 'median', 'rms'},
        #},
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
print('Running classifiers...')
motions = set(training_data['type'])
total_crosstabs = {clf_type: None for clf_type in config['classifiers']}
total_confusion_matrices = {clf_type: None for clf_type in config['classifiers']}

for i in range(config['num_rums']):
    if i % 10 == 1:
        print('Iteration', i)

    # Randomly split data for training and cross-validation
    training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= (1 - (config['data_crossvalidation_percentage'] / 100))
    training_data.head()

    train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

    # Train classifiers
    classifiers = {clf_type: Classifier(clf_type=clf_type, feature_config=config['features']) for clf_type in config['classifiers']}
    for clf_type, clf in classifiers.items():
        clf.train(train)
        
        pred = clf.classify(test)
        crosstab = pd.crosstab(test['type'], pred, rownames=['actual'], colnames=['preds'])
        confusion_matrix_ = confusion_matrix(test['type'], pred)
        
        crosstab = crosstab.reindex_axis(config['classes'], axis=0)
        crosstab = crosstab.reindex_axis(config['classes'], axis=1)
        crosstab.fillna(0, inplace=True)
       
        total_crosstabs[clf_type] = crosstab if total_crosstabs[clf_type] is None else total_crosstabs[clf_type] + crosstab
        total_confusion_matrices[clf_type] = confusion_matrix_ if total_confusion_matrices[clf_type] is None else total_confusion_matrices[clf_type] + confusion_matrix_
print('Classifiers done!')

# Compute predicition accuracies
print('Computing prediction accuracies...')
accuracies = pd.DataFrame(0, index=sorted(config['classifiers']), columns=sorted(motions))

for clf_type in config['classifiers']:
    sums = total_crosstabs[clf_type].sum(axis=1)
    for motion in motions:
        accuracies.loc[clf_type, motion] = 100 * total_crosstabs[clf_type].loc[motion, motion] / sums[motion]
print('Done computing accuracies!')

#######################################################################
# Print info
print(config)

print('-------------------------------------------------------------------')
print('-------------------------------------------------------------------')

# Print crosstabs
for clf_type, crosstab in total_crosstabs.items():
    print(clf_type)
    print(crosstab)
    
    sums = crosstab.sum(axis=1)
    for motion1 in motions:
        for motion2 in motions:
            crosstab.loc[motion1, motion2] = crosstab.loc[motion1, motion2] / sums[motion1]
        
    crosstab.to_csv('output/' + clf_type + '.csv', sep='&', float_format='%.2f')
    print('-------------------------------------------------------------------')

print('-------------------------------------------------------------------')

# Print accuracies
print(accuracies)
accuracies = accuracies.reindex_axis(config['classes'], axis=1)
accuracies.to_csv('output/accuracies.csv', sep='&', float_format='%.2f')


#######################################################################
def plot_confusion_matrix(cm, classes, normalize=False, title='Confusion matrix', cmap=plt.cm.Blues):
    """
    This function prints and plots the confusion matrix.
    Normalization can be applied by setting `normalize=True`.
    """
    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]

    plt.imshow(cm, interpolation='nearest', cmap=cmap)
    plt.title(title)
    plt.colorbar()
    tick_marks = np.arange(len(classes))
    plt.xticks(tick_marks, classes, rotation=45)
    plt.yticks(tick_marks, classes)

    fmt = '.2f' if normalize else 'd'
    thresh = cm.max() / 2.
    for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
        plt.text(j, i, format(cm[i, j], fmt),
                 horizontalalignment="center",
                 color="white" if cm[i, j] > thresh else "black")

    plt.tight_layout()
    plt.ylabel('True label')
    plt.xlabel('Predicted label')
    
# Plot confusion matrices
for clf_type, mat in total_confusion_matrices.items():
    np.set_printoptions(precision=2)

    #plt.figure()
    #plot_confusion_matrix(mat, classes=motions, normalize=True, title=clf_type)

#plt.show()
