package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.PacketValidator;
import zombie.network.packets.INetworkPacket;

public class PlayerHitPlayerPacket extends PlayerHitPacket implements INetworkPacket {
   protected final Player target = new Player();
   protected final WeaponHit hit = new WeaponHit();
   protected final Fall fall = new Fall();

   public PlayerHitPlayerPacket() {
      super(HitCharacterPacket.HitType.PlayerHitPlayer);
   }

   public void set(IsoPlayer var1, IsoPlayer var2, HandWeapon var3, float var4, boolean var5, float var6, boolean var7, boolean var8) {
      super.set(var1, var3, var7);
      this.target.set(var2, false);
      this.hit.set(var5, var4, var6, var2.getHitForce(), var2.getHitDir().x, var2.getHitDir().y, var8);
      this.fall.set(var2.getHitReactionNetworkAI());
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.target.parse(var1, var2);
      this.target.parsePlayer((UdpConnection)null);
      this.hit.parse(var1, var2);
      this.fall.parse(var1, var2);
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      this.target.write(var1);
      this.hit.write(var1);
      this.fall.write(var1);
   }

   public boolean isConsistent() {
      return super.isConsistent() && this.target.isConsistent() && this.hit.isConsistent();
   }

   public String getDescription() {
      String var10000 = super.getDescription();
      return var10000 + "\n\tTarget " + this.target.getDescription() + "\n\tHit " + this.hit.getDescription() + "\n\tFall " + this.fall.getDescription();
   }

   protected void preProcess() {
      super.preProcess();
      this.target.process();
   }

   protected void process() {
      this.hit.process(this.wielder.getCharacter(), this.target.getCharacter(), this.weapon.getWeapon());
      this.fall.process(this.target.getCharacter());
   }

   protected void postProcess() {
      super.postProcess();
      this.target.process();
   }

   public boolean validate(UdpConnection var1) {
      if (!PacketValidator.checkPVP(var1, this.wielder, this.target, PlayerHitPlayerPacket.class.getSimpleName())) {
         return false;
      } else if (!PacketValidator.checkLongDistance(var1, this.wielder, this.target, PlayerHitPlayerPacket.class.getSimpleName())) {
         return false;
      } else {
         return PacketValidator.checkDamage(var1, this.hit, PlayerHitPlayerPacket.class.getSimpleName());
      }
   }

   protected void attack() {
      this.wielder.attack(this.weapon.getWeapon(), true);
   }

   protected void react() {
      this.target.react();
   }
}
