
/***
 *  @author Manmeet Singh
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
import com.assignment2.dos.smarterHomes.Network.OutletDeviceCommunicator;
import com.assignment2.dos.smarterHomes.Network.RegisterName;
import com.assignment2.dos.smarterHomes.Network.TemperatureSensorCommunicator;
import com.assignment2.dos.smarterHomes.Network.UpdateNames;
import com.assignment2.dos.smarterHomes.Network.UpdateStatus;
 
/**
 * TemperatureSensor class is based on push/pull model and extends from Sensor and communicates the temperature to the gateway.
 *
 *
 */
public class TemperatureSensor extends Sensors {

	ChatFrame chatFrame;
    Client client;
    String name;
    double temp;
    boolean isLeader;
    LogicalClock clock;
    BerkeleyClock berkeleyClock;
    
	public TemperatureSensor() {
		super();
	}

	/**
	 * Get the current temperature
	 */
	double getTemperature() {
		return temp;
	}
	

	/**
	 * Set the current temperature
	 */
	void setTemperature(double temp) {
		this.temp = temp;
	}
	

	/**
	 * Constructor with component id and name
	 */
	public TemperatureSensor(int id, String sensorName) {
		super(id, sensorName);
		temp = 0;
        isLeader = false;
		client = new Client();
        clock = new LogicalClock();

        berkeleyClock = new BerkeleyClock();
        berkeleyClock.addMilliseconds(-900);
        client.start();


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
                                clock.Compare(Double.parseDouble(updateNames.time));
                                chatFrame.setNames(updateNames.names);
                                return;
                        }

                        if (object instanceof ChatMessage) {
                                ChatMessage chatMessage = (ChatMessage)object;
                                chatFrame.addMessage(chatMessage.text);
                                clock.Compare(Double.parseDouble(chatMessage.time));
                                String message = chatMessage.text;
                                if (message == null)
                                	return;
                                String []tmp = message.split(":"); // check if its a leader election message
                                if (tmp.length== 2 && tmp[0].equalsIgnoreCase("New Leader"))
                                	if (tmp[1].equalsIgnoreCase("TemperatureSensor"))
                                		isLeader = true;
                                	else
                                		isLeader = false;
                                
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

                        if (object instanceof TemperatureSensorCommunicator) {
                        	TemperatureSensorCommunicator sensorCommunicator = (TemperatureSensorCommunicator)object;
                        	if (sensorCommunicator.text.equalsIgnoreCase("get-status") || sensorCommunicator.text.equalsIgnoreCase("status") || sensorCommunicator.text.equalsIgnoreCase("temperature-status")) {
                                clock.Compare(Double.parseDouble(sensorCommunicator.time));
                        		UpdateStatus status = new UpdateStatus();
                        		status.text = getTemperature() + "" ;
                        		status.name = "TemperatureSensor";
                                status.time = clock.GetStringTime();

                                client.sendTCP(status);
                            	chatFrame.addMessage("Sending temp:" + getTemperature() + " to gateway on request.");
                            	String log = CurrentTime.getCurrentTime() + " Temperature Sensor Gateway " + getTemperature();
                            	SmartHomesLogger logger = new SmartHomesLogger(log);
                                
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
        String input = (String)JOptionPane.showInputDialog(null, "Host:", "Connect to chat server", JOptionPane.QUESTION_MESSAGE,
                null, null, "localhost");
        if (input == null || input.trim().length() == 0) System.exit(1);
        final String host = input.trim();

        // Request the user's name.
        input = (String)JOptionPane.showInputDialog(null, "Name:", "Connect to chat server", JOptionPane.QUESTION_MESSAGE, null,
                null, "TemperatureSensor");
        if (input == null || input.trim().length() == 0) System.exit(1);
        name = input.trim();

        // All the ugly Swing stuff is hidden in ChatFrame so it doesn't clutter the KryoNet example code.
        chatFrame = new ChatFrame(host);
        // This listener is called when the send button is clicked.
        chatFrame.setSendListener(new Runnable() {
                public void run () {
                        TemperatureSensorCommunicator temperatureSensorCommunicator = new TemperatureSensorCommunicator();
                        temperatureSensorCommunicator.text = chatFrame.getSendText();
                        clock.Event();

                        Double tem = temp;
                        try {
                        	tem = Double.parseDouble(temperatureSensorCommunicator.text);
                        } catch (NumberFormatException nfe) {
                        	System.out.println("Invalid temperature, enter float or integer.");
                        	return;
                        };
                        temperatureSensorCommunicator.time = clock.GetStringTime();

                        String log = CurrentTime.getCurrentTime() + " Temperature Sensor/temperature " + tem;;
                    	SmartHomesLogger logger = new SmartHomesLogger(log);
                        
                        System.out.println("Setting current temperature to:" + tem);
                        setTemperature(tem);
                        
                        
                        //client.sendTCP(temperatureSensorCommunicator);

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
                            
                              //  ex.printStackTrace();
                                System.exit(1);
                        }
                }
        }.start();
	}
		
	
	
	void pushNotification(boolean movement) {
		
	}

        static private class ChatFrame extends JFrame {
                CardLayout cardLayout;
                JProgressBar progressBar;
                JList messageList;
                JTextField sendText;
                JButton sendButton;
                JList nameList;

                public ChatFrame (String host) {
                        super("Temperature Sensor");
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

        public static void main (String[] args) throws IOException {
                Log.set(Log.LEVEL_DEBUG);
                TemperatureSensor manager =  new TemperatureSensor(1, "temperature-sensor");
                if (args.length > 0 && args[0] != null) {
                        System.out.println("reading file:" + args[0] + "\n");
                        BufferedReader br = new BufferedReader(new FileReader(args[0]));
                        String line = null;
                        String firstLine = null;
                        String [] components = {"Motion", "Temperature", "Gateway", "Door", "Outlet", "Bulb", "Time"};
                        String []csvComponents = null;
                        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
                        while ((line = br.readLine()) != null) {
                                System.out.println("print line:" + line);
                                String []tmp = line.split(",");
                                if (firstLine == null) {
                                        firstLine = line;
                                        System.out.println(firstLine);
                                        csvComponents = tmp;
                                        continue;
                                }
                                for (int i = 0; i < tmp.length; i++) {
                                        String component = csvComponents[i];
                                        ArrayList<String> list = map.get(component);
                                        if (list == null)
                                                list = new ArrayList<String>();
                                        list.add(tmp[i]);
                                        map.put(component, list);
                                }

                        }
                        
                        System.out.println("file read complete!");
                        ArrayList<String> sensorEvents = map.get("Temperature");
                        ArrayList<String> time = map.get("Time");
                        try {
                            Thread.sleep(1000);                 //1000 milliseconds is one second.
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        if (sensorEvents == null)
                        	return;
                        for (int i = 0 ; i < sensorEvents.size(); i++) {
                                String event = sensorEvents.get(i).toLowerCase();
                                try {
                                    Thread.sleep(5000);                 //1000 milliseconds is one second.
                                } catch(InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                                try {
                                	manager.setTemperature(Double.parseDouble(sensorEvents.get(i)));
                                } catch(NumberFormatException nexp){
                                	System.out.println("Improper input to temperature");
                                }
                                // call the temperature sensor
                                System.out.println("Temperature Sensor sending status to Gateway");
                                SmartHomesLogger logger = new SmartHomesLogger("Temperature Sensor sending status to Gateway");


                        }
                }
        }
}