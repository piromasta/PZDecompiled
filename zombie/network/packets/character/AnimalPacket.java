package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.PFBData;
import zombie.network.fields.Vehicle;
import zombie.network.packets.INetworkPacket;

public class AnimalPacket implements INetworkPacket {
   public float realX;
   public float realY;
   public float realZ;
   public byte realDirection;
   public float x;
   public float y;
   public float z;
   public float direction;
   public short alertedID;
   public String type;
   public String breed;
   public String idle;
   public int stateVariables;
   public final PFBData pfbData = new PFBData();
   public float health;
   public Existences existanceType;
   public Vehicle vehicleID = new Vehicle();
   public byte hutchNestBox;
   public byte hutchPosition;

   public AnimalPacket() {
   }

   public void reset(Object var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public static enum Existences {
      isInWorld,
      isInHutch,
      isInVehicle,
      isItem;

      private Existences() {
      }
   }
}
