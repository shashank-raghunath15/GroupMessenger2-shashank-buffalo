package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by shash on 3/11/2018.
 */

public class ClientTask extends AsyncTask<String, Void, Void> {
    public static final String TAG = ClientTask.class.getSimpleName();

    @Override
    protected Void doInBackground(String... strings) {
        try {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.valueOf(strings[1]));
            Message message = new Message();
            message.setMessage(strings[0]);
            message.setSourcePort(strings[1]);
            message.setFromPort(strings[1]);
            message.setStatus(MessageStatus.NEW);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
            for (String port : GroupMessengerActivity.portList) {
                if(port.equals(strings[1])){
                    continue;
                }
                Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.valueOf(port));
                Message message2 = new Message();
                message2.setMessage(strings[0]);
                message2.setSourcePort(strings[1]);
                message2.setFromPort(strings[1]);
                message2.setStatus(MessageStatus.NEW);
                ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(socket1.getOutputStream());
                objectOutputStream2.writeObject(message2);
                objectOutputStream2.flush();
            }

        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException");
        }
        return null;
    }
}
