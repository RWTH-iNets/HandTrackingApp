package de.rwth_aachen.inets.gollum;

import android.os.Build;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


/**
 * Created by julian on 2/16/17.
 */

public class HttpUploadHelper {

    String csrf_token = "";
    String user_id = "";

    String ep_new = "";
    String ep_chunk = "";
    String ep_done = "";
    String ep_register = "";

    LoggingServiceDBHelper db_helper = null;

    public HttpUploadHelper(LoggingServiceDBHelper db_helper, String ep_new, String ep_chunk,
                            String ep_done, String ep_register)
    {
        this.db_helper = db_helper;
        this.ep_new = ep_new;
        this.ep_register = ep_register;
        this.ep_chunk = ep_chunk;
        this.ep_done = ep_done;
    }

    public String start_upload_session()
    {
        this.get_user_id();
        String new_session_id = "";
        try {
            HttpURLConnection con = (HttpURLConnection) (
                    (new URL("http://" + this.ep_new + this.user_id + "/")).openConnection());
            con.setDoInput(true);
            con.setRequestMethod("GET");
            con.connect();

            String response = con.getResponseMessage();
            InputStreamReader reader = new InputStreamReader(con.getInputStream(), "ASCII");
            JsonReader json = new JsonReader(reader);

            if (con.getResponseCode() == 200) {
                json.beginObject();

                String status = "";
                String session_id = "";

                while(json.hasNext()) {
                    switch(json.nextName()) {
                        case "status": status = json.nextString(); break;
                        case "upload_session_id": session_id = json.nextString(); break;
                        default: json.skipValue(); break;
                    }
                }

                json.endObject();

                if(status.equals("OK")) {
                    new_session_id = session_id;
                }
            }

            json.close();
            reader.close();
            con.getInputStream().close();
            con.disconnect();

        }
        catch (Exception e) {
            Log.e("WARN", e.toString());
        }

        return new_session_id;
    }

    public int end_upload_session(String ul_session_id)
    {
        this.get_user_id();
        int retcode = -1;
        try {
            HttpURLConnection con = (HttpURLConnection) (
                    (new URL("http://"+this.ep_done + user_id + "/" + ul_session_id + "/"))
                            .openConnection());

            con.setDoInput(true);
            con.setRequestMethod("GET");
            con.connect();

            InputStreamReader reader = new InputStreamReader(con.getInputStream(), "ASCII");

            JsonReader response_reader = new JsonReader(reader);
            String response = con.getResponseMessage();

            if (response.equals("OK")) {
                response_reader.beginObject();
                response_reader.nextName();
                if(response_reader.nextString().equals("OK")) {
                    retcode = 0;
                }
                response_reader.endObject();
            }

            response_reader.close();
            reader.close();
            con.getInputStream().close();
            con.disconnect();
        }
        catch (Exception e) {
            Log.e("WARN", e.toString());
        }
        return retcode;
    }

    public int send_chunk(String ul_session_id, String chunk, long offset)
    {
        int retcode = -1;

        retcode = this.get_user_id();
        if(retcode < 0){
            return retcode;
        }

        retcode = this.get_csrf_token();
        if(retcode < 0){
            return retcode;
        }

        retcode = -1;

        try {
            byte[] data = chunk.getBytes("UTF-8");

            HttpURLConnection con = (HttpURLConnection) (
                    (new URL("http://"+ this.ep_chunk + user_id + "/" + ul_session_id + "/"
                            + offset + "/"))
                            .openConnection());

            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.addRequestProperty("X-CSRFToken", this.csrf_token);
            con.addRequestProperty("Cookie", "csrftoken=" + this.csrf_token);
            con.addRequestProperty("Content-Type", "application/json; charset=utf-8");
            //con.addRequestProperty("Content-Encoding", "gzip");
            con.setFixedLengthStreamingMode(data.length);

            con.connect();
            //GZIPOutputStream gzip_stream = new GZIPOutputStream(con.getOutputStream());

            //gzip_stream.write(data, 0, data.length);
            //gzip_stream.flush();
            con.getOutputStream().write(data);
            con.getOutputStream().flush();
            con.getOutputStream().close();

            con.getInputStream().read();
            con.getInputStream().close();

            if(con.getResponseCode() == 200) {
                retcode = 0;
            }

            con.disconnect();
        } catch (Exception e) {
            Log.e("WARN", e.toString());
        }

        return retcode;

    }

    public int send_chunk(String ul_session_id, byte[] data, int len, long offset)
    {
        int retcode = -1;

        retcode = this.get_user_id();
        if(retcode < 0){
            return retcode;
        }

        retcode = this.get_csrf_token();
        if(retcode < 0){
            return retcode;
        }

        retcode = -1;

        try {

            HttpURLConnection con = (HttpURLConnection) (
                    (new URL("http://"+ this.ep_chunk + user_id + "/" + ul_session_id + "/"
                            + offset + "/"))
                            .openConnection());

            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.addRequestProperty("X-CSRFToken", this.csrf_token);
            con.addRequestProperty("Cookie", "csrftoken=" + this.csrf_token);
            con.addRequestProperty("Content-Type", "application/json; charset=utf-8");
            //con.addRequestProperty("Content-Encoding", "gzip");
            con.setFixedLengthStreamingMode(len);

            con.connect();
            //GZIPOutputStream gzip_stream = new GZIPOutputStream(con.getOutputStream());

            //gzip_stream.write(data, 0, data.length);
            //gzip_stream.flush();
            con.getOutputStream().write(data, 0, len);
            con.getOutputStream().flush();
            con.getOutputStream().close();

            con.getInputStream().read();
            con.getInputStream().close();

            if(con.getResponseCode() == 200) {
                retcode = 0;
            }

            con.disconnect();
        } catch (Exception e) {
            Log.e("WARN", e.toString());
        }

        return retcode;

    }

    private int get_user_id()
    {
        int retcode = -1;

        if(this.db_helper.getUserId().equals("")) {
            try {
                String model = Build.MODEL;
                HttpURLConnection con = (HttpURLConnection) (
                        (new URL("http://" + this.ep_register + model + "/")).openConnection());
                con.setDoInput(true);
                con.setRequestMethod("GET");
                con.connect();

                InputStreamReader reader = new InputStreamReader(con.getInputStream(), "ASCII");
                JsonReader json = new JsonReader(reader);
                String response = con.getResponseMessage();

                if (con.getResponseCode() == 200) {

                    json.beginObject();

                    String status = "";
                    String my_user_id = "";

                    while(json.hasNext()) {
                        switch(json.nextName()) {
                            case "status": status = json.nextString(); break;
                            case "user_id": my_user_id = json.nextString(); break;
                            default: json.skipValue(); break;
                        }
                    }

                    json.endObject();

                    if(status.equals("OK")) {
                        this.user_id = my_user_id;
                        db_helper.addUserId(my_user_id);
                        retcode = 0;
                    }

                }

                json.close();
                reader.close();
                con.disconnect();

            } catch (Exception e) {
                Log.e("WARN", e.toString());
            }
        } else {

            this.user_id = this.db_helper.getUserId();
            retcode = 0;
        }

        return retcode;
    }

    private int get_csrf_token()
    {
        int retcode = -1;

        if(this.csrf_token.equals("")) {
            try {

                //get CSRF cookie
                HttpURLConnection con = (HttpURLConnection) (
                        (new URL("http://"+ this.ep_chunk))
                                .openConnection());
                con.setDoInput(true);
                con.setRequestMethod("GET");
                con.connect();

                con.getInputStream().read();
                String cookie = con.getHeaderField("Set-Cookie");
                Matcher matcher = Pattern.compile("(csrftoken=)(\\w*);").matcher(cookie);
                String csrf_token = "";

                while (matcher.find()) {
                    csrf_token = matcher.group(2);
                    this.csrf_token = csrf_token;
                }

                if(con.getResponseCode() == 200 && !csrf_token.equals("")){
                    retcode = 0;
                }

                con.getInputStream().close();
                con.disconnect();

            } catch (Exception e) {

            }
        } else {
            retcode = 0;
        }

        return retcode;
    }
}
