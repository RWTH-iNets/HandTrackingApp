import os

from .json_io import JSONDatabase
from .sqlite3_io import SQLiteDatabase


def load(filepath):
    filename, extension = os.path.splitext(filepath)
    
    if extension == '.db':
        return SQLiteDatabase(filepath)
    elif extension == '.json':
        return JSONDatabase(filename=filepath)
    else:
        raise ValueError('Unknown file extension "{}"!'.format(extension))

def loads(buffer, fileformat):    
    if fileformat == 'json':
        return JSONDatabase(buffer=buffer)
    else:
        raise ValueError('Unknown buffer format "{}"!'.format(fileformat))
