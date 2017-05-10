from django.contrib import admin
from .models import User
from .models import UploadSession
from .models import LogSession
from .models import LogEvent
from .models import UploadChunk

class UserAdmin(admin.ModelAdmin):
  readonly_fields = ('registered_on',)
class UploadSessionAdmin(admin.ModelAdmin):
  readonly_fields = ('created_on',)

# Register your models here.
admin.site.register(User, UserAdmin)
admin.site.register(UploadSession, UploadSessionAdmin)
#admin.site.register(LogSession)
#admin.site.register(LogEvent)
#admin.site.register(UploadChunk)
