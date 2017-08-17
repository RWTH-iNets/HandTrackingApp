import numpy as np
import pandas as pd

from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression

from .feature_extraction import ALL_FEATURES, get_feature_names

class Classifier:
    def __init__(self, clf_type='RF', clf_args=None, feature_config=None):
        self.class_ids = None
        self.class_names = None
        self.used_feature_names = self._init_feature_names(feature_config)
        print(self.used_feature_names, '({} features)'.format(len(self.used_feature_names)))

        # Initialize classifier
        if clf_type == 'RF':
            clf_args = self._init_default_args(clf_args, {'n_jobs': -1})
            self.classifier = RandomForestClassifier(**clf_args)
        elif clf_type == 'KNN':
            clf_args = self._init_default_args(clf_args, {'n_jobs': -1})
            self.classifier = KNeighborsClassifier(**clf_args)
        elif clf_type == 'SVM':
            clf_args = self._init_default_args(clf_args, {})
            self.classifier = SVC(**clf_args)
        elif clf_type == 'LR':
            clf_args = self._init_default_args(clf_args, {'n_jobs': -1})
            self.classifier = LogisticRegression(**clf_args)
        else:
            raise ValueError('Invalid classifier type {} specified!'.format(clf_type))

    @staticmethod
    def _init_default_args(args, default_args):
        args = {} if args is None else args
        for k, v in default_args.items():
            if k not in args:
                args[k] = v
        return args

    @staticmethod
    def _init_feature_names(feature_config):
        # Use default configuration if none is specified
        if feature_config is None:
            feature_config = ALL_FEATURES

        return get_feature_names(feature_config)

    def train(self, training_data):
        self.class_ids, self.class_names = pd.factorize(training_data['type'])
        self.classifier.fit(training_data[self.used_feature_names], self.class_ids)

    def classify(self, features):
        predicted_class_ids = self.classifier.predict(features[self.used_feature_names])
        return self.class_names[predicted_class_ids]

