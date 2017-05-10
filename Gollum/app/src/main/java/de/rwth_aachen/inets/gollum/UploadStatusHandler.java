package de.rwth_aachen.inets.gollum;

/**
 * Created by julian on 2/15/17.
 */

public class UploadStatusHandler implements Runnable {

    IUploadProgress cb;
    int status_code;
    public UploadStatusHandler(IUploadProgress cb, int status_code)
    {
        this.cb = cb;
        this.status_code = status_code;
    }
    public void run() {
        if(this.status_code == 3)
            cb.onUploadComplete();
        if(this.status_code == 4)
            cb.onUploadError();
    }
}
