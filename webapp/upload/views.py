from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_protect, ensure_csrf_cookie
from django.http import JsonResponse
from .models import User
from .models import UploadSession
from .models import LogSession
from .models import LogEvent
from .models import UploadChunk
import json
import base64

# Create your views here.
@ensure_csrf_cookie
def upload(request, command="", uid="", ul_session_id="", ul_chunk_offset=-1):
  context = {}
  print("request: upload")
  print("params: " + uid + ", " + command + ", " + ul_session_id + ", " +
          str(ul_chunk_offset))

  if request.method == "POST":
    if command == "chunk":
      print("new chunk")

      if len(request.body) > 0:
        #chunk = request.body.decode("utf-8")
        chunk = base64.standard_b64encode(request.body)
        chunk = chunk.decode("utf-8")
        ul_sess = UploadSession.objects.get(signed_id=ul_session_id)

        if not ul_sess:
          return JsonResponse({'status': 'ERROR'})
        else:
          ul_chunk = UploadChunk(session=ul_sess, data=chunk,
                                  offset=ul_chunk_offset)
          ul_chunk.save()
          print('added chunk to db')
          return JsonResponse({'status': 'OK'})
  else:
    if command == "new":
      try:
        user = User.objects.get(signed_id=uid)
      except:
        return JsonResponse({'status': 'ERROR'})

      ul_sess = UploadSession(user=user)
      ul_sess.save()
      ul_sess.generate_signed_id()
      ul_sess.save()
      response = JsonResponse({'status': 'OK',
                               'upload_session_id': ul_sess.signed_id})
      return response

    if command == "done":
      ul_sess = UploadSession.objects.get(signed_id=ul_session_id)
      if ul_sess is None:
        return JsonResponse({'status': 'ERROR'})

      ul_sess.reassambe_to_file("tmp/")
      #json_data = ul_sess.reassemble_to_json()
      #if json_data is None:
        #response = JsonResponse({'status': 'ERROR'})
        #return response
      #else:
        #ul_sess.delete()
        #desc = json_data["description"]
        #sess_entry = LogSession(user=ul_sess.user, description=desc)
        #sess_entry.save()
        #bulk_create_list = []
#
        #for e in json_data["events"]:
          #if e["type"] == "ROTATION_VECTOR":
            #ev = LogEvent(log_session=sess_entry,
                          #event_time=e["session_time"],
                          #event_type=e["type"],
                          #data0 = 0,
                          #data1 = float(e["quaternion"][0]),
                          #data2 = float(e["quaternion"][1]),
                          #data3 = float(e["quaternion"][2]),
                          #data4 = float(e["quaternion"][3]))
            #bulk_create_list.append(ev)
#
          #if e["type"] == "LOG_STOPPED":
            #ev = LogEvent(log_session=sess_entry,
                          #event_time=e["session_time"],
                          #event_type=e["type"],
                          #data0 = 0,
                          #data1 = 0,
                          #data2 = 0,
                          #data3 = 0,
                          #data4 = 0)
            #bulk_create_list.append(ev)

          #if e["type"] == "LOG_STARTED":
            #ev = LogEvent(log_session=sess_entry,
                          #event_time=e["session_time"],
                          #event_type=e["type"],
                          #data0 = 0,
                          #data1 = 0,
                          #data2 = 0,
                          #data3 = 0,
                          #data4 = 0)
            #bulk_create_list.append(ev)
#
          ## SQLite can only handle up to 999 items per bulk
          ## create
          #if len(bulk_create_list) >= 990:
            #LogEvent.objects.bulk_create(bulk_create_list)
            #bulk_create_list.clear()
#
        #if len(bulk_create_list) > 0:
          #LogEvent.objects.bulk_create(bulk_create_list)
          #bulk_create_list.clear()

      response = JsonResponse({'status': 'OK'})
      return response

    #create upload form
    if command == "token":
      pass

  return render(request, 'upload/form.html', context)

def register(request, model):
    print("request: register")
    print("model: " + str(model))
    new_user = User()
    new_user.model = model
    new_user.save()
    new_user.generate_signed_id()
    new_user.save()
    response = JsonResponse({'user_id': new_user.signed_id,
                             'status': 'OK'})
    return response
