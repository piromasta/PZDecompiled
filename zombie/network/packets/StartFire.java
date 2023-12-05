package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketValidator;
import zombie.network.ServerOptions;
import zombie.network.packets.hit.Square;

public class StartFire implements INetworkPacket {
   protected final Square square = new Square();
   protected int fireEnergy;
   protected boolean ignite;
   protected int life;
   protected boolean smoke;
   protected int spreadDelay;
   protected int numParticles;

   public StartFire() {
   }

   public void set(IsoGridSquare var1, boolean var2, int var3, int var4, boolean var5) {
      this.square.set(var1);
      this.fireEnergy = var3;
      this.ignite = var2;
      this.life = var4;
      this.smoke = var5;
      this.spreadDelay = 0;
      this.numParticles = 0;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.square.parse(var1, var2);
      this.fireEnergy = var1.getInt();
      this.ignite = var1.get() == 1;
      this.life = var1.getInt();
      this.smoke = var1.get() == 1;
      if (GameClient.bClient) {
         this.spreadDelay = var1.getInt();
         this.numParticles = var1.getInt();
      }

   }

   public void write(ByteBufferWriter var1) {
      this.square.write(var1);
      var1.putInt(this.fireEnergy);
      var1.putBoolean(this.ignite);
      var1.putInt(this.life);
      var1.putBoolean(this.smoke);
      if (GameServer.bServer) {
         var1.putInt(this.spreadDelay);
         var1.putInt(this.numParticles);
      }

   }

   public void process() {
      IsoFire var1;
      if (GameServer.bServer) {
         var1 = this.smoke ? new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true) : new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
         IsoFireManager.Add(var1);
         this.spreadDelay = var1.getSpreadDelay();
         this.numParticles = var1.numFlameParticles;
         this.square.getSquare().getObjects().add(var1);
      }

      if (GameClient.bClient) {
         var1 = this.smoke ? new IsoFire(IsoWorld.instance.CurrentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true) : new IsoFire(IsoWorld.instance.CurrentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
         var1.SpreadDelay = this.spreadDelay;
         var1.numFlameParticles = this.numParticles;
         IsoFireManager.Add(var1);
         this.square.getSquare().getObjects().add(var1);
      }

   }

   public boolean isConsistent() {
      return this.square.getSquare() != null && this.life <= 500;
   }

   public boolean validate(UdpConnection var1) {
      if (GameServer.bServer && !this.smoke && ServerOptions.instance.NoFire.getValue()) {
         if (ServerOptions.instance.AntiCheatProtectionType16.getValue() && PacketValidator.checkUser(var1)) {
            PacketValidator.doKickUser(var1, this.getClass().getSimpleName(), "Type16", this.getDescription());
         }

         return false;
      } else if (!this.smoke && !IsoFire.CanAddFire(this.square.getSquare(), this.ignite, this.smoke)) {
         float var10000 = this.square.getX();
         DebugLog.log("not adding fire that on " + var10000 + "," + this.square.getY());
         if (ServerOptions.instance.AntiCheatProtectionType17.getValue() && PacketValidator.checkUser(var1)) {
            PacketValidator.doKickUser(var1, this.getClass().getSimpleName(), "Type17", this.getDescription());
         }

         return false;
      } else if (this.smoke && !IsoFire.CanAddSmoke(this.square.getSquare(), this.ignite)) {
         if (ServerOptions.instance.AntiCheatProtectionType18.getValue() && PacketValidator.checkUser(var1)) {
            PacketValidator.doKickUser(var1, this.getClass().getSimpleName(), "Type18", this.getDescription());
         }

         return false;
      } else {
         return GameClient.bClient || var1.RelevantTo(this.square.getX(), this.square.getY());
      }
   }

   public String getDescription() {
      String var1 = "\n\t" + this.getClass().getSimpleName() + " [";
      var1 = var1 + "square=" + this.square.getDescription() + " | ";
      var1 = var1 + "fireEnergy=" + this.fireEnergy + " | ";
      var1 = var1 + "ignite=" + this.ignite + " | ";
      var1 = var1 + "life=" + this.life + " | ";
      var1 = var1 + "smoke=" + this.smoke + " | ";
      var1 = var1 + "spreadDelay=" + this.spreadDelay + " | ";
      var1 = var1 + "numParticles=" + this.numParticles + "] ";
      return var1;
   }
}
