package com.assignment2.dos.smartHomes;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.assignment2.dos.smartHomes.Node;
import com.assignment2.dos.smartHomes.CurrentTime;
import com.assignment2.dos.smartHomes.SmartHomesLogger;
import com.assignment2.dos.smartHomes.Network.ChatMessage;
import com.assignment2.dos.smartHomes.Network.DatabaseCommunicator;
import com.assignment2.dos.smartHomes.Network.DoorSensorCommunicator;
import com.assignment2.dos.smartHomes.Network.LightBulbDeviceCommunicator;
import com.assignment2.dos.smartHomes.Network.MotionSensorCommunicator;
import com.assignment2.dos.smartHomes.Network.OutletDeviceCommunicator;
import com.assignment2.dos.smartHomes.Network.RegisterName;
import com.assignment2.dos.smartHomes.Network.TemperatureSensorCommunicator;
import com.assignment2.dos.smartHomes.Network.UpdateNames;
import com.assignment2.dos.smartHomes.Network.UpdateStatus;

/*
 * GatewayManager handles the complete functionality of the Gateway. 
 * It's a stateful server and communicates with the devices controllers 
 * and the sensors. 
 * 
 */

public class GatewayManager {
        Server server;
        final static Double TEMP_LOW = 1.0;
        final static Double TEMP_HIGH = 2.0;
        int count;
        boolean isHome;
        boolean doorStatus;
        boolean motionStatus;
        LogicalClock clock;
        HashMap<String, Connection> map; // stores the connection of the devices along with device-names
        HashSet<String> connectionsList;
        String leader;
        
        void pollTemperatureSensor(Connection tempConnector) {
        	java.util.Timer timer = new java.util.Timer();
        	java.util.TimerTask updates = new java.util.TimerTask() {
        		@Override
        		public void run() {
                    clock.Event();
        			TemperatureSensorCommunicator communicator = new TemperatureSensorCommunicator();
        			communicator.text = "temperature-status";
                    communicator.time = clock.GetStringTime();
                	server.sendToTCP(map.get("TemperatureSensor").getID(), communicator);
                	System.out.println("Gateway quering the temperature sensor");
        		}
        	};
        	timer.scheduleAtFixedRate(updates, new Date(), 10000);
        	
        }
        
        void checkMotion(Connection connector) {

        	java.util.Timer timer = new java.util.Timer();
        	java.util.TimerTask updates = new java.util.TimerTask() {
        		@Override
        		public void run() {
        			count++;
        			if(count >= 60)
        			{
                        clock.Event();
        				count = 0;
        				LightBulbDeviceCommunicator communicator = new LightBulbDeviceCommunicator();
        				communicator.text = "turn-off";
        				if (map.get("LightBulb") == null)
        					return;
                        communicator.time = clock.GetStringTime();
        				server.sendToTCP(map.get("LightBulb").getID(), communicator);
        				System.out.println("Gateway messaging the light-bulb to be turned off!!!!");
        			}
        		}
        	};
        	timer.scheduleAtFixedRate(updates, new Date(), 5000);
        	
        }
        
        public GatewayManager () throws IOException {
        		map = new HashMap<String, Connection>();
        		connectionsList = new HashSet<String>();
        		connectionsList.add("GatewayManager");
        		leader = null;
        		isHome = true;
            clock = new LogicalClock();
                server = new Server() {
                        protected Connection newConnection () {
                                // By providing our own connection implementation, we can store per
                                // connection state without a connection ID to state look up.
                                return new ChatConnection();
                        }
                };

                
                // For consistency, the classes to be sent over the network are
                // registered by the same method for both the client and server.
                Network.register(server);
                
                server.addListener(new Listener() {
                        public void received (Connection c, Object object) {
                                // We know all connections for this server are actually ChatConnections.
                                ChatConnection connection = (ChatConnection)c;

                                if (object instanceof RegisterName) {
                                        // Ignore the object if a client has already registered a name. This is
                                        // impossible with our client, but a hacker could send messages at any time.
                                        if (connection.name != null) return;

                                    clock.Compare(Long.parseLong(((RegisterName)object).time));
                                        // Ignore the object if the name is invalid.
                                        String name = ((RegisterName)object).name;
                                        if (name == null) return;
                                        name = name.trim();
                                        if (name.length() == 0) return;
                                        // Store the name on the connection.
                                        connection.name = name;
                                        map.put(connection.name, connection);
                                        if (connection.name.equalsIgnoreCase("TemperatureSensor")) {
                                        	pollTemperatureSensor(connection);
                                        	
                                        }
                                        if (connection.name.equalsIgnoreCase("MotionSensor")) {
                                        	checkMotion(connection);
                                        }
                                        connectionsList.add(connection.name);
                                        System.out.println("" + connection.name + " registered with server");
                                        // Send a "connected" message to everyone except the new client.
                                        ChatMessage chatMessage = new ChatMessage();
                                        chatMessage.text = name + " connected.";
                                        server.sendToAllExceptTCP(connection.getID(), chatMessage);
                                        // Send everyone a new list of connection names.
                                        updateNames();
                                        if (connectionsList.size() >= 5) {
                                        	electLeader();
                                        }
                                        
                                        String log = CurrentTime.getCurrentTime() + " Gateway " + connection.name + " registered";
                                    	SmartHomesLogger logger = new SmartHomesLogger(log);
                                        return;
                                }

                                if (object instanceof ChatMessage) {
                                        // Ignore the object if a client tries to chat before registering a name.
                                        if (connection.name == null) return;
                                        ChatMessage chatMessage = (ChatMessage)object;
                                        // Ignore the object if the chat message is invalid.

                                        clock.Compare(Long.parseLong(chatMessage.time));
                                        String message = chatMessage.text;
                                        if (message == null) return;
                                        message = message.trim();
                                        if (message.length() == 0) return;
                                       // Prepend the connection's name and send to everyone.
                                        chatMessage.text = connection.name + ": " + message;
                                        server.sendToAllTCP(chatMessage);
                                        return;
                                }
                                if (object instanceof MotionSensorCommunicator) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    MotionSensorCommunicator motionSensorCommunicator = (MotionSensorCommunicator)object;

                                    clock.Compare(Long.parseLong(motionSensorCommunicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = motionSensorCommunicator.text;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the motion sensor!");
                                    // Prepend the connection's name and send to everyone.

                                    clock.Event();
                                    DoorSensorCommunicator doorCommunicator = new DoorSensorCommunicator();
                                    doorCommunicator.text = "get-status";
                                    doorCommunicator.time = clock.GetStringTime();
                                    server.sendToTCP(map.get("DoorSensor").getID(), doorCommunicator);
                                    System.out.println("Gateway quering the door sensor");
                                    
                                    if(message.equalsIgnoreCase("HOME"))
                                    {
                                    	isHome = true;
                                    	return;
                                    }
                                    else if(message.equalsIgnoreCase("AWAY"))
                                    {
                                    	isHome = false;
                                    	return;
                                    }
                                    if(!isHome)
                                    {
                                    	//JOptionPane.showMessageDialog(null, "Someone is at home!");
                                    	System.out.println("ALERT!! Someone is at home!");
                                    	return;
                                    }
                                    resetCount();
                                    LightBulbDeviceCommunicator deviceCommunicator = new LightBulbDeviceCommunicator();
                                    deviceCommunicator.text = "turn-on";
                                    if (map.get("LightBulb") == null) {
                                    	System.out.println("Light Bulb Device not running");
                                    	return;
                                    }
                                    String log = CurrentTime.getCurrentTime() + " Gateway/motion 1";
                                	SmartHomesLogger logger = new SmartHomesLogger(log);
                                    server.sendToTCP(map.get("LightBulb").getID(), deviceCommunicator);
                                    //String log = CurrentTime.getCurrentTime() + " Gateway " + "LightBulb " + deviceCommunicator.text;
                                	//SmartHomesLogger logger = new SmartHomesLogger(log);
                                    return;
                                }
                                if (object instanceof DoorSensorCommunicator) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    DoorSensorCommunicator doorSensorCommunicator = (DoorSensorCommunicator)object;

                                    clock.Compare(Long.parseLong(doorSensorCommunicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = doorSensorCommunicator.text;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the door sensor!");

                                    clock.Event();
                                    MotionSensorCommunicator motionCommunicator = new MotionSensorCommunicator();
                                    motionCommunicator.text = "get-status";
                                    motionCommunicator.time = clock.GetStringTime();
                                    server.sendToTCP(map.get("MotionSensor").getID(), motionCommunicator);
                                    System.out.println("Gateway quering the motion sensor");
                                    
                                    if(message.equalsIgnoreCase("DoorOpened"))
                                    {
                                    	isHome = true;
                                    	return;
                                    }
                                    else if(message.equalsIgnoreCase("AWAY"))
                                    {
                                    	isHome = false;
                                    	return;
                                    }
                                    if(!isHome)
                                    {
                                    	//JOptionPane.showMessageDialog(null, "Someone is at home!");
                                    	System.out.println("ALERT!! Someone is at home!");
                                    	return;
                                    }
                                    resetCount();
                                    LightBulbDeviceCommunicator deviceCommunicator = new LightBulbDeviceCommunicator();
                                    deviceCommunicator.text = "turn-on";
                                    if (map.get("LightBulb") == null) {
                                    	System.out.println("Light Bulb Device not running");
                                    	return;
                                    }
                                    String log = CurrentTime.getCurrentTime() + " Gateway/motion 1";
                                	SmartHomesLogger logger = new SmartHomesLogger(log);
                                    deviceCommunicator.time = clock.GetStringTime();
                                    server.sendToTCP(map.get("LightBulb").getID(), deviceCommunicator);
                                    //String log = CurrentTime.getCurrentTime() + " Gateway " + "LightBulb " + deviceCommunicator.text;
                                	//SmartHomesLogger logger = new SmartHomesLogger(log);
                                    return;
                                }
                                
                                if (object instanceof OutletDeviceCommunicator) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    OutletDeviceCommunicator communicator = (OutletDeviceCommunicator)object;

                                    clock.Compare(Long.parseLong(communicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = communicator.text;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the Outlet Device!");
                                    // Prepend the connection's name and send to everyone.
                                    MotionSensorCommunicator deviceCommunicator = new MotionSensorCommunicator();
                                    deviceCommunicator.text = message;
                                    deviceCommunicator.time = clock.GetStringTime();
                                 //   server.sendToAllTCP(deviceCommunicator);
                                    server.sendToTCP(map.get("TemperatureSensor").getID(), deviceCommunicator);
                                    String log = CurrentTime.getCurrentTime() + " Gateway " + "Outlet " + message;
                                	SmartHomesLogger logger = new SmartHomesLogger(log);
                                    return;
                                }
                                if (object instanceof DatabaseCommunicator) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    DatabaseCommunicator communicator = (DatabaseCommunicator)object;

                                    clock.Compare(Long.parseLong(communicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = communicator.text;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the Database communicator!");
                                    System.out.println(message);
                                    String log = CurrentTime.getCurrentTime() + " Gateway " + "Database " + message;
                                	SmartHomesLogger logger = new SmartHomesLogger(log);
                                    return;
                                }

                                if (object instanceof UpdateStatus) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    UpdateStatus status = (UpdateStatus)object;

                                    clock.Compare(Long.parseLong(status.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = status.text;
                                    String name = status.name;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message:" + message + " from:" + name + " at " + CurrentTime.getCurrentTime());
                                    // Prepend the connection's name and send to everyone.
                                    if (name != null && name.equalsIgnoreCase("TemperatureSensor")) {
                                    	Double temp = Double.parseDouble(message);
                                    	if (temp < 1.0)
                                    		message = "turn-on";
                                    	else if (temp > 2.0)
                                    		message = "turn-off";
                                    	else
                                    		return;
                                    	OutletDeviceCommunicator deviceCommunicator = new OutletDeviceCommunicator();
                                        deviceCommunicator.text = message;
                                        System.out.println("After recieving update from Temperature Sensor, sending message to outlet.");
                                        if (map.get("Outlet") == null) {
                                        	System.out.println("Outlet not running");
                                        	return;
                                        }
                                        deviceCommunicator.time = clock.GetStringTime();
                                        server.sendToTCP(map.get("Outlet").getID(), deviceCommunicator);
                                        String log = CurrentTime.getCurrentTime() + " Gateway/temperature " + temp;
                                    	SmartHomesLogger logger = new SmartHomesLogger(log);
                                    }
                                    return;
                                }
                                
                               
                        }

                        private void electLeader() {
                        	 InetAddress IP = null;
							try {
								IP = InetAddress.getLocalHost();
							} catch (UnknownHostException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
                         
							String ipAddress = null;
                            if (IP != null)
                            	ipAddress = IP.getHostAddress();
                            try {
								Node node = new Node("leader-election");
								leader = node.getLeader(connectionsList);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                            ChatMessage chatMessage = new ChatMessage();
                            chatMessage.text = "New Leader: " + leader;
                            System.out.println(chatMessage.text);

                            server.sendToAllTCP(chatMessage);
                            
                            
						}

						private void resetCount() {
                        	count = 0;
						}

						public void disconnected (Connection c) {
                                ChatConnection connection = (ChatConnection)c;
                                if (connection.name != null) {
                                        clock.Event();
                                        // Announce to everyone that someone (with a registered name) has left.
                                        ChatMessage chatMessage = new ChatMessage();
                                        chatMessage.text = connection.name + " disconnected.";
                                        if (leader.equals(connection.name)) {
                                        	System.out.println("Leader disconnected!");
                                        	System.out.println("Re-electing leader");
                                        }
                                        connectionsList.remove(connection.name);
                                        
                                        chatMessage.time = clock.GetStringTime();
                                        server.sendToAllTCP(chatMessage);
                                        electLeader();
                                        updateNames();
                                }
                        }
                });
                server.bind(Network.port);
                server.start();

                // Open a window to provide an easy way to stop the server.
                JFrame frame = new JFrame("GATEWAY MANAGER");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                        public void windowClosed (WindowEvent evt) {
                                server.stop();
                        }
                });
              //  frame.getContentPane().add(new JLabel("Gateway Manager running!"));
              //  frame.setIconImage(new ImageIcon("images/server_room.jpg"));
              //  String path = "C:\\Users\\msingh\\workspace\\SmartHomes\\src\\com\\assignment1\\dos\\smartHomes\\server_room.jpg";

                String path = System.getProperty("user.dir") + "/server_room.jpg";
                File file = new File(path);
                BufferedImage image = null;
                try {
                	image = ImageIO.read(file);
                } catch (Exception e) {};
                if (image != null)
                	frame.getContentPane().add(new JLabel(new ImageIcon(image)));
                frame.setSize(600, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                System.out.println(CurrentTime.getCurrentTime() + " Server Running!");


        }

        void updateNames () {
                // Collect the names for each connection.
                clock.Event();
                Connection[] connections = server.getConnections();
                ArrayList names = new ArrayList(connections.length);
                for (int i = connections.length - 1; i >= 0; i--) {
                        ChatConnection connection = (ChatConnection)connections[i];
                        names.add(connection.name);
                }
                // Send the names to everyone.
                UpdateNames updateNames = new UpdateNames();
                updateNames.names = (String[])names.toArray(new String[names.size()]);
                updateNames.time = clock.GetStringTime();
                server.sendToAllTCP(updateNames);
        }

        // This holds per connection state.
        static class ChatConnection extends Connection {
                public String name;
        }

        public static void main (String[] args) throws IOException {
                Log.set(Log.LEVEL_DEBUG);
                new GatewayManager();
        }
}