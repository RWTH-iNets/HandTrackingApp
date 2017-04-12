package de.rwth_aachen.inets.gollum;

/**
 * Created by julian on 2/15/17.
 */

public interface IUploadProgress {
    void onProgressUpdate();
    void onUploadComplete();
    void onUploadError();
    void dispatchProgressUpdate();
    void dispatchUploadComplete();
    void dispatchUploadError();
}
