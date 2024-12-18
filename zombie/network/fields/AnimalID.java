package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.popman.animal.AnimalInstanceManager;

public class AnimalID extends IDShort implements INetworkPacketField, IPositional {
   protected IsoAnimal animal;

   public AnimalID() {
   }

   public void set(IsoAnimal var1) {
      super.setID(var1.getOnlineID());
      this.animal = var1;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.animal = AnimalInstanceManager.getInstance().get(this.getID());
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
   }

   public IsoAnimal getAnimal() {
      return this.animal;
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.getAnimal() != null;
   }

   public String toString() {
      return this.animal == null ? "?" : this.animal.getAnimalType() + "(" + this.getID() + ")";
   }

   public void copy(AnimalID var1) {
      this.setID(var1.getID());
      this.animal = var1.animal;
   }

   public float getX() {
      return this.animal != null ? this.animal.getX() : 0.0F;
   }

   public float getY() {
      return this.animal != null ? this.animal.getY() : 0.0F;
   }

   public boolean isRelevant(UdpConnection var1) {
      return var1.RelevantTo(this.getX(), this.getX());
   }
}
