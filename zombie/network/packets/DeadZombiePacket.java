package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public class DeadZombiePacket extends DeadCharacterPacket implements INetworkPacket {
   private byte zombieFlags;
   private IsoZombie zombie;

   public DeadZombiePacket() {
   }

   public void set(IsoGameCharacter var1) {
      super.set(var1);
      this.zombie = (IsoZombie)var1;
      this.zombieFlags |= (byte)(this.zombie.isCrawling() ? 1 : 0);
   }

   public void process() {
      if (this.zombie != null) {
         this.zombie.setCrawler((this.zombieFlags & 1) != 0);
         super.process();
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      if (GameServer.bServer) {
         this.zombie = (IsoZombie)ServerMap.instance.ZombieMap.get(this.id);
      } else if (GameClient.bClient) {
         this.zombie = (IsoZombie)GameClient.IDToZombieMap.get(this.id);
      }

      if (this.zombie != null) {
         this.character = this.zombie;
         if (!GameServer.bServer || !this.zombie.isReanimatedPlayer()) {
            this.parseCharacterInventory(var1);
            this.parseCharacterHumanVisuals(var1);
         }

         this.character.setHealth(0.0F);
         this.character.getHitReactionNetworkAI().process(this.x, this.y, this.z, this.angle);
         this.character.getNetworkCharacterAI().setDeadBody(this);
      } else {
         IsoDeadBody var3 = this.getDeadBody();
         if (var3 != null) {
            this.parseDeadBodyInventory(var3, var1);
            this.parseDeadBodyHumanVisuals(var3, var1);
         }
      }

   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      this.writeCharacterInventory(var1);
      this.writeCharacterHumanVisuals(var1);
   }

   public String getDescription() {
      String var10000 = super.getDescription();
      return var10000 + String.format(" | isCrawling=%b", (this.zombieFlags & 1) != 0);
   }

   public IsoZombie getZombie() {
      return this.zombie;
   }
}
