package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.characters.IsoLivingCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.network.JSONField;

public class AttackVars implements INetworkPacketField {
   @JSONField
   private boolean isBareHeadsWeapon;
   @JSONField
   public boolean bAimAtFloor;
   @JSONField
   public boolean bCloseKill;
   @JSONField
   public boolean bDoShove;
   @JSONField
   public boolean bDoGrapple;
   @JSONField
   public float useChargeDelta;
   @JSONField
   public int recoilDelay;
   @JSONField
   public final ArrayList<HitInfo> targetsStanding = new ArrayList();
   @JSONField
   public final ArrayList<HitInfo> targetsProne = new ArrayList();
   public MovingObject targetOnGround = new MovingObject();
   public boolean isProcessed = false;

   public AttackVars() {
   }

   public void setWeapon(HandWeapon var1) {
      this.isBareHeadsWeapon = "BareHands".equals(var1.getType());
   }

   public HandWeapon getWeapon(IsoLivingCharacter var1) {
      return !this.isBareHeadsWeapon && var1.getUseHandWeapon() != null ? var1.getUseHandWeapon() : var1.bareHands;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      byte var3 = var1.get();
      this.isBareHeadsWeapon = AttackVars.AttackFlags.isFlagSet(var3, AttackVars.AttackFlags.isBareHeadsWeapon);
      this.bAimAtFloor = AttackVars.AttackFlags.isFlagSet(var3, AttackVars.AttackFlags.bAimAtFloor);
      this.bCloseKill = AttackVars.AttackFlags.isFlagSet(var3, AttackVars.AttackFlags.bCloseKill);
      this.bDoShove = AttackVars.AttackFlags.isFlagSet(var3, AttackVars.AttackFlags.bDoShove);
      this.bDoGrapple = AttackVars.AttackFlags.isFlagSet(var3, AttackVars.AttackFlags.bDoGrapple);
      this.targetOnGround.parse(var1, var2);
      this.useChargeDelta = var1.getFloat();
      this.recoilDelay = var1.getInt();
      byte var4 = var1.get();
      this.targetsStanding.clear();

      int var5;
      HitInfo var6;
      for(var5 = 0; var5 < var4; ++var5) {
         var6 = new HitInfo();
         var6.parse(var1, var2);
         this.targetsStanding.add(var6);
      }

      var4 = var1.get();
      this.targetsProne.clear();

      for(var5 = 0; var5 < var4; ++var5) {
         var6 = new HitInfo();
         var6.parse(var1, var2);
         this.targetsProne.add(var6);
      }

   }

   public void write(ByteBufferWriter var1) {
      byte var2 = 0;
      byte var6 = AttackVars.AttackFlags.setFlagState(var2, AttackVars.AttackFlags.isBareHeadsWeapon, this.isBareHeadsWeapon);
      var6 = AttackVars.AttackFlags.setFlagState(var6, AttackVars.AttackFlags.bAimAtFloor, this.bAimAtFloor);
      var6 = AttackVars.AttackFlags.setFlagState(var6, AttackVars.AttackFlags.bCloseKill, this.bCloseKill);
      var6 = AttackVars.AttackFlags.setFlagState(var6, AttackVars.AttackFlags.bDoShove, this.bDoShove);
      var6 = AttackVars.AttackFlags.setFlagState(var6, AttackVars.AttackFlags.bDoGrapple, this.bDoGrapple);
      var1.putByte(var6);
      this.targetOnGround.write(var1);
      var1.putFloat(this.useChargeDelta);
      var1.putInt(this.recoilDelay);
      byte var3 = (byte)Math.min(100, this.targetsStanding.size());
      var1.putByte(var3);

      int var4;
      HitInfo var5;
      for(var4 = 0; var4 < var3; ++var4) {
         var5 = (HitInfo)this.targetsStanding.get(var4);
         var5.write(var1);
      }

      var3 = (byte)Math.min(100, this.targetsProne.size());
      var1.putByte(var3);

      for(var4 = 0; var4 < var3; ++var4) {
         var5 = (HitInfo)this.targetsProne.get(var4);
         var5.write(var1);
      }

   }

   public int getPacketSizeBytes() {
      int var1 = 11 + this.targetOnGround.getPacketSizeBytes();
      byte var2 = (byte)Math.min(100, this.targetsStanding.size());

      int var3;
      HitInfo var4;
      for(var3 = 0; var3 < var2; ++var3) {
         var4 = (HitInfo)this.targetsStanding.get(var3);
         var1 += var4.getPacketSizeBytes();
      }

      var2 = (byte)Math.min(100, this.targetsProne.size());

      for(var3 = 0; var3 < var2; ++var3) {
         var4 = (HitInfo)this.targetsProne.get(var3);
         var1 += var4.getPacketSizeBytes();
      }

      return var1;
   }

   public void copy(AttackVars var1) {
      this.isBareHeadsWeapon = var1.isBareHeadsWeapon;
      this.targetOnGround = var1.targetOnGround;
      this.bAimAtFloor = var1.bAimAtFloor;
      this.bCloseKill = var1.bCloseKill;
      this.bDoShove = var1.bDoShove;
      this.bDoGrapple = var1.bDoGrapple;
      this.useChargeDelta = var1.useChargeDelta;
      this.recoilDelay = var1.recoilDelay;
      this.targetsStanding.clear();
      Iterator var2 = var1.targetsStanding.iterator();

      HitInfo var3;
      while(var2.hasNext()) {
         var3 = (HitInfo)var2.next();
         this.targetsStanding.add(var3);
      }

      this.targetsProne.clear();
      var2 = var1.targetsProne.iterator();

      while(var2.hasNext()) {
         var3 = (HitInfo)var2.next();
         this.targetsProne.add(var3);
      }

   }

   public void clear() {
      this.targetOnGround.setMovingObject((IsoMovingObject)null);
      this.targetsStanding.clear();
      this.targetsProne.clear();
      this.isProcessed = false;
   }

   public static class AttackFlags {
      public static byte isBareHeadsWeapon = 1;
      public static byte bAimAtFloor = 2;
      public static byte bCloseKill = 4;
      public static byte bDoShove = 8;
      public static byte bDoGrapple = 16;

      public AttackFlags() {
      }

      public static boolean isFlagSet(byte var0, byte var1) {
         return (var0 & var1) == var1;
      }

      public static boolean anyFlagsSet(byte var0, byte var1) {
         return (var0 & var1) != 0;
      }

      public static byte setFlagState(byte var0, byte var1, boolean var2) {
         return var2 ? (byte)(var0 | var1) : (byte)(var0 & ~var1);
      }
   }
}
