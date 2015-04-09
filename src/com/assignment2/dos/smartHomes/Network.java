/* @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */
package com.assignment2.dos.smartHomes;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;

// This class is a convenient place to keep things common to both the client and server.
public class Network {
        static public final int port = 65320;

        // This registers objects that are going to be sent over the network.
        static public void register (EndPoint endPoint) {
                Kryo kryo = endPoint.getKryo();
                kryo.register(RegisterName.class);
                kryo.register(String[].class);
                kryo.register(UpdateNames.class);
                kryo.register(ChatMessage.class);
                kryo.register(OutletDeviceCommunicator.class);
                kryo.register(LightBulbDeviceCommunicator.class);
                kryo.register(MotionSensorCommunicator.class);
                kryo.register(TemperatureSensorCommunicator.class);
                kryo.register(UpdateStatus.class);
                kryo.register(DoorSensorCommunicator.class);
                kryo.register(DatabaseCommunicator.class);
                kryo.register(GatewayCommunicator.class);
        }

        static public class RegisterName {
                public String name;
                public String time;
        }

        static public class UpdateNames {
                public String[] names;
                public String time;
        }

        static public class ChatMessage {
                public String text;
                public String time;
        }
        
        static public class OutletDeviceCommunicator {
        		public String text;
                public String time;
        }
        
        static public class LightBulbDeviceCommunicator {
    		public String text;
                public String time;
        }
        
        static public class MotionSensorCommunicator {
    		public String text;
                public String time;
        }
        
        static public class DoorSensorCommunicator {
    		public String text;
                public String time;
        }
        
        static public class DatabaseCommunicator {
    		public String text;
                public String time;
        }
        
        static public class TemperatureSensorCommunicator {
    		public String text;
                public String time;
        }
        
        static public class GatewayCommunicator {
    		public String text;
                public String time;
    		public String component;
        }
        
        
        static public class UpdateStatus {
    		public String name;
    		public String text;
                public String time;
        }
}