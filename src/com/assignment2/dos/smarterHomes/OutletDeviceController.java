/* @author Manmeet Singh
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
import com.assignment2.dos.smarterHomes.LogicalClock;
import com.assignment2.dos.smarterHomes.Network.ChatMessage;
import com.assignment2.dos.smarterHomes.Network.LightBulbDeviceCommunicator;
import com.assignment2.dos.smarterHomes.Network.MotionSensorCommunicator;
import com.assignment2.dos.smarterHomes.Network.OutletDeviceCommunicator;
import com.assignment2.dos.smarterHomes.Network.RegisterName;
import com.assignment2.dos.smarterHomes.Network.UpdateNames;


public class OutletDeviceController {
		Outlet outlet;
        ChatFrame chatFrame;
        Client client;
        String name;
        LogicalClock clock;
        boolean isLeader;

        public OutletDeviceController () {
        		outlet = new Outlet();
                client = new Client();
                client.start();
                isLeader = false;
                clock = new LogicalClock();

                // For consistency, the classes to be sent over the network are
                // registered by the same method for both the client and server.
                Network.register(client);

                client.addListener(new Listener() {
                        public void connected (Connection connection) {
                                clock.Event();
                                RegisterName registerName = new RegisterName();
                                registerName.name = name;
                                registerName.time = "0";//clock.GetStringTime();
                                client.sendTCP(registerName);
                        }

                        public void received (Connection connection, Object object) {

                                if (object instanceof UpdateNames) {
                                        UpdateNames updateNames = (UpdateNames)object;
                                        clock.Compare(Long.parseLong(updateNames.time));
                                        chatFrame.setNames(updateNames.names);
                                        return;
                                }

                                if (object instanceof ChatMessage) {
                                        ChatMessage chatMessage = (ChatMessage)object;
                                      
                                        clock.Compare(Long.parseLong(chatMessage.time));
                                        chatFrame.addMessage(chatMessage.text);
                                        String message = chatMessage.text;
                                        if (message == null)
                                        	return;
                                        String []tmp = message.split(":"); // check if its a leader election message
                                        if (tmp.length== 2 && tmp[0].equalsIgnoreCase("New Leader"))
                                        	if (tmp[1].equalsIgnoreCase("OutletDevice"))
                                        		isLeader = true;
                                        	else
                                        		isLeader = false;
                                        
                                        return;
                                }
                                
                                if (object instanceof OutletDeviceCommunicator) {
                                	OutletDeviceCommunicator deviceCommunicator = (OutletDeviceCommunicator)object;
                                        clock.Compare(Long.parseLong(deviceCommunicator.time));
                                	boolean statusSame = (deviceCommunicator.text.equalsIgnoreCase("turn-off") && outlet.getStatus().equals(State.OFF)) || 
                                			(deviceCommunicator.text.equalsIgnoreCase("turn-on") && outlet.getStatus().equals(State.ON));
                                	if (deviceCommunicator.text.equalsIgnoreCase("turn-off"))
                                		outlet.turnOff();
                                	else
                                		outlet.turnOn();
                                        clock.Event();
                                	System.out.println("Gateway sending the message of the Temperature Sensor");
                                	chatFrame.addMessage("Recieved message:" + deviceCommunicator.text + " from gateway.");
                                	if (!statusSame)
                                		chatFrame.addMessage("Turning device " + outlet.getStatus() + ".");
                                	else {
                                		chatFrame.addMessage("Device already " + outlet.getStatus());
                                	}
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
                String input = (String)JOptionPane.showInputDialog(null, "Host:", "Connect to Gateway Manager", JOptionPane.QUESTION_MESSAGE,
                        null, null, "localhost");
                if (input == null || input.trim().length() == 0) System.exit(1);
                final String host = input.trim();

                // Request the user's name.
                input = (String)JOptionPane.showInputDialog(null, "Name:", "Connect to Gateway Manager", JOptionPane.QUESTION_MESSAGE, null,
                        null, "Outlet");
                if (input == null || input.trim().length() == 0) System.exit(1);
                name = input.trim();

                // All the ugly Swing stuff is hidden in ChatFrame so it doesn't clutter the KryoNet example code.
                chatFrame = new ChatFrame(host);
                // This listener is called when the send button is clicked.
                chatFrame.setSendListener(new Runnable() {
                        public void run () {
                                clock.Event();
                        	 OutletDeviceCommunicator outletDeviceCommunicator = new OutletDeviceCommunicator();
                        	 outletDeviceCommunicator.text = chatFrame.getSendText();
                                outletDeviceCommunicator.time = clock.GetStringTime();
                             client.sendTCP(outletDeviceCommunicator);

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
                                        ex.printStackTrace();
                                        System.exit(1);
                                }
                        }
                }.start();
        }

        static private class ChatFrame extends JFrame {
                CardLayout cardLayout;
                JProgressBar progressBar;
                JList messageList;
                JTextField sendText;
                JButton sendButton;
                JList nameList;

                public ChatFrame (String host) {
                        super("Outlet Device");
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
                                                topPanel.add(new JScrollPane(messageList = new JList()));
                                                messageList.setModel(new DefaultListModel());
                                        }
                                        {
                                                topPanel.add(new JScrollPane(nameList = new JList()));
                                                nameList.setModel(new DefaultListModel());
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
                                        DefaultListModel model = (DefaultListModel)nameList.getModel();
                                        model.removeAllElements();
                                        for (String name : names)
                                                model.addElement(name);
                                }
                        });
                }

                public void addMessage (final String message) {
                        EventQueue.invokeLater(new Runnable() {
                                public void run () {
                                        DefaultListModel model = (DefaultListModel)messageList.getModel();
                                        model.addElement(message);
                                        messageList.ensureIndexIsVisible(model.size() - 1);
                                }
                        });
                }
        }

        public static void main (String[] args) {
                Log.set(Log.LEVEL_DEBUG);
                new OutletDeviceController();
        }
}