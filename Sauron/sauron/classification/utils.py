def is_active_usage(window):
    # TODO: what about incoming phone call while device is locked?
    return window['screen_on'] and not window['daydream_active'] and not window['device_locked']

def filter_windows(windows, active_usage=None, foreground_app=None, power_connected=None):
    for window in windows:
        # Check active usage
        if active_usage is not None and active_usage != is_active_usage(window):
            continue

        # Check if power is connected
        if power_connected is not None and power_connected != window['power_connected']:
            continue

        # Check foreground app
        if foreground_app is not None:
            if isinstance(foreground_app, str):
                if window['foreground_app'] != foreground_app:
                    continue
            elif isinstance(foreground_app, list) or isinstance(foreground_app, set) or isinstance(foreground_app, tuple):
                if window['foreground_app'] not in foreground_app:
                    continue

        # All checks passed, window suits the requirements
        yield window

