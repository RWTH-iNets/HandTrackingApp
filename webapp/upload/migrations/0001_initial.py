# -*- coding: utf-8 -*-
# Generated by Django 1.10.5 on 2017-02-17 14:49
from __future__ import unicode_literals

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='LogEvent',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('event_time', models.BigIntegerField()),
                ('event_type', models.IntegerField()),
                ('data0', models.IntegerField()),
                ('data1', models.FloatField()),
                ('data2', models.FloatField()),
                ('data3', models.FloatField()),
                ('data4', models.FloatField()),
            ],
        ),
        migrations.CreateModel(
            name='LogSession',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('description', models.CharField(max_length=2048)),
                ('timestamp', models.DateTimeField(auto_now_add=True)),
            ],
        ),
        migrations.CreateModel(
            name='UploadChunk',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('offset', models.BigIntegerField()),
                ('data', models.TextField()),
            ],
        ),
        migrations.CreateModel(
            name='UploadSession',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('signed_id', models.CharField(max_length=2048)),
            ],
        ),
        migrations.CreateModel(
            name='User',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('signed_id', models.CharField(max_length=2048)),
            ],
        ),
        migrations.AddField(
            model_name='uploadsession',
            name='user',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='upload.User'),
        ),
        migrations.AddField(
            model_name='uploadchunk',
            name='session',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='upload.UploadSession'),
        ),
        migrations.AddField(
            model_name='logsession',
            name='user',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='upload.User'),
        ),
        migrations.AddField(
            model_name='logevent',
            name='log_session',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='upload.LogSession'),
        ),
    ]
