
/** @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */
package com.assignment2.dos.smarterHomes;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.assignment2.dos.smarterHomes.Network.BerkeleyTimeSync;
import com.assignment2.dos.smarterHomes.Network.ChatMessage;
import com.assignment2.dos.smarterHomes.Network.DatabaseCommunicator;
import com.assignment2.dos.smarterHomes.Network.GatewayCommunicator;
import com.assignment2.dos.smarterHomes.Network.RegisterName;
import com.assignment2.dos.smarterHomes.Network.UpdateDataBase;
import com.assignment2.dos.smarterHomes.Network.UpdateNames;

/**
 * DataBaseHandler acts a client for the gateway and communicates with the Database, thus acting as a bridge for the database and the gateway
 */
public class DatabaseHandler {
		Database db;
        ChatFrame chatFrame;
        Client client;
        String name;
        LogicalClock clock;
        BerkeleyClock berkeleyClock;

        /**
         * handleGatewayMessage method takes the message and the component as arguments and replies back the the response based on the
         * query. The message which is the query may be to get the history or the status for all or a particular component
         */
        private String handleGatewayMessage(String message, String component) {
            clock.Event();
        	HashMap<String, List<String>> ret = null;
        	if (message.equalsIgnoreCase("status")) {
        		switch(component) {
        		case "all":
        			ret = db.GetAll();
        		default:
        			ret = db.GetCurrentStatus(component);
        		}
        		
        	} else if (message.equalsIgnoreCase("history")) {
        		switch(component) {
        		case "all":
        			ret = db.GetAllHistory();
        		default:
        			ret = db.GetHistory(component);
        		}
    		}
        	
        	String response = generateResonse(ret);
        	
        	return response;
		}
        
        private String generateResonse(HashMap<String, List<String>> ret) {
			StringBuffer res = new StringBuffer();
			Iterator<Entry<String, List<String>>> it = ret.entrySet().iterator();
		    while (it.hasNext()) {
		        Entry<String, List<String>> pair = it.next();
		        res.append(pair.getKey() + " : ");
		        List<String> list = (List<String>)pair.getValue();
		        for (String s: list) {
		        	res.append(s + " ");
		        }
		        res.append("\n");
		    }
		    
		    return res.toString();
		}

        /**
         * Constructor for the DataBase-Handler class
         */
		public DatabaseHandler () {
        		db = new Database();
                client = new Client();
                client.start();
                clock = new LogicalClock();
                berkeleyClock = new BerkeleyClock();
                berkeleyClock.addMilliseconds(700);

                // For consistency, the classes to be sent over the network are
                // registered by the same method for both the client and server.
                Network.register(client);

                client.addListener(new Listener() {
                        public void connected (Connection connection) {
                                RegisterName registerName = new RegisterName();
                                registerName.name = "DataBase";
                                registerName.time = "0";// Start with logical clock 0;
                                client.sendTCP(registerName);
                        }

                        public void received (Connection connection, Object object) {
                                if (object instanceof UpdateNames) {

                                        UpdateNames updateNames = (UpdateNames)object;
                                        clock.Compare(Double.parseDouble(updateNames.time));
                                        chatFrame.setNames(updateNames.names);
                                        return;
                                }

                                if (object instanceof ChatMessage) {
                                        ChatMessage chatMessage = (ChatMessage)object;
                                        clock.Compare(Double.parseDouble(chatMessage.time));
                                        chatFrame.addMessage(chatMessage.text);
                                        return;
                                }
                                
                                if (object instanceof BerkeleyTimeSync) {
                                    BerkeleyTimeSync chatMessage = (BerkeleyTimeSync)object;
                                    clock.Compare(Double.parseDouble(chatMessage.time));
                                    String message = chatMessage.time;
                                    if (message == null)
                                    	return;
                                   long time = 0;
                                   try {
                                	   Double t = Double.parseDouble(message);
                                	   time = Math.round(t);
                                   } catch(Exception e) {
                                	   return;
                                   }
                                   berkeleyClock.setTime(time); 
                                   String _log = "Clock synchronized to:: " + berkeleyClock.toString(); 
                                   SmartHomesLogger logger = new SmartHomesLogger(_log);
                                   System.out.println(_log); 
                                   chatFrame.addMessage("Clock syncronized to :" + berkeleyClock.toString());

                                   return;
                            }

                                
                                if (object instanceof GatewayCommunicator) {
                                	GatewayCommunicator deviceCommunicator = (GatewayCommunicator)object;
                                	String message = deviceCommunicator.text;
                                	String component = deviceCommunicator.component;
                                        clock.Compare(Double.parseDouble(deviceCommunicator.time));
                                	String response = handleGatewayMessage(message, component);
                                	
                                	System.out.println("Gateway sending the message of the Database handler : " + message);
                                	chatFrame.addMessage("Recieved message:" + deviceCommunicator.text + " from gateway.");
                                	chatFrame.addMessage("Sending response back to the gateway");
                                	DatabaseCommunicator communicator = new DatabaseCommunicator();
                                	communicator.text = response;
                                    communicator.time = clock.GetStringTime();
                                	client.sendTCP(communicator);
                                	return;
                                }

                                if (object instanceof UpdateDataBase) {
                                	UpdateDataBase updateDataBase = (UpdateDataBase)object;
                                	String message = updateDataBase.text;
                                	String component = updateDataBase.component;
                                    clock.Compare(Double.parseDouble(updateDataBase.time));
                                	String time = clock.GetStringTime();
                                	db.PutStatus(component, message, time);
                                	return;
                                }
                        }

                        
						public void disconnected (Connection connection) {
                                EventQueue.invokeLater(new Runnable() {
                                        public void run () {
                                                // Closing the frame calls the close listener which will stop the client's update thread.
                                                chatFrame.dispose();
                                        }
                                });
                        }
                });

                // Request the host from the user.
                String input = (String)JOptionPane.showInputDialog(null, "Host:", "Connect to gateway", JOptionPane.QUESTION_MESSAGE,
                        null, null, "localhost");
                if (input == null || input.trim().length() == 0) System.exit(1);
                final String host = input.trim();

                // Request the user's name.
                input = (String)JOptionPane.showInputDialog(null, "Name:", "Connect to Gateway", JOptionPane.QUESTION_MESSAGE, null,
                        null, "DataBase");
                if (input == null || input.trim().length() == 0) System.exit(1);
                name = input.trim();

                // All the ugly Swing stuff is hidden in ChatFrame so it doesn't clutter the KryoNet example code.
                chatFrame = new ChatFrame(host);
                // This listener is called when the send button is clicked.
                chatFrame.setSendListener(new Runnable() {
                        public void run () {
                        	DatabaseCommunicator communicator = new DatabaseCommunicator();
                        	communicator.text = chatFrame.getSendText();
                                communicator.time = clock.GetStringTime();
                            client.sendTCP(communicator);
                        }
                });
                // This listener is called when the chat window is closed.
                chatFrame.setCloseListener(new Runnable() {
                        public void run () {
                                client.stop();
                        }
                });
                chatFrame.setVisible(true);

                // We'll do the connect on a new thread so the ChatFrame can show a progress bar.
                // Connecting to localhost is usually so fast you won't see the progress bar.
                new Thread("Connect") {
                        public void run () {
                                try {
                                        client.connect(5000, host, Network.port);
                                        // Server communication after connection can go here, or in Listener#connected().
                                } catch (IOException ex) {
                                    System.out.println("Unable to connect to the gateway");
                                		//ex.printStackTrace();
                                        System.exit(1);
                                }
                        }
                }.start();
        }

        static private class ChatFrame extends JFrame {
                CardLayout cardLayout;
                JProgressBar progressBar;
                JList<String> messageList;
                JTextField sendText;
                JButton sendButton;
                JList<String> nameList;

                public ChatFrame (String host) {
                        super("DataBase-Controller");
                        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        setSize(640, 200);
                        setLocationRelativeTo(null);

                        Container contentPane = getContentPane();
                        cardLayout = new CardLayout();
                        contentPane.setLayout(cardLayout);
                        {
                                JPanel panel = new JPanel(new BorderLayout());
                                contentPane.add(panel, "progress");
                                panel.add(new JLabel("Connecting to " + host + "..."));
                                {
                                        panel.add(progressBar = new JProgressBar(), BorderLayout.SOUTH);
                                        progressBar.setIndeterminate(true);
                                }
                        }
                        {
                                JPanel panel = new JPanel(new BorderLayout());
                                contentPane.add(panel, "chat");
                                {
                                        JPanel topPanel = new JPanel(new GridLayout(1, 2));
                                        panel.add(topPanel);
                                        {
                                                topPanel.add(new JScrollPane(messageList = new JList<String>()));
                                                messageList.setModel(new DefaultListModel<String>());
                                        }
                                        {
                                                topPanel.add(new JScrollPane(nameList = new JList<String>()));
                                                nameList.setModel(new DefaultListModel<String>());
                                        }
                                        DefaultListSelectionModel disableSelections = new DefaultListSelectionModel() {
                                                public void setSelectionInterval (int index0, int index1) {
                                                }
                                        };
                                        messageList.setSelectionModel(disableSelections);
                                        nameList.setSelectionModel(disableSelections);
                                }
                                {
                                        JPanel bottomPanel = new JPanel(new GridBagLayout());
                                        panel.add(bottomPanel, BorderLayout.SOUTH);
                                        bottomPanel.add(sendText = new JTextField(), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER,
                                                GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
                                        bottomPanel.add(sendButton = new JButton("Send"), new GridBagConstraints(1, 0, 1, 1, 0, 0,
                                                GridBagConstraints.CENTER, 0, new Insets(0, 0, 0, 0), 0, 0));
                                }
                        }

                        sendText.addActionListener(new ActionListener() {
                                public void actionPerformed (ActionEvent e) {
                                        sendButton.doClick();
                                }
                        });
                }

                public void setSendListener (final Runnable listener) {
                        sendButton.addActionListener(new ActionListener() {
                                public void actionPerformed (ActionEvent evt) {
                                        if (getSendText().length() == 0) return;
                                        listener.run();
                                        sendText.setText("");
                                        sendText.requestFocus();
                                }
                        });
                }

                public void setCloseListener (final Runnable listener) {
                        addWindowListener(new WindowAdapter() {
                                public void windowClosed (WindowEvent evt) {
                                        listener.run();
                                }
                                
                                public void windowClosing (WindowEvent evt) {
                                    dispose();
                                }
                                
                                public void windowActivated (WindowEvent evt) {
                                        sendText.requestFocus();
                                }
                        });
                }

                public String getSendText () {
                        return sendText.getText().trim();
                }

                public void setNames (final String[] names) {
                        // This listener is run on the client's update thread, which was started by client.start().
                        // We must be careful to only interact with Swing components on the Swing event thread.
                        EventQueue.invokeLater(new Runnable() {
                                public void run () {
                                        cardLayout.show(getContentPane(), "chat");
                                        DefaultListModel<String> model = (DefaultListModel<String>)nameList.getModel();
                                        model.removeAllElements();
                                        for (String name : names)
                                                model.addElement(name);
                                }
                        });
                }

                public void addMessage (final String message) {
                        EventQueue.invokeLater(new Runnable() {
                                public void run () {
                                        DefaultListModel<String> model = (DefaultListModel<String>)messageList.getModel();
                                        model.addElement(message);
                                        messageList.ensureIndexIsVisible(model.size() - 1);
                                }
                        });
                }
        }

        public static void main (String[] args) {
                Log.set(Log.LEVEL_DEBUG);
                new DatabaseHandler();
        }
}