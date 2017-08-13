import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier

#from sauron.classification.feature_extraction import load_features, save_features
from sauron.classification.training import extract_training_data_features_from_folder

#######################################
training_data = extract_training_data_features_from_folder('../Data/training/')
training_data = training_data.dropna()

training_data['is_train'] = np.random.uniform(0, 1, len(training_data)) <= .75
training_data.head()

train, test = training_data[training_data['is_train']==True], training_data[training_data['is_train']==False]

features = training_data.columns[:4]
clf = RandomForestClassifier(n_jobs=2)
y, target_names = pd.factorize(train['type'])
clf.fit(train[features], y)

preds = target_names[clf.predict(test[features])]
res = pd.crosstab(test['type'], preds, rownames=['actual'], colnames=['preds'])

print(res)

