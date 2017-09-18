import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier

from sauron.classification.feature_extraction import load_features, save_features
from sauron.classification.training import extract_training_data_features_from_folder
from sauron.classification.classification import Classifier

#######################################
training_data = extract_training_data_features_from_folder('../Data/training/')
training_data = training_data.dropna()
save_features("../Data/training/features.csv", training_data)
#training_data = load_features('../Data/features.csv')

# Perform cross-validation using 25% of the training data for testing
training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= .75
training_data.head()

train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

clf = Classifier()
clf.train(train)
print(pd.crosstab(test['type'], clf.classify(test), rownames=['actual'], colnames=['preds']))

