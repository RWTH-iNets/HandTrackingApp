from itsdangerous import URLSafeTimedSerializer
from django.conf import settings
from django.db import models
import base64
import json

class User(models.Model):
  signed_id = models.CharField(max_length=2048)
  model = models.CharField(max_length=1024, null=True)
  registered_on = models.DateTimeField(auto_now_add=True)

  def generate_signed_id(self):
    s = URLSafeTimedSerializer(settings.SECRET_KEY)
    new_id = s.dumps(self.id)
    self.signed_id = new_id

  def __str__(self):
    return  str(self.signed_id) + " on: " + str(self.model)

class UploadSession(models.Model):
  signed_id = models.CharField(max_length=2048)
  user = models.ForeignKey('User',
                          on_delete=models.CASCADE)
  db = models.CharField(max_length=1024, null=True)
  created_on = models.DateTimeField(auto_now_add=True)

  def generate_signed_id(self):
    s = URLSafeTimedSerializer(settings.SECRET_KEY)
    new_id = s.dumps(self.id)
    self.signed_id = new_id

  def reassemble_to_json(self):
    json_str = ""
    index = 0
    for chunk in self.uploadchunk_set.all().order_by('offset'):
      if chunk.offset != index:
        print("chunk out of sequence!")
        return None
      index = index + len(chunk.data)
      json_str = json_str + str(chunk.data)
    session_obj = json.loads(json_str)

  def reassambe_to_file(self, path):
    base64_str = ""
    index = 0
    path = path + str(self.user.signed_id) + "-" + str(self.signed_id)[:10] + '.sqlite'

    with open(path, 'wb') as f:
      for chunk in self.uploadchunk_set.all().order_by('offset'):
        if chunk.offset != index:
          print("chunk out of sequence!")
          return None
        bin_data = base64.standard_b64decode(chunk.data)
        index = index + len(bin_data)
        f.write(bin_data)
      self.db = path
      self.save()

class UploadChunk(models.Model):
  session = models.ForeignKey('UploadSession',
                              on_delete=models.CASCADE)
  offset = models.BigIntegerField()
  data = models.TextField()

class LogSession(models.Model):
  user = models.ForeignKey('User',
                            on_delete=models.CASCADE)
  description = models.CharField(max_length=2048)
  timestamp = models.DateTimeField(auto_now_add=True)

class LogEvent(models.Model):
  log_session = models.ForeignKey('LogSession',
                                  on_delete=models.CASCADE)
  event_time = models.BigIntegerField()
  event_type = models.CharField(max_length=128)
  data0 = models.IntegerField()
  data1 = models.FloatField()
  data2 = models.FloatField()
  data3 = models.FloatField()
  data4 = models.FloatField()

