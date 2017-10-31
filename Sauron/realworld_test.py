import numpy as np
import pandas as pd
from collections import Counter

from sauron.io import load
from sauron.classification.feature_extraction import load_features, extract_features_from_windows, ALL_FEATURES, save_features
from sauron.classification.classification import Classifier
from sauron.classification.utils import is_active_usage, filter_windows

#######################################################################
#feature_config = ALL_FEATURES

feature_config = {
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
    'rotation_vector': {
        'methods': {'quaternion'},
        'features': {'mean', 'stddev', 'median', 'rms'},
    },
    'game_rotation_vector': {
        'methods': {'quaternion'},
        'features': {'mean', 'stddev', 'median', 'rms'},
    },
}

classes = ['standing_lefthand', 'standing_righthand', 'standing_bothhandslandscape', 'standing_leftpocket', 'standing_rightpocket', 'standing_leftear', 'standing_rightear', 'walking_lefthand', 'walking_righthand', 'walking_bothhandslandscape', 'walking_leftpocket', 'walking_rightpocket', 'walking_leftear', 'walking_rightear']

#######################################################################   
correct_windows = [
    'walking_righthand',  #  3.26s -  4.76s
    'walking_righthand',  #  4.01s -  5.51s
    'walking_righthand',  #  4.76s -  6.26s
    'walking_righthand',  #  5.51s -  7.01s
    'walking_righthand',  #  6.26s -  7.76s
    'walking_righthand',  #  7.01s -  8.51s
    'walking_righthand',  #  7.76s -  9.26s
    'walking_righthand',  #  8.51s - 10.01s
    'walking_righthand',  #  9.26s - 10.76s
    'walking_righthand',  # 10.01s - 11.51s
    'walking_righthand',  # 10.76s - 12.26s
    'walking_righthand',  # 11.51s - 13.01s
    'walking_righthand',  # 12.26s - 13.76s
    'walking_righthand',  # 13.01s - 14.51s
    'walking_righthand',  # 13.76s - 15.26s
    'walking_righthand',  # 14.51s - 16.01s
    'walking_righthand',  # 15.26s - 16.76s
    'walking_righthand',  # 16.01s - 17.51s
    'walking_righthand',  # 16.76s - 18.26s
    'walking_righthand',  # 17.51s - 19.01s
    'walking_righthand',  # 18.26s - 19.76s
    'walking_righthand',  # 19.01s - 20.51s
    'walking_righthand',  # 19.76s - 21.26s
    'walking_righthand',  # 20.51s - 22.01s
    'walking_righthand',  # 21.26s - 22.76s
    'walking_righthand',  # 22.01s - 23.51s
    'walking_righthand',  # 22.76s - 24.26s
    'walking_righthand',  # 23.51s - 25.01s
    'walking_righthand',  # 24.26s - 25.76s
    'walking_righthand',  # 25.01s - 26.51s
    'standing_righthand', # 25.76s - 27.26s
    'standing_righthand', # 26.51s - 28.01s
    'standing_righthand', # 27.26s - 28.76s
    'standing_righthand', # 28.01s - 29.51s
    'standing_rightear',  # 28.76s - 30.26s
    'standing_rightear',  # 29.51s - 31.01s
    'standing_rightear',  # 30.26s - 31.76s
    'walking_rightear',   # 31.01s - 32.51s
    'walking_rightear',   # 31.76s - 33.26s
    'walking_rightear',   # 32.51s - 34.01s
    'walking_rightear',   # 33.26s - 34.76s
    'walking_rightear',   # 34.01s - 35.51s
    'walking_rightear',   # 34.76s - 36.26s
    'walking_rightear',   # 35.51s - 37.01s
    'walking_rightear',   # 36.26s - 37.76s
    'walking_rightear',   # 37.01s - 38.51s
    'walking_rightear',   # 37.76s - 39.26s
    'walking_rightear',   # 38.51s - 40.01s
    'walking_rightear',   # 39.26s - 40.76s
    'walking_rightear',   # 40.01s - 41.51s
    'walking_rightear',   # 40.76s - 42.26s
    'walking_rightear',   # 41.51s - 43.01s
    'walking_rightear',   # 42.26s - 43.76s
    'walking_rightear',   # 43.01s - 44.51s
    'walking_rightear',   # 43.76s - 45.26s
    'walking_rightear',   # 44.51s - 46.01s
    'walking_rightear',   # 45.26s - 46.76s
    'walking_rightear',   # 46.01s - 47.51s
    'walking_rightear',   # 46.76s - 48.26s
    'walking_rightear',   # 47.51s - 49.01s
    'walking_rightear',   # 48.26s - 49.76s
    'walking_rightear',   # 49.01s - 50.51s
    'walking_rightear',   # 49.76s - 51.26s
    'walking_rightear',   # 50.51s - 52.01s
    'walking_rightear',   # 51.26s - 52.76s
    'walking_rightear',   # 52.01s - 53.51s
    'walking_rightear',   # 52.76s - 54.26s
    'standing_rightear',  # 53.51s - 55.01s
    'standing_rightear',  # 54.26s - 55.76s
    'standing_rightear',  # 55.01s - 56.51s
    'standing_rightear',  # 55.76s - 57.26s
    'standing_rightear',  # 56.51s - 58.01s
    'standing_rightear',  # 57.26s - 58.76s
    'standing_rightear',  # 58.01s - 59.51s
    'standing_rightear',  # 58.76s - 60.26s
    'standing_rightear',  # 59.51s - 61.01s
    'standing_rightear',  # 60.26s - 61.76s
    'standing_rightear',  # 61.01s - 62.51s
    'standing_rightear',  # 61.76s - 63.26s
    'standing_rightear',  # 62.51s - 64.01s
    'standing_rightear',  # 63.26s - 64.76s
    'standing_rightear',  # 64.01s - 65.51s
    'standing_rightear',  # 64.76s - 66.26s
    'standing_rightear',  # 65.51s - 67.01s
    'standing_rightear',  # 66.26s - 67.76s
    'standing_rightear',  # 67.01s - 68.51s
    'standing_rightear',  # 67.76s - 69.26s
    'walking_rightear',   # 68.51s - 70.01s
    'walking_rightear',   # 69.26s - 70.76s
    'walking_rightear',   # 70.01s - 71.51s
    'walking_rightear',   # 70.76s - 72.26s
    'walking_rightear',   # 71.51s - 73.01s
    'walking_rightear',   # 72.26s - 73.76s
    'walking_rightear',   # 73.01s - 74.51s
    'walking_rightear',   # 73.76s - 75.26s
    'walking_rightear',   # 74.51s - 76.01s
    'walking_rightear',   # 75.26s - 76.76s
    'walking_rightear',   # 76.01s - 77.51s
    'walking_rightear',   # 76.76s - 78.26s
    'walking_rightear',   # 77.51s - 79.01s
    'walking_rightear',   # 78.26s - 79.76s
    'transition',  # 80.51s - 82.01s
    'standing_lefthand',  # 81.26s - 82.76s
    'walking_leftear',    # 82.01s - 83.51s
    'walking_leftear',    # 82.76s - 84.26s
    'walking_leftear',    # 83.51s - 85.01s
    'walking_leftear',    # 84.26s - 85.76s
    'walking_leftear',    # 85.01s - 86.51s
    'walking_leftear',    # 85.76s - 87.26s
    'walking_leftear',    # 86.51s - 88.01s
    'walking_leftear',    # 87.26s - 88.76s
    'walking_leftear',    # 88.01s - 89.51s
    'walking_leftear',    # 88.76s - 90.26s
    'walking_leftear',    # 91.01s - 92.51s
    'walking_leftear',    # 92.51s - 94.01s
    'walking_leftear',    # 93.26s - 94.76s
    'walking_leftear',    # 94.01s - 95.51s
    'walking_leftear',    # 94.76s - 96.26s
    'walking_leftear',    # 95.51s - 97.01s
    'walking_leftear',    # 96.26s - 97.76s
    'walking_leftear',    # 97.01s - 98.51s
    'walking_leftear',    # 97.76s - 99.26s
    'walking_leftear',    # 98.51s - 100.01s
    'walking_leftear',    # 99.26s - 100.76s
    'walking_leftear',    # 100.76s - 102.26s
    'walking_leftear',    # 101.51s - 103.01s
    'walking_leftear',    # 102.26s - 103.76s
    'walking_leftear',    # 103.01s - 104.51s
    'walking_leftear',    # 103.76s - 105.26s
    'walking_leftear',    # 106.76s - 108.26s
    'walking_leftear',    # 108.26s - 109.76s
    'walking_leftear',    # 109.01s - 110.51s
    'walking_leftear',    # 109.76s - 111.26s
    'walking_leftear',    # 110.51s - 112.01s
    'walking_leftear',    # 111.26s - 112.76s
    'standing_leftear',   # 112.01s - 113.51s
    'standing_leftear',   # 112.76s - 114.26s
    'standing_leftear',   # 113.51s - 115.01s
    'standing_leftear',   # 114.26s - 115.76s
    'standing_leftear',   # 115.01s - 116.51s
    'standing_leftear',   # 115.76s - 117.26s
    'standing_leftear',   # 116.51s - 118.01s
    'standing_leftear',   # 117.26s - 118.76s
    'standing_leftear',   # 118.01s - 119.51s
    'standing_leftear',   # 118.76s - 120.26s
    'standing_leftear',   # 119.51s - 121.01s
    'standing_leftear',   # 120.26s - 121.76s
    'standing_leftear',   # 121.01s - 122.51s
    'standing_leftear',   # 121.76s - 123.26s
    'standing_leftear',   # 122.51s - 124.01s
    'walking_leftear',    # 123.26s - 124.76s
    'walking_leftear',    # 124.01s - 125.51s
    'walking_leftear',    # 124.76s - 126.26s
    'walking_leftear',    # 125.51s - 127.01s
    'walking_leftear',    # 126.26s - 127.76s
    'walking_leftear',    # 127.01s - 128.51s
    'walking_leftear',    # 127.76s - 129.26s
    'walking_leftear',    # 128.51s - 130.01s
    'walking_leftear',    # 129.26s - 130.76s
    'walking_leftear',    # 130.01s - 131.51s
    'walking_leftear',    # 130.76s - 132.26s
    'walking_leftear',    # 131.51s - 133.01s
    'walking_leftear',    # 132.26s - 133.76s
    'walking_leftear',    # 133.01s - 134.51s
    'walking_leftear',    # 133.76s - 135.26s
    'walking_leftear',    # 134.51s - 136.01s
    'walking_righthand',  # 135.26s - 136.76s
    'walking_righthand',  # 136.01s - 137.51s
    'walking_righthand',  # 136.76s - 138.26s
    'walking_righthand',  # 137.51s - 139.01s
    'walking_righthand',  # 138.26s - 139.76s
    'walking_righthand',  # 139.01s - 140.51s
    'walking_righthand',  # 139.76s - 141.26s
    'standing_rightpocket',  # 140.51s - 142.01s
    'standing_rightpocket',  # 141.26s - 142.76s
    'standing_rightpocket',  # 142.01s - 143.51s
    'standing_rightpocket',  # 142.76s - 144.26s
    'standing_rightpocket',  # 143.51s - 145.01s
    'standing_rightpocket',  # 144.26s - 145.76s
    'standing_rightpocket',  # 145.01s - 146.51s
    'standing_rightpocket',  # 145.76s - 147.26s
    'standing_rightpocket',  # 146.51s - 148.01s
    'standing_rightpocket',  # 147.26s - 148.76s
    'standing_rightpocket',  # 148.01s - 149.51s
    'standing_rightpocket',  # 148.76s - 150.26s
    'standing_rightpocket',  # 149.51s - 151.01s
    'standing_rightpocket',  # 150.26s - 151.76s
    'standing_rightpocket',  # 151.01s - 152.51s
    'standing_rightpocket',  # 151.76s - 153.26s
    'standing_rightpocket',  # 152.51s - 154.01s
    'standing_rightpocket',  # 153.26s - 154.76s
    'standing_rightpocket',  # 154.01s - 155.51s
    'standing_rightpocket',  # 154.76s - 156.26s
    'walking_rightpocket',  # 174.26s - 175.76s
    'walking_rightpocket',  # 175.01s - 176.51s
    'walking_rightpocket',  # 175.76s - 177.26s
    'walking_rightpocket',  # 176.51s - 178.01s
    'walking_rightpocket',  # 177.26s - 178.76s
    'walking_rightpocket',  # 178.01s - 179.51s
    'walking_rightpocket',  # 178.76s - 180.26s
    'transition',  # 179.51s - 181.01s
    'transition',  # 180.26s - 181.76s
    'transition',  # 181.01s - 182.51s
    'transition',  # 181.76s - 183.26s
    'transition',  # 183.26s - 184.76s
    'transition',  # 184.01s - 185.51s
    'walking_leftpocket',  # 184.76s - 186.26s
    'walking_leftpocket',  # 185.51s - 187.01s
    'walking_leftpocket',  # 187.01s - 188.51s
    'walking_leftpocket',  # 187.76s - 189.26s
    'walking_leftpocket',  # 188.51s - 190.01s
    'walking_leftpocket',  # 189.26s - 190.76s
    'walking_leftpocket',  # 190.01s - 191.51s
    'walking_leftpocket',  # 190.76s - 192.26s
    'walking_leftpocket',  # 191.51s - 193.01s
    'walking_leftpocket',  # 196.01s - 197.51s
    'walking_leftpocket',  # 196.76s - 198.26s
    'walking_leftpocket',  # 197.51s - 199.01s
    'walking_leftpocket',  # 198.26s - 199.76s
    'walking_leftpocket',  # 199.01s - 200.51s
    'walking_leftpocket',  # 199.76s - 201.26s
    'walking_leftpocket',  # 200.51s - 202.01s
    'walking_leftpocket',  # 201.26s - 202.76s
    'walking_leftpocket',  # 217.01s - 218.51s
    'walking_leftpocket',  # 217.76s - 219.26s
    'walking_leftpocket',  # 218.51s - 220.01s
    'walking_leftpocket',  # 219.26s - 220.76s
    'walking_leftpocket',  # 220.01s - 221.51s
    'walking_leftpocket',  # 220.76s - 222.26s
    'walking_leftpocket',  # 221.51s - 223.01s
    'standing_leftpocket',  # 222.26s - 223.76s
    'transition',  # 225.26s - 226.76s
    'transition',  # 226.01s - 227.51s
    'standing_lefthand',  # 226.76s - 228.26s
    'standing_lefthand',  # 227.51s - 229.01s
    'standing_lefthand',  # 228.26s - 229.76s
    'standing_lefthand',  # 229.01s - 230.51s
    'standing_lefthand',  # 229.76s - 231.26s
    'standing_lefthand',  # 230.51s - 232.01s
    'standing_lefthand',  # 231.26s - 232.76s
    'standing_lefthand',  # 232.01s - 233.51s
    'standing_lefthand',  # 232.76s - 234.26s
    'standing_lefthand',  # 233.51s - 235.01s
    'standing_lefthand',  # 234.26s - 235.76s
    'standing_lefthand',  # 235.01s - 236.51s
    'standing_lefthand',  # 235.76s - 237.26s
    'standing_lefthand',  # 236.51s - 238.01s
    'standing_lefthand',  # 237.26s - 238.76s
    'standing_lefthand',  # 238.01s - 239.51s
    'standing_lefthand',  # 238.76s - 240.26s
    'standing_lefthand',  # 239.51s - 241.01s
    'standing_lefthand',  # 240.26s - 241.76s
    'standing_lefthand',  # 241.01s - 242.51s
    'standing_lefthand',  # 241.76s - 243.26s
    'standing_lefthand',  # 242.51s - 244.01s
    'standing_lefthand',  # 243.26s - 244.76s
    'standing_lefthand',  # 244.01s - 245.51s
    'transition',  # 244.76s - 246.26s
    'standing_bothhandslandscape',  # 245.51s - 247.01s
    'standing_bothhandslandscape',  # 246.26s - 247.76s
    'standing_bothhandslandscape',  # 247.01s - 248.51s
    'standing_bothhandslandscape',  # 247.76s - 249.26s
    'standing_bothhandslandscape',  # 248.51s - 250.01s
    'standing_bothhandslandscape',  # 249.26s - 250.76s
    'standing_bothhandslandscape',  # 250.01s - 251.51s
    'standing_bothhandslandscape',  # 250.76s - 252.26s
    'standing_bothhandslandscape',  # 251.51s - 253.01s
    'standing_bothhandslandscape',  # 252.26s - 253.76s
    'standing_bothhandslandscape',  # 253.01s - 254.51s
    'standing_bothhandslandscape',  # 253.76s - 255.26s
    'standing_bothhandslandscape',  # 254.51s - 256.01s
    'standing_bothhandslandscape',  # 255.26s - 256.76s
    'standing_bothhandslandscape',  # 256.01s - 257.51s
    'standing_bothhandslandscape',  # 256.76s - 258.26s
    'standing_bothhandslandscape',  # 257.51s - 259.01s
    'standing_bothhandslandscape',  # 258.26s - 259.76s
    'walking_bothhandslandscape',  # 259.01s - 260.51s
    'walking_bothhandslandscape',  # 259.76s - 261.26s
    'walking_bothhandslandscape',  # 260.51s - 262.01s
    'walking_bothhandslandscape',  # 261.26s - 262.76s
    'walking_bothhandslandscape',  # 262.01s - 263.51s
    'walking_bothhandslandscape',  # 262.76s - 264.26s
    'walking_bothhandslandscape',  # 263.51s - 265.01s
    'walking_bothhandslandscape',  # 264.26s - 265.76s
    'walking_bothhandslandscape',  # 265.01s - 266.51s
    'walking_bothhandslandscape',  # 265.76s - 267.26s
    'walking_bothhandslandscape',  # 266.51s - 268.01s
    'walking_bothhandslandscape',  # 267.26s - 268.76s
    'walking_bothhandslandscape',  # 268.01s - 269.51s
    'walking_bothhandslandscape',  # 268.76s - 270.26s
    'walking_bothhandslandscape',  # 269.51s - 271.01s
    'walking_bothhandslandscape',  # 270.26s - 271.76s
    'walking_bothhandslandscape',  # 271.01s - 272.51s
    'walking_bothhandslandscape',  # 271.76s - 273.26s
    'walking_bothhandslandscape',  # 272.51s - 274.01s
    'walking_bothhandslandscape',  # 273.26s - 274.76s
    'walking_bothhandslandscape',  # 274.01s - 275.51s
    'walking_bothhandslandscape',  # 274.76s - 276.26s
    'walking_bothhandslandscape',  # 275.51s - 277.01s
    'walking_bothhandslandscape',  # 276.26s - 277.76s
    'walking_bothhandslandscape',  # 277.01s - 278.51s
    'walking_bothhandslandscape',  # 277.76s - 279.26s
    'walking_bothhandslandscape',  # 278.51s - 280.01s
    'walking_bothhandslandscape',  # 279.26s - 280.76s
    'walking_bothhandslandscape',  # 280.01s - 281.51s
    'walking_bothhandslandscape',  # 280.76s - 282.26s
    'walking_bothhandslandscape',  # 281.51s - 283.01s
    'walking_bothhandslandscape',  # 282.26s - 283.76s
    'walking_bothhandslandscape',  # 283.01s - 284.51s
    'walking_bothhandslandscape',  # 283.76s - 285.26s
    'walking_bothhandslandscape',  # 284.51s - 286.01s
    'walking_bothhandslandscape',  # 285.26s - 286.76s
    'walking_bothhandslandscape',  # 286.01s - 287.51s
    'walking_bothhandslandscape',  # 286.76s - 288.26s
    'walking_bothhandslandscape',  # 287.51s - 289.01s
    'walking_bothhandslandscape',  # 288.26s - 289.76s
    'walking_bothhandslandscape',  # 289.01s - 290.51s
    'walking_bothhandslandscape',  # 289.76s - 291.26s
    'walking_bothhandslandscape',  # 290.51s - 292.01s
    'walking_bothhandslandscape',  # 291.26s - 292.76s
    'walking_bothhandslandscape',  # 292.01s - 293.51s
    'walking_bothhandslandscape',  # 292.76s - 294.26s
    'walking_bothhandslandscape',  # 293.51s - 295.01s
    'walking_bothhandslandscape',  # 294.26s - 295.76s
    'walking_lefthand',  # 295.01s - 296.51s
    'walking_lefthand',  # 295.76s - 297.26s
    'walking_lefthand',  # 296.51s - 298.01s
    'walking_lefthand',  # 297.26s - 298.76s
    'walking_lefthand',  # 298.01s - 299.51s
    'walking_lefthand',  # 298.76s - 300.26s
    'walking_lefthand',  # 299.51s - 301.01s
    'walking_lefthand',  # 300.26s - 301.76s
    'walking_lefthand',  # 301.01s - 302.51s
    'walking_lefthand',  # 301.76s - 303.26s
    'walking_lefthand',  # 302.51s - 304.01s
    'walking_lefthand',  # 303.26s - 304.76s
    'walking_lefthand',  # 304.01s - 305.51s
    'walking_lefthand',  # 304.76s - 306.26s
    'walking_lefthand',  # 305.51s - 307.01s
    'walking_lefthand',  # 306.26s - 307.76s
    'walking_lefthand',  # 307.01s - 308.51s
    'walking_lefthand',  # 307.76s - 309.26s
    'walking_lefthand',  # 308.51s - 310.01s
    'walking_lefthand',  # 309.26s - 310.76s
    'walking_lefthand',  # 310.01s - 311.51s
    'walking_lefthand',  # 310.76s - 312.26s
    'walking_lefthand',  # 311.51s - 313.01s
    'walking_lefthand',  # 312.26s - 313.76s
    'walking_lefthand',  # 313.01s - 314.51s
    'walking_lefthand',  # 313.76s - 315.26s
    'standing_lefthand',  # 314.51s - 316.01s
    'standing_lefthand',  # 315.26s - 316.76s
    'standing_lefthand',  # 316.01s - 317.51s
    'standing_lefthand',  # 316.76s - 318.26s
    'standing_lefthand',  # 317.51s - 319.01s
    'standing_lefthand',  # 318.26s - 319.76s
    'standing_lefthand',  # 319.01s - 320.51s
    'transition',  # 319.76s - 321.26s
    'transition',  # 320.51s - 322.01s
    'transition',  # 321.26s - 322.76s
    'transition',  # 322.01s - 323.51s
    'standing_rightpocket',  # 322.76s - 324.26s
    'standing_rightpocket',  # 323.51s - 325.01s
    'walking_rightpocket',  # 324.26s - 325.76s
    'walking_rightpocket',  # 328.01s - 329.51s
    'standing_rightpocket',  # 328.76s - 330.26s
    'standing_rightpocket',  # 329.51s - 331.01s
    'standing_rightpocket',  # 331.01s - 332.51s
    'standing_rightpocket',  # 331.76s - 333.26s
    'standing_rightpocket',  # 332.51s - 334.01s
    'standing_rightpocket',  # 333.26s - 334.76s
    'standing_rightpocket',  # 334.01s - 335.51s
    'standing_righthand',  # 344.51s - 346.01s
    'standing_righthand',  # 345.26s - 346.76s
    'standing_righthand',  # 346.01s - 347.51s
    'standing_righthand',  # 346.76s - 348.26s
    'standing_righthand',  # 347.51s - 349.01s
    'standing_righthand',  # 348.26s - 349.76s
    'standing_righthand',  # 349.01s - 350.51s
    'standing_righthand',  # 349.76s - 351.26s
]

timeline = False

# Load features and train classifier
training_data = load_features('../Data/training/features.csv')

# Load session
db = load('realworld_test.json')
for session_id in db.get_all_session_ids():
    session = db.get_session(session_id)
    print('Analyzing session:', session.description)
    print('  - Duration:', session.events[-1].session_time - session.events[0].session_time, 'seconds')
    print('  -', len(session.events), 'logged events')

    # Load & filter windows
    windows = list(session.events.sliding_window(1.5, 0.5))
    print('  -', len(windows), 'windows in total')
    #windows = list(filter_windows(windows, active_usage=True))
    #windows = list(filter_windows(windows, active_usage=True, foreground_app={"com.google.android.youtube", "de.rwth_aachen.inets.gollum"}))
    print('  -', len(windows), 'windows after filtering')

    # Extract features
    features = extract_features_from_windows(windows, feature_config=feature_config)
    first = True
    for row in features.iterrows():
        if row[1].isnull().values.any():
            print('Deleting window {} ({:5.2f}s - {:5.2f}s) due to NaNs'.format(row[0], windows[row[0]]['start'], windows[row[0]]['end']))
            del windows[row[0]]
    features.dropna(inplace=True)
    
    if timeline:
        # Classify windows
        predicted_class_names = None
        for i in range(500):
            clf = Classifier(clf_type='RF', 
                             clf_args={'n_estimators': 14, 'min_samples_leaf': 1, 'max_features': 'log2'}, 
                             feature_config=feature_config)
            clf.train(training_data)
            if predicted_class_names is None:
                predicted_class_names = [[x] for x in clf.classify(features)]
            else:
                for i, x in enumerate(clf.classify(features)):
                    predicted_class_names[i].append(x)
                    
        predicted_class_names = [Counter(x).most_common(1)[0][0] for x in predicted_class_names]
        print(predicted_class_names)    

        # Output results
        static_time_offset = 3.263
        for window, predicted_class_name in zip(windows, predicted_class_names):
            print('{:5.2f}s - {:5.2f}s:'.format(window['start'] + static_time_offset, window['end'] + static_time_offset),
                  predicted_class_name,
                  '/ {} usage'.format('active' if is_active_usage(window) else 'inactive'),
                  '/ foreground app:', window['foreground_app'])
                  
        # Export timeline
        prev_class = None
        prev_class_start = None
        for id, (window, predicted_class_name) in enumerate(zip(windows, predicted_class_names)):
            if prev_class is None:
                prev_class = predicted_class_name
                prev_class_start = window['start'] + static_time_offset
            elif prev_class != predicted_class_name:
                if id < len(predicted_class_names) - 1 and predicted_class_names[id + 1] == prev_class:
                    # just 1 window jitter, filter it out
                    continue
                else:
                    end = window['start'] + static_time_offset
                    print('{:5.2f}s - {:5.2f}s: {}'.format(prev_class_start, end, prev_class))
                    prev_class = predicted_class_name
                    prev_class_start = window['start'] + static_time_offset
    else:
        motions = set([x.replace('left', '').replace('right', '') for x in classes] + ['transition'])
        total_crosstab = None

        for i in range(500):
            if i % 10 == 1:
                print('Iteration', i)

            # Train classifier
            clf = Classifier(clf_type='RF', 
                             clf_args={'n_estimators': 14, 'min_samples_leaf': 1, 'max_features': 'log2'}, 
                             feature_config=feature_config)
            #clf = Classifier(clf_type='KNN', 
            #                 clf_args={'n_neighbors': 5}, 
            #                 feature_config=feature_config)
            clf.train(training_data)
            
            pred = clf.classify(features)
            crosstab = pd.crosstab(pd.Index([x.replace('left', '').replace('right', '') for x in correct_windows]), pd.Index([x.replace('left', '').replace('right', '') for x in pred]), rownames=['actual'], colnames=['preds'])
            
            crosstab = crosstab.reindex_axis(['standing_hand', 'standing_bothhandslandscape', 'standing_pocket', 'standing_ear', 'walking_hand', 'walking_bothhandslandscape', 'walking_pocket', 'walking_ear', 'transition'], axis=0)
            crosstab = crosstab.reindex_axis(['standing_hand', 'standing_bothhandslandscape', 'standing_pocket', 'standing_ear', 'walking_hand', 'walking_bothhandslandscape', 'walking_pocket', 'walking_ear', 'transition'], axis=1)
            crosstab.fillna(0, inplace=True)
           
            total_crosstab = crosstab if total_crosstab is None else total_crosstab + crosstab

        # Print crosstabs
        print(total_crosstab)
        
        sums = total_crosstab.sum(axis=1)
        for motion1 in motions:
            for motion2 in motions:
                total_crosstab.loc[motion1, motion2] = total_crosstab.loc[motion1, motion2] / sums[motion1]
            
        total_crosstab.to_csv('output/realworld_test/RF_no_lr.csv', sep='&', float_format='%.2f')
        print('-------------------------------------------------------------------')
