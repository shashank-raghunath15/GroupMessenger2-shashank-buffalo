package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    static final List<String> portList = Arrays.asList("11108", "11112", "11116", "11120", "11124");
    public static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    private static int id = -1;
    private static String myPort;

    public static int getId() {
        return ++id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Error creating server task");
            return;
        }
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, myPort);
            }
        });
    }

    class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        private ArrayList<Message> messages = new ArrayList<Message>();
        private Map<Integer, Message> finalMessages = new TreeMap<Integer, Message>();
        private Map<String, List<Message>> acks = new HashMap<String, List<Message>>();
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            ServerSocket serverSocket = serverSockets[0];
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) objectInputStream.readObject();
                    if (message.getStatus().equals(MessageStatus.NEW)) {
                        message.setSequenceNumber(messages.size());
                        acks.put(message.getMessage(),new ArrayList<Message>());
                        messages.add(message);
                    }
                    if (message.getFromPort().equals(myPort)) {
                        messageFromMe(message);
                    } else {
                        messageFromOthers(message);
                    }


                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i).getReplyCount() == maxReplyCount() && !(messages.get(i).getStatus().equals(MessageStatus.DON))) {
                            sendFinalSequenceMessage(messages.get(i));
                        }
                    }

                    for (Integer i : finalMessages.keySet()) {
                        if (finalMessages.get(i).getStatus().equals(MessageStatus.DON)) {
                            System.out.println(finalMessages.get(i).getMessage());
                            System.out.println(finalMessages.get(i).getStatus());
                            System.out.println(finalMessages.get(i).getFinalSequenceNumber());
                            finalMessages.get(i).setStatus(MessageStatus.FIN);
                            publishProgress(finalMessages.get(i).getMessage());
                        }
                    }
                    objectInputStream.close();
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void sendFinalSequenceMessage(Message message) {
            try {
                for (String port : GroupMessengerActivity.portList) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.valueOf(port));
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    if (finalMessages.containsKey(message.getFinalSequenceNumber())) {
                        //Random random = new Random();
                        message.setFinalSequenceNumber(message.getFinalSequenceNumber() + 126);
                    }
                    message.setFromPort(myPort);
                    message.setStatus(MessageStatus.SEQ);
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }

        private void messageFromOthers(Message message) {
            if (message.getStatus().equals(MessageStatus.SEQ)) {
                for (Message m : messages) {
                    if (m.getMessage().equals(message.getMessage())) {
                        m.setStatus(MessageStatus.DON);
                    }
                }
                message.setStatus(MessageStatus.DON);
                if (!finalMessages.containsKey(message.getFinalSequenceNumber()))
                    finalMessages.put(message.getFinalSequenceNumber(), message);
            } else {
                if (message.getStatus().equals(MessageStatus.NEW)) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.valueOf(message.getSourcePort()));
                        message.setStatus(MessageStatus.ACK);
                        message.setFromPort(myPort);
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (message.getStatus().equals(MessageStatus.ACK)) {
                    acks.get(message.getMessage()).add(message);
                    if(acks.get(message.getMessage()).size() == maxReplyCount()) {
                        Message message1 = null;
                        int index = -1;
                        for(Message m: messages){
                            if(m.getMessage().contains(message.getMessage())){
                                message1 = m;
                            }
                            index++;
                        }
                        for(Message m: acks.get(message.getMessage())){
                            if(m.getSequenceNumber()>message1.getFinalSequenceNumber()){
                                message1.setFinalSequenceNumber(m.getSequenceNumber());
                            }
                            message1.setReplyCount(message1.getReplyCount() + 1);
                            messages.remove(index);
                            messages.add(index, message1);
                        }
                    }
                }

            }
        }

        private int maxReplyCount() {
            return 4;
        }

        private void messageFromMe(Message message) {
            if (message.getStatus().equals(MessageStatus.SEQ)) {
                for (Message m : messages) {
                    if (m.getMessage().equals(message.getMessage())) {
                        m.setStatus(MessageStatus.DON);
                    }
                }
                message.setStatus(MessageStatus.DON);
                if (!finalMessages.containsKey(message.getFinalSequenceNumber()))
                    finalMessages.put(message.getFinalSequenceNumber(), message);
            }
        }

        @Override
        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\n");
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", GroupMessengerActivity.getId());
            contentValues.put("value", strReceived);
            getContentResolver().insert(buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider"), contentValues);
            return;
        }

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
