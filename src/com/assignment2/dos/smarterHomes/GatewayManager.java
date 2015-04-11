package com.assignment2.dos.smarterHomes;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.assignment2.dos.smarterHomes.CurrentTime;
import com.assignment2.dos.smarterHomes.Network.UpdateDataBase;
import com.assignment2.dos.smarterHomes.Node;
import com.assignment2.dos.smarterHomes.SmartHomesLogger;
import com.assignment2.dos.smarterHomes.Network.ChatMessage;
import com.assignment2.dos.smarterHomes.Network.DatabaseCommunicator;
import com.assignment2.dos.smarterHomes.Network.DoorSensorCommunicator;
import com.assignment2.dos.smarterHomes.Network.LightBulbDeviceCommunicator;
import com.assignment2.dos.smarterHomes.Network.MotionSensorCommunicator;
import com.assignment2.dos.smarterHomes.Network.OutletDeviceCommunicator;
import com.assignment2.dos.smarterHomes.Network.RegisterName;
import com.assignment2.dos.smarterHomes.Network.TemperatureSensorCommunicator;
import com.assignment2.dos.smarterHomes.Network.UpdateNames;
import com.assignment2.dos.smarterHomes.Network.UpdateStatus;

/**
 * GatewayManager (stateful server) handles the complete functionality of the Gateway. 
 * It's a stateful server and communicates with the devices controllers 
 * and the sensors. 
 * 
 */

public class GatewayManager {
        Server server; // acts as server
        BerkeleyClock berkeleyClock; // Berkeley Clock instance for clock synchronization
        final static Double TEMP_LOW = 1.0;
        final static Double TEMP_HIGH = 2.0;
        int count;
        boolean isHome;
        boolean doorStatus;
        boolean motionStatus;
        LogicalClock clock;
        HashMap<String, Connection> map; // stores the connection of the devices along with device-names
        HashSet<String> connectionsList; // Stateful server;//contains the complete connection which are active
        String leader;
        boolean isLeader;
        
        /**
         * 
         * Polls the Temperature Sensor after every threshold time
         */
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
        
        /**
         * 
         * Checks if motion detected and communicates to the LightBulb
         */

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
        				System.out.println("No motion for 3 minutes, turning light off!!!!");
        			}
        		}
        	};
        	timer.scheduleAtFixedRate(updates, new Date(), 5000);
        	
        }

        /**
         * 
         * @param component
         * @param status
         * method to communicate with DabaBaseHandler for updating the database
         */
        void updateDataBase(String component, String status) {
        	clock.Event();
			UpdateDataBase communicator = new UpdateDataBase();
			communicator.text = status;
            communicator.time = clock.GetStringTime();
            communicator.component = component;
            if (map.get("DataBase") == null) {
            	System.out.println("DataBase component not registered yet! Cant update db");
            	SmartHomesLogger logger = new SmartHomesLogger("DataBase component not registered yet! Cant update db");
            	return;
            }
        	server.sendToTCP(map.get("DataBase").getID(), communicator);
        	System.out.println("Gateway sending update to database");
        	SmartHomesLogger logger = new SmartHomesLogger("Gateway sending update to database");
		}
        /**
         * GatewayManager default constructor: Creates instance of GatewayManager
         */
        public GatewayManager () throws IOException {
        		map = new HashMap<String, Connection>();
        		connectionsList = new HashSet<String>();
        		connectionsList.add("GatewayManager");
        		leader = null;
        		isHome = true;
        		isLeader = false;
        		clock = new LogicalClock();
        		berkeleyClock = new BerkeleyClock();
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
                /**
                 * Listeners for the gateway
                 */
                server.addListener(new Listener() {
                        public void received (Connection c, Object object) {
                                // We know all connections for this server are actually ChatConnections.
                                ChatConnection connection = (ChatConnection)c;

                                if (object instanceof RegisterName) {
                                        // Ignore the object if a client has already registered a name. This is
                                        // impossible with our client, but a hacker could send messages at any time.
                                        if (connection.name != null) return;

                                        clock.Compare(Double.parseDouble(((RegisterName) object).time));
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
                                        if (connection.name.equalsIgnoreCase("Outlet")) {
                                        	updateDataBase("Outlet", "off");
                                        
                                        }
                                        if (connection.name.equalsIgnoreCase("LightBulb")) {
                                        	updateDataBase("LightBulb", "off");
                                        
                                        }
                                        connectionsList.add(connection.name);
                                        System.out.println("" + connection.name + " registered with server");
                                        // Send a "connected" message to everyone except the new client.
                                        ChatMessage chatMessage = new ChatMessage();
                                        chatMessage.text = name + " connected.";
                                        chatMessage.time = clock.GetStringTime();
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

                                        clock.Compare(Double.parseDouble(chatMessage.time));
                                        String message = chatMessage.text;
                                        if (message == null) return;
                                        message = message.trim();
                                        if (message.length() == 0) return;
                                       // Prepend the connection's name and send to everyone.
                                        chatMessage.text = connection.name + ": " + message;
                                        chatMessage.time = clock.GetStringTime();
                                        server.sendToAllTCP(chatMessage);
                                        return;
                                }
                                if (object instanceof MotionSensorCommunicator) {
                                    // Ignore the object if a client tries to chat before registering a name.
                                    if (connection.name == null) return;
                                    MotionSensorCommunicator motionSensorCommunicator = (MotionSensorCommunicator)object;

                                    clock.Compare(Double.parseDouble(motionSensorCommunicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = motionSensorCommunicator.text;
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the motion sensor!");
                                    // Prepend the connection's name and send to everyone.
                                    
                                    clock.Event();
                                    updateDataBase("MotionSensor", "motion-detected");
                                    DoorSensorCommunicator doorCommunicator = new DoorSensorCommunicator();
                                    doorCommunicator.text = "SYNC";
                                    doorCommunicator.time = clock.GetStringTime();
                                    if (map.get("DoorSensor") == null) {
                                    	System.out.println("Door Sensor not registered yet!");
                                    	SmartHomesLogger logger = new SmartHomesLogger("Door Sensor not registered yet!");
                                    	return;
                                    }
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
                                    else if(message.equalsIgnoreCase("false")) {
                                        motionStatus = false;
                                        return;
                                    }
                                    motionStatus = true;
                                    if(!isHome)
                                    {
                                    	//JOptionPane.showMessageDialog(null, "Someone is at home!");
                                    	System.out.println("ALERT!! Someone is at home!");
                                    	SmartHomesLogger logger = new SmartHomesLogger("ALERT!! Someone is at home!");
                                    	return;
                                    }
                                    if(doorStatus == false)
                                        return;
                                    resetCount();
                                    LightBulbDeviceCommunicator deviceCommunicator = new LightBulbDeviceCommunicator();
                                    deviceCommunicator.text = "turn-on";
                                    deviceCommunicator.time = clock.GetStringTime();
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

                                    clock.Compare(Double.parseDouble(doorSensorCommunicator.time));
                                    // Ignore the object if the chat message is invalid.
                                    String message = doorSensorCommunicator.text;
                                    updateDataBase("DoorSensor", message);
                                    if (message == null) return;
                                    message = message.trim();
                                    if (message.length() == 0) return;
                                    System.out.println("Recieved message from the door sensor!");

                                    clock.Event();
                                    MotionSensorCommunicator motionCommunicator = new MotionSensorCommunicator();
                                    motionCommunicator.text = "SYNC";
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
                                    else if(message.equalsIgnoreCase("false")){
                                        doorStatus = false;
                                        return;
                                    }
                                    doorStatus = true;
                                    if(!isHome)
                                    {
                                    	//JOptionPane.showMessageDialog(null, "Someone is at home!");
                                    	System.out.println("ALERT!! Someone is at home!");
                                    	
                                    	return;
                                    }
                                    if(!motionStatus)
                                        return;
                                    resetCount();
                                    LightBulbDeviceCommunicator deviceCommunicator = new LightBulbDeviceCommunicator();
                                    deviceCommunicator.text = "turn-off";
                                    if (map.get("LightBulb") == null) {
                                    	System.out.println("Light Bulb Device not running");
                                    	return;
                                    }
                                    String rMessage = "on";
                                		rMessage = "off";
                                    updateDataBase("LightBulb", rMessage);

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

                                    clock.Compare(Double.parseDouble(communicator.time));
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
                                    if (connection.name == null) return;
                                    DatabaseCommunicator communicator = (DatabaseCommunicator)object;

                                    clock.Compare(Double.parseDouble(communicator.time));
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

                                    clock.Compare(Double.parseDouble(status.time));
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
                                    	updateDataBase("TemperatureSensor", temp + "");
                                    	if (temp <= TEMP_LOW)
                                    		message = "turn-on";
                                    	else if (temp >= TEMP_HIGH)
                                    		message = "turn-off";
                                    	else
                                    		return;
                                    	String rMessage = "on";
                                    	if (message.equals("turn-off"))
                                    		rMessage = "off";
                                        updateDataBase("Outlet", rMessage);

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
                        /**
                         * Method used to communicate for Leader election
                         */
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
                            chatMessage.text = "New Leader:" + leader;
                            clock.Event();
                            chatMessage.time = clock.GetStringTime();
                            System.out.println(chatMessage.text);

                            server.sendToAllTCP(chatMessage);
                            
                            
						}

						private void resetCount() {
                        	count = 0;
						}
						
						/**
						 * method to handle when a  component leaves 
						 */
						public void disconnected (Connection c) {
                                ChatConnection connection = (ChatConnection)c;
                                if (connection.name != null) {
                                        clock.Event();
                                        // Announce to everyone that someone (with a registered name) has left.
                                        ChatMessage chatMessage = new ChatMessage();
                                        chatMessage.text = connection.name + " disconnected.";
                                        if (leader != null && leader.equals(connection.name)) {
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
                                System.exit(0);
                        }
                   /*     public void windowClosing (WindowEvent evt) {
                            server.stop();
                            System.exit(0);
                        }
                   */
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

        /**
         * Checks for the connection updates
         */
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

                GatewayManager manager = new GatewayManager();
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
                ArrayList<String> gatewayEvents = map.get("Gateway");
                ArrayList<String> time = map.get("Time");
                for (int i = 0 ; i < gatewayEvents.size(); i++) {
                	String event = gatewayEvents.get(i).toLowerCase();
                	for (int wait = 0; wait < 1000000000; wait++) {
                		; // wait for a moment
                	}
                	if (event.contains("q(temp)")) {
                		// call the temperature sensor
                		manager.clock.Event();
                        TemperatureSensorCommunicator communicator = new TemperatureSensorCommunicator();
                        communicator.text = "get-status";
                        communicator.time = manager.clock.GetStringTime();
                        if (manager.map.get("TemperatureSensor") == null) {
                        	System.out.println("Temperature Sensor not registerd");
                        } else {
                        	manager.server.sendToTCP(manager.map.get("TemperatureSensor").getID(), communicator);
                        	System.out.println("Gateway quering the temperature sensor");
                        	SmartHomesLogger logger = new SmartHomesLogger("Gateway quering the temperature sensor");

                        }	
                	}
                	if (event.contains("q(motion)")) {
                		// call the motion sensor
                        manager.clock.Event();
                        MotionSensorCommunicator motionCommunicator = new MotionSensorCommunicator();
                        motionCommunicator.text = "get-status";
                        motionCommunicator.time = manager.clock.GetStringTime();
                        if (manager.map.get("MotionSensor") == null) {
                        	System.out.println("Motion Sensor not registerd");
                        	SmartHomesLogger logger = new SmartHomesLogger("Motion Sensor not registerd");

                        	
                        } else {
                        	manager.server.sendToTCP(manager.map.get("MotionSensor").getID(), motionCommunicator);
                        	System.out.println("Gateway quering the motion sensor");
                        }
                	}
                	if (event.contains("q(door)")) {
                		// call the door sensor
                		manager.clock.Event();
                        DoorSensorCommunicator communicator = new DoorSensorCommunicator();
                        communicator.text = "get-status";
                        communicator.time = manager.clock.GetStringTime();
                        if (manager.map.get("DoorSensor") == null) {
                        	System.out.println("Door Sensor not registerd");
                        } else {
                        	manager.server.sendToTCP(manager.map.get("DoorSensor").getID(), communicator);
                        	System.out.println("Gateway quering the door sensor");
                        	SmartHomesLogger logger = new SmartHomesLogger("Gateway quering the door sensor");

                        }
                	}
                	if (event.contains("q(bulb)")) {
                		// call the light bulb device
                		manager.clock.Event();
                        LightBulbDeviceCommunicator communicator = new LightBulbDeviceCommunicator();
                        communicator.text = "get-status";
                        communicator.time = manager.clock.GetStringTime();
                        if (manager.map.get("LightBulb") == null) {
                        	System.out.println("LightBulb not registerd");
                        	SmartHomesLogger logger = new SmartHomesLogger("LightBulb not registered");

                        	return;
                        } else {
                        	manager.server.sendToTCP(manager.map.get("LightBulb").getID(), communicator);
                        	System.out.println("Gateway quering the light bulb");
                        	SmartHomesLogger logger = new SmartHomesLogger("Gateway quering the light bulb");

                        }
                	}
                	if (event.contains("q(outlet)")) {
                		// call the outletdevice
                		manager.clock.Event();
                       OutletDeviceCommunicator communicator = new OutletDeviceCommunicator();
                        communicator.text = "get-status";
                        communicator.time = manager.clock.GetStringTime();
                        if (manager.map.get("Outlet") == null) {
                        	System.out.println("Outlet not registerd");
                        } else {
                        	manager.server.sendToTCP(manager.map.get("Outlet").getID(), communicator);
                        	System.out.println("Gateway quering the outlet");
                        	SmartHomesLogger logger = new SmartHomesLogger("Gateway quering the outlet");
                        }
                	}
                }
                } 
        }
}