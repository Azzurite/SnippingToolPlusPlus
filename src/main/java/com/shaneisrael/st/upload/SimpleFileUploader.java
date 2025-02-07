package com.shaneisrael.st.upload;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleFileUploader implements Runnable
{
    private final String uri;
    private final File image;
    private final String clientId;
    private final String userAgent;
    private final Map<String, String> fields;

    private UploadListener listener;
    private Thread self;

    public SimpleFileUploader(String uri, File image, String userAgent, String clientId)
    {
        this.uri = uri;
        this.image = image;
        this.userAgent = userAgent;
        this.clientId = clientId;
        fields = new LinkedHashMap<>();
    }

    public void uploadAsync(UploadListener listener)
    {
        this.listener = listener;
        self = new Thread(this, String.format("[%s] downloader", uri));
        self.start();
    }

    public void addField(String key, String value)
    {
        fields.put(key, value);
    }

    @Override
    public void run()
    {
        HTTPFileUploader fileUploader = new HTTPFileUploader(uri, userAgent);
        fileUploader.setClientId(clientId);
        fileUploader.setHeader("Authorization", "Client-ID " + clientId);
        fileUploader.setField("type", "file");
        for (Map.Entry<String, String> field : fields.entrySet())
        {
            fileUploader.setField(field.getKey(), field.getValue());
        }
        fileUploader.setFile("image", image);

        String serverResponse = "";
        try
        {
            fileUploader.startUpload();
            serverResponse = fileUploader.finish();
            listener.onUploadSuccess(serverResponse);
        } catch (UploadException ue)
        {
            listener.onUploadFail(ue.getHttpStatusCode(), ue.getMessage());
        } catch (IOException io)
        {
            listener.onUploadFail(-1, io.getMessage());
        } finally
        {
            try
            {
                self.join();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

    }
}
