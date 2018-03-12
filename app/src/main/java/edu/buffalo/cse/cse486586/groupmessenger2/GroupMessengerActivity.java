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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
    static List<String> portList = new ArrayList<String>();

    public static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    private static int id = -1;
    private static String myPort;
    private int maxReplyCount = 5;
    private ArrayList<Message> messages = new ArrayList<Message>();
    private Map<Integer, Message> finalMessages = new TreeMap<Integer, Message>();
    private Map<String, List<Message>> acknowledgements = new HashMap<String, List<Message>>();
    //Map<String, ObjectOutputStream> writers = new HashMap<String, ObjectOutputStream>();

    public static int getId() {
        return ++id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        portList.add("11108");
        portList.add("11112");
        portList.add("11116");
        portList.add("11120");
        portList.add("11124");
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

        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            ServerSocket serverSocket = serverSockets[0];
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(1000);
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    Message message = Message.getMessageObject(dataInputStream.readUTF());
                    processMessage(socket, message);
                    if (message.getStatus().equals(MessageStatus.DON))
                        publishProgress(message.getMessage());
                    dataInputStream.close();
                    socket.close();
                }
            } catch (IOException e) {
                decrementMaxCount();
                Log.e(TAG, "Server IO");
            }
            return null;
        }

        private void processMessage(Socket socket, Message message) {
            if (message.getStatus().equals(MessageStatus.NEW)) {
                try {
                    processNewMessage(socket, message);
                } catch (IOException e) {
                    Log.e(TAG, "Process New IOException");
                }
            }
            if (message.getStatus().equals(MessageStatus.SEQ)) {
                processFinalSequence(message);
            }
        }

        private void processFinalSequence(Message message) {
            message.setStatus(MessageStatus.DON);
            finalMessages.put(message.getFinalSequenceNumber(), message);
        }

        private void processNewMessage(Socket socket, Message message) throws IOException {
            message.setSequenceNumber(messages.size());
            message.setSequenceOf(myPort);
            messages.add(message);
            message.setStatus(MessageStatus.ACK);
            writeMessage(socket, message);
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0];
            System.out.println(id + strReceived);
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

    class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {

            Iterator<String> ports = portList.iterator();
            while (ports.hasNext()) {
                String port = ports.next();
                try {

                    Socket socket = writeMessage2(port, buildNewMessage(strings));
                    Message message = readMessage(socket);
                    if (message.getStatus().equals(MessageStatus.ACK)) {
                        processAcknowledgementMessage(message);
                    }
                } catch (UnknownHostException e) {
                    decrementMaxCount();
                    portList.remove(port);
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    decrementMaxCount();
                    portList.remove(port);
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }


            return null;
        }

    }

    void decrementMaxCount() {
        if (maxReplyCount == 5) {
            maxReplyCount = 4;
        }
    }

    private int maxReplyCount() {
        return maxReplyCount;
    }

    private void processAcknowledgementMessage(Message message) {
        if (acknowledgements.containsKey(message.getMessage())) {
            acknowledgements.get(message.getMessage()).add(message);
            if (acknowledgements.get(message.getMessage()).size() == maxReplyCount()) {
                Message mFinal = getMessage(message.getMessage());
                for (Message m : acknowledgements.get(message.getMessage())) {
                    if (m.getSequenceNumber() > mFinal.getFinalSequenceNumber()) {
                        mFinal.setFinalSequenceNumber(m.getSequenceNumber());
                    }
                }
                if (finalMessages.containsKey(mFinal.getFinalSequenceNumber())) {
                    for (Message msg : acknowledgements.get(message.getMessage())) {
                        //System.out.println("Clashed ------->" + msg.toString2());
                        Random random = new Random();
                        if (msg.getSequenceOf().equals(portList.get(random.nextInt(portList.size() - 1)))) {
                            mFinal.setFinalSequenceNumber(msg.getSequenceNumber() + 111);
                        }
                    }
                }
                sendFinalSequenceMessage(mFinal);
            }
        } else {
            ArrayList<Message> messages = new ArrayList<Message>();
            messages.add(message);
            acknowledgements.put(message.getMessage(), messages);
        }
    }

    private Message getMessage(String message) {
        for (Message m : messages) {
            if (m.getMessage().contains(message)) {
                return m;
            }
        }
        return null;
    }

    private void sendFinalSequenceMessage(Message message) {
        try {
            Iterator<String> ports = portList.iterator();
            while (ports.hasNext()) {
                message.setStatus(MessageStatus.SEQ);
                writeMessage2(ports.next(), message);
            }
        } catch (UnknownHostException e) {
            decrementMaxCount();
            Log.e(TAG, "FinalSequence UnknownHostException");
        } catch (IOException e) {
            decrementMaxCount();
            Log.e(TAG, "FinalSequence IOException");
        }
    }

    void writeMessage(Socket socket, Message message) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(message.toString());
        dataOutputStream.flush();
    }

    Socket writeMessage2(String port, Message message) throws IOException {
        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                Integer.valueOf(port));
        socket.setSoTimeout(1000);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(message.toString());
        dataOutputStream.flush();
        return socket;
    }

    Message readMessage(Socket socket) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        Message message = Message.getMessageObject(dataInputStream.readUTF());
        return message;
    }

    Message buildNewMessage(String[] strings) {
        Message message = new Message();
        message.setStatus(MessageStatus.NEW);
        message.setSourcePort(strings[1]);
        message.setMessage(strings[0]);
        return message;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
