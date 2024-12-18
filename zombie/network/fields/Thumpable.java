package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoThumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.pathfind.Path;
import zombie.util.Type;

public class Thumpable extends Square implements IPositional, INetworkPacketField {
   @JSONField
   protected int index;
   protected IsoObject isoObject;

   public Thumpable() {
   }

   public void set(IsoObject var1) {
      super.set(var1.getSquare());
      this.index = var1.getObjectIndex();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.index = var1.getInt();
      this.isoObject = (IsoObject)this.square.getObjects().get(this.index);
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      var1.putInt(this.index);
   }

   public void process(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)Type.tryCastTo(var1, IsoAnimal.class);
      IsoThumpable var3 = (IsoThumpable)Type.tryCastTo(this.isoObject, IsoThumpable.class);
      if (GameServer.bServer) {
         if (var3 != null) {
            var3.animalHit(var2);
            if ((float)var3.Health <= 0.0F) {
               var3.destroy();
               var2.thumpTarget = null;
            }
         }
      } else if (GameClient.bClient) {
         var2.thumpTarget = this.isoObject;
         var2.setPath2((Path)null);
      }

   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.isoObject != null;
   }

   public boolean isRelevant(UdpConnection var1) {
      return var1.RelevantTo(this.positionX, this.positionY);
   }

   public IsoObject getIsoObject() {
      return this.isoObject;
   }
}
