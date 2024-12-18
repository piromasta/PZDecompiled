package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.PersistentOutfits;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.ModelManager;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.ServerGUI;
import zombie.network.ServerMap;

public class Zombie extends CharacterField implements INetworkPacketField {
   @JSONField
   protected short zombieFlags;
   @JSONField
   protected String attackOutcome;
   @JSONField
   protected String attackPosition;
   protected IsoZombie zombie;

   public Zombie() {
   }

   public void set(IsoZombie var1, boolean var2) {
      super.set(var1);
      this.zombie = var1;
      this.zombieFlags = 0;
      this.zombieFlags = (short)(this.zombieFlags | (var1.isStaggerBack() ? 1 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.isFakeDead() ? 2 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.isBecomeCrawler() ? 4 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.isCrawling() ? 8 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.isKnifeDeath() ? 16 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.isJawStabAttach() ? 32 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var2 ? 64 : 0));
      this.zombieFlags = (short)(this.zombieFlags | (var1.getAttackDidDamage() ? 128 : 0));
      this.attackOutcome = var1.getAttackOutcome();
      this.attackPosition = var1.getPlayerAttackPosition();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.zombieFlags = var1.getShort();
      this.attackOutcome = GameWindow.ReadString(var1);
      this.attackPosition = GameWindow.ReadString(var1);
      if (GameServer.bServer) {
         this.zombie = (IsoZombie)ServerMap.instance.ZombieMap.get(this.getID());
         this.character = this.zombie;
      } else if (GameClient.bClient) {
         this.zombie = (IsoZombie)GameClient.IDToZombieMap.get(this.getID());
         this.character = this.zombie;
      }

   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
      var1.putShort(this.zombieFlags);
      var1.putUTF(this.attackOutcome);
      var1.putUTF(this.attackPosition);
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.zombie != null;
   }

   public void process() {
      super.process();
      this.zombie.setAttackOutcome(this.attackOutcome);
      this.zombie.setPlayerAttackPosition(this.attackPosition);
      this.zombie.setStaggerBack((this.zombieFlags & 1) != 0);
      this.zombie.setFakeDead((this.zombieFlags & 2) != 0);
      this.zombie.setBecomeCrawler((this.zombieFlags & 4) != 0);
      this.zombie.setCrawler((this.zombieFlags & 8) != 0);
      this.zombie.setKnifeDeath((this.zombieFlags & 16) != 0);
      this.zombie.setJawStabAttach((this.zombieFlags & 32) != 0);
      this.zombie.setAttackDidDamage((this.zombieFlags & 128) != 0);
   }

   public void react(HandWeapon var1) {
      if (this.zombie.isJawStabAttach()) {
         this.zombie.setAttachedItem("JawStab", var1);
      }

      if (GameServer.bServer && (this.zombieFlags & 64) != 0 && !PersistentOutfits.instance.isHatFallen(this.zombie)) {
         PersistentOutfits.instance.setFallenHat(this.zombie, true);
         if (ServerGUI.isCreated()) {
            PersistentOutfits.instance.removeFallenHat(this.zombie.getPersistentOutfitID(), this.zombie);
            ModelManager.instance.ResetNextFrame(this.zombie);
         }
      }

      this.react();
   }

   public IsoGameCharacter getCharacter() {
      return this.zombie;
   }

   public static class Flags {
      public static final short isStaggerBack = 1;
      public static final short isFakeDead = 2;
      public static final short isBecomeCrawler = 4;
      public static final short isCrawling = 8;
      public static final short isKnifeDeath = 16;
      public static final short isJawStabAttach = 32;
      public static final short isHelmetFall = 64;
      public static final short AttackDidDamage = 128;

      public Flags() {
      }
   }
}
