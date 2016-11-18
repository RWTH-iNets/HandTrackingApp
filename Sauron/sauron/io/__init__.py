import os

from .json import JSONDatabase
from .sqlite3 import SQLiteDatabase


def load(filepath):
    filename, extension = os.path.splitext(filepath)
    
    if extension == '.db':
        return SQLiteDatabase(filepath)
    elif extension == '.json':
        return JSONDatabase(filepath)
    else:
        raise ValueError('Unknown file extension "{}"!'.format(extension))
