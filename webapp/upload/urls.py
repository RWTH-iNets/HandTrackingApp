from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^(?P<command>[\w]+)/(?P<uid>[\w.-]+)/$', views.upload, name='upload'),
    url(r'^(?P<command>[\w]+)/(?P<uid>[\w.-]+)/(?P<ul_session_id>[\w.-]+)/$', views.upload, name='upload'),

    url(r'^(?P<command>[\w]+)/(?P<uid>[\w.-]+)/(?P<ul_session_id>[\w.\-]+)/(?P<ul_chunk_offset>[0-9]+)/$', views.upload, name='upload'),

    url(r'^register/(?P<model>[\w-]+)/$', views.register, name='register'),
]

