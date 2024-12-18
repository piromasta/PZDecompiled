package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoLivingCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.util.Type;

public class Weapon extends IDInteger implements INetworkPacketField {
   @JSONField
   protected HandWeapon weapon;

   public Weapon() {
   }

   public void set(HandWeapon var1) {
      super.setID(var1.getID());
      this.weapon = var1;
   }

   public void parse(ByteBuffer var1, UdpConnection var2, IsoLivingCharacter var3) {
      super.parse(var1, var2);
      if (var3.bareHands.getID() == this.getID()) {
         this.weapon = var3.bareHands;
      } else if (GameServer.bServer) {
         this.weapon = (HandWeapon)Type.tryCastTo(var3.getInventory().getItemWithID(this.ID), HandWeapon.class);
      } else if (GameClient.bClient) {
         if (var3.getPrimaryHandItem() != null) {
            if (var3.getPrimaryHandItem().getID() == this.getID()) {
               this.weapon = (HandWeapon)Type.tryCastTo(var3.getPrimaryHandItem(), HandWeapon.class);
            }
         } else {
            this.weapon = var3.bareHands;
         }
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      DebugLog.Multiplayer.error("Weapon.parse is not implemented");
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1);
   }

   public HandWeapon getWeapon() {
      return this.weapon;
   }
}
