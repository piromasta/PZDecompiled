package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;

public class Damage implements INetworkPacketField {
   protected static final float MAX_DAMAGE = 100.0F;
   @JSONField
   protected boolean ignore;
   @JSONField
   protected float damage;

   public Damage() {
   }

   public void set(boolean var1, float var2) {
      this.ignore = var1;
      this.damage = Math.min(var2, 100.0F);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.ignore = var1.get() != 0;
      this.damage = var1.getFloat();
   }

   public void write(ByteBufferWriter var1) {
      var1.putBoolean(this.ignore);
      var1.putFloat(this.damage);
   }

   public void processAnimal(IsoAnimal var1, IsoAnimal var2) {
      var1.atkTarget = var2;
      if (GameServer.bServer) {
         var2.HitByAnimal(var1, this.ignore);
      } else if (GameClient.bClient) {
         var2.setHitReaction("default");
      }

   }

   public void processPlayer(IsoAnimal var1, IsoPlayer var2) {
      var1.atkTarget = var2;
      if (GameServer.bServer) {
         var2.hitConsequences((HandWeapon)null, var1, this.ignore, this.damage, false);
      } else if (GameClient.bClient) {
         var2.setHitReaction("hitreact");
      }

   }

   public float getDamage() {
      return this.damage;
   }
}
