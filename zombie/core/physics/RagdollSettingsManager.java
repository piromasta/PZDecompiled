package zombie.core.physics;

import zombie.characters.IsoZombie;
import zombie.characters.RagdollBuilder;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public class RagdollSettingsManager {
   public static final int GlobalChanceID = 0;
   private static RagdollSettingsManager instance = new RagdollSettingsManager();
   private final int RagdollSettingsCount = 3;
   private final int HitReactionSettingsCount = 36;
   private final int LocationsCount = 11;
   private final int GlobalImpulseID = 12;
   private final int GlobalUpImpulseID = 24;
   private final RagdollSetting[] ragdollSettings = new RagdollSetting[3];
   private final HitReactionSetting[] hitReactionSettings = new HitReactionSetting[36];
   private final ForceHitReactionLocation[] forceHitReactionLocations = new ForceHitReactionLocation[11];

   public RagdollSettingsManager() {
      this.setup();
   }

   public static RagdollSettingsManager getInstance() {
      return instance;
   }

   public static void setInstance(RagdollSettingsManager var0) {
      instance = var0;
   }

   public int getSettingsCount() {
      return 3;
   }

   public int getHitReactionSettingsCount() {
      return 36;
   }

   public int getHitReactionLocationsCount() {
      return 11;
   }

   public boolean usePhysicHitReaction(IsoZombie var1) {
      if (!this.isForcedHitReaction()) {
         return false;
      } else {
         HitReactionSetting var2 = this.getHitReactionSetting(0);
         float var3 = var2.getAdminValue();
         if (!var2.isEnableAdmin()) {
            String var4 = var1.getHitReaction();
            RagdollBodyPart var5 = RagdollBodyPart.BODYPART_PELVIS;
            switch (var4) {
               case "ShotBelly":
               case "ShotBellyStep":
                  var5 = RagdollBodyPart.BODYPART_PELVIS;
                  break;
               case "ShotChest":
               case "ShotChestR":
               case "ShotChestL":
                  var5 = RagdollBodyPart.BODYPART_SPINE;
                  break;
               case "ShotLegR":
                  var5 = RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
                  break;
               case "ShotLegL":
                  var5 = RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
                  break;
               case "ShotShoulderStepR":
                  var5 = RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
                  break;
               case "ShotShoulderStepL":
                  var5 = RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
                  break;
               case "ShotHeadFwd":
               case "ShotHeadFwd02":
                  var5 = RagdollBodyPart.BODYPART_HEAD;
                  break;
               default:
                  DebugLog.Physics.debugln("RagdollState: HitReaction %s CASE NOT DEFINED", var4);
            }

            boolean var6 = this.getEnabledSetting(var5);
            if (!var6) {
               return false;
            } else {
               var3 = this.getChanceSetting(var5);
               float var8 = Rand.Next(0.0F, 100.0F);
               return var3 > 0.0F && var8 <= var3;
            }
         } else {
            return true;
         }
      }
   }

   private RagdollSetting initRagdollSetting(int var1, String var2, float var3, float var4, float var5) {
      if (var1 >= 0 && var1 < this.ragdollSettings.length) {
         return this.ragdollSettings[var1].init(var1, var2, var3, var4, var5);
      } else {
         DebugLog.Physics.error("RagdollSetting: id(%i) out of range ", var1);
         return null;
      }
   }

   private HitReactionSetting initHitReactionSetting(int var1, String var2, float var3, float var4, float var5) {
      if (var1 >= 0 && var1 < this.hitReactionSettings.length) {
         return this.hitReactionSettings[var1].init(var1, var2, var3, var4, var5);
      } else {
         DebugLog.Physics.error("HitReactionSetting: id(%i) out of range ", var1);
         return null;
      }
   }

   private ForceHitReactionLocation initForceHitReactionLocation(int var1, String var2) {
      if (var1 >= 0 && var1 < this.forceHitReactionLocations.length) {
         return this.forceHitReactionLocations[var1].init(var1, var2);
      } else {
         DebugLog.Physics.error("ForceHitReactionLocation: id(%i) out of range ", var1);
         return null;
      }
   }

   public RagdollSetting getSetting(int var1) {
      if (var1 >= 0 && var1 < this.ragdollSettings.length) {
         return this.ragdollSettings[var1];
      } else {
         DebugLog.Physics.error("RagdollSetting: id(%i) out of range ", var1);
         return null;
      }
   }

   public HitReactionSetting getHitReactionSetting(int var1) {
      if (var1 >= 0 && var1 < this.hitReactionSettings.length) {
         return this.hitReactionSettings[var1];
      } else {
         DebugLog.Physics.error("HitReactionSetting: id(%i) out of range ", var1);
         return null;
      }
   }

   public boolean getEnabledSetting(RagdollBodyPart var1) {
      int var2 = var1.ordinal() + 1;
      return this.getHitReactionSetting(var2).isAdminOverride;
   }

   public float getChanceSetting(RagdollBodyPart var1) {
      int var2 = var1.ordinal() + 1;
      return this.getHitReactionSetting(var2).adminValue;
   }

   public float getImpulseSetting(RagdollBodyPart var1) {
      int var2 = var1.ordinal() + 12 + 1;
      return this.getHitReactionSetting(var2).adminValue;
   }

   public float getUpImpulseSetting(RagdollBodyPart var1) {
      int var2 = var1.ordinal() + 24 + 1;
      return this.getHitReactionSetting(var2).adminValue;
   }

   public float getGlobalImpulseSetting() {
      return this.getHitReactionSetting(12).adminValue;
   }

   public float getGlobalUpImpulseSetting() {
      return this.getHitReactionSetting(24).adminValue;
   }

   public ForceHitReactionLocation getForceHitReactionLocation(int var1) {
      if (var1 >= 0 && var1 < this.forceHitReactionLocations.length) {
         return this.forceHitReactionLocations[var1];
      } else {
         DebugLog.Physics.error("ForceHitReactionLocation: id(%i) out of range ", var1);
         return null;
      }
   }

   private void setup() {
      int var1;
      for(var1 = 0; var1 < this.ragdollSettings.length; ++var1) {
         this.ragdollSettings[var1] = new RagdollSetting();
      }

      for(var1 = 0; var1 < this.hitReactionSettings.length; ++var1) {
         this.hitReactionSettings[var1] = new HitReactionSetting();
      }

      for(var1 = 0; var1 < this.forceHitReactionLocations.length; ++var1) {
         this.forceHitReactionLocations[var1] = new ForceHitReactionLocation();
      }

      float var20 = RagdollBuilder.instance.getMass();
      float var2 = 1.0F;
      float var3 = 100.0F;
      float var4 = RagdollBuilder.instance.getFriction();
      float var5 = 0.0F;
      float var6 = 5.0F;
      float var7 = RagdollBuilder.instance.getRollingFriction();
      float var8 = 0.0F;
      float var9 = 5.0F;
      float var10 = 100.0F;
      float var11 = 0.0F;
      float var12 = 100.0F;
      float var13 = 80.0F;
      float var14 = 0.0F;
      float var15 = 200.0F;
      float var16 = 40.0F;
      float var17 = 0.0F;
      float var18 = 200.0F;
      int var19 = 0;
      this.initRagdollSetting(var19++, "Mass", var20, var2, var3);
      this.initRagdollSetting(var19++, "Friction", var4, var5, var6);
      this.initRagdollSetting(var19++, "Rolling Friction", var7, var8, var9);
      byte var22 = 0;
      var19 = var22 + 1;
      this.initHitReactionSetting(var22, "Chance:Global", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:Pelvis", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:Spine", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:Head", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:L Thigh", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:L Calf", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:R Thigh", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:R Calf", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:L Upper Arm", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:L Lower Arm", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:R Upper Arm", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Chance:R Lower Arm", var10, var11, var12);
      this.initHitReactionSetting(var19++, "Impulse:Global", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:Pelvis", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:Spine", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:Head", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:L Thigh", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:L Calf", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:R Thigh", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:R Calf", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:L Upper Arm", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:L Lower Arm", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:R Upper Arm", var13, var14, var15);
      this.initHitReactionSetting(var19++, "Impulse:R Lower Arm", var13, var14, var15);
      this.initHitReactionSetting(var19++, "UpImpulse:Global", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:Pelvis", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:Spine", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:Head", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:L Thigh", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:L Calf", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:R Thigh", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:R Calf", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:L Upper Arm", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:L Lower Arm", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:R Upper Arm", var16, var17, var18);
      this.initHitReactionSetting(var19++, "UpImpulse:R Lower Arm", var16, var17, var18);
      var22 = 0;
      var19 = var22 + 1;
      this.initForceHitReactionLocation(var22, "Pelvis");
      this.initForceHitReactionLocation(var19++, "Spine");
      this.initForceHitReactionLocation(var19++, "Head");
      this.initForceHitReactionLocation(var19++, "L Thigh");
      this.initForceHitReactionLocation(var19++, "L Calf");
      this.initForceHitReactionLocation(var19++, "R Thigh");
      this.initForceHitReactionLocation(var19++, "R Calf");
      this.initForceHitReactionLocation(var19++, "L Upper Arm");
      this.initForceHitReactionLocation(var19++, "L Lower Arm");
      this.initForceHitReactionLocation(var19++, "R Upper Arm");
      this.initForceHitReactionLocation(var19++, "R Lower Arm");
   }

   public float getSandboxHitReactionFrequency() {
      float var1 = 100.0F;
      return var1;
   }

   public float getSandboxHitReactionImpulseStrength() {
      float var1 = this.getGlobalImpulseSetting();
      return var1;
   }

   public float getSandboxHitReactionUpImpulseStrength() {
      float var1 = this.getGlobalUpImpulseSetting();
      return var1;
   }

   public void resetToDefaults() {
      int var1;
      for(var1 = 0; var1 < this.ragdollSettings.length; ++var1) {
         this.ragdollSettings[var1].reset();
      }

      for(var1 = 0; var1 < this.hitReactionSettings.length; ++var1) {
         this.hitReactionSettings[var1].reset();
      }

      for(var1 = 0; var1 < this.forceHitReactionLocations.length; ++var1) {
         this.forceHitReactionLocations[var1].setAdminValue(false);
      }

   }

   public boolean isForcedHitReaction() {
      for(int var1 = 0; var1 < this.forceHitReactionLocations.length; ++var1) {
         if (this.forceHitReactionLocations[var1].getAdminValue()) {
            return true;
         }
      }

      return false;
   }

   public ForceHitReactionLocation getForceHitReactionLocation() {
      for(int var1 = 0; var1 < this.forceHitReactionLocations.length; ++var1) {
         if (this.forceHitReactionLocations[var1].getAdminValue()) {
            return this.forceHitReactionLocations[var1];
         }
      }

      return null;
   }

   public String getForcedHitReactionLocationAsShotLocation() {
      int var1 = -1;

      for(int var2 = 0; var2 < this.forceHitReactionLocations.length; ++var2) {
         if (this.forceHitReactionLocations[var2].getAdminValue()) {
            var1 = var2;
            break;
         }
      }

      String var3 = "Default";
      switch (var1) {
         case 0:
            var3 = "ShotBelly";
            break;
         case 1:
            var3 = "ShotChest";
            break;
         case 2:
            var3 = "ShotHeadFwd";
            break;
         case 3:
            var3 = "ShotLegL";
            break;
         case 4:
            var3 = "ShotLegL";
            break;
         case 5:
            var3 = "ShotLegR";
            break;
         case 6:
            var3 = "ShotLegR";
            break;
         case 7:
            var3 = "ShotShoulderStepL";
            break;
         case 8:
            var3 = "ShotShoulderStepL";
            break;
         case 9:
            var3 = "ShotShoulderStepR";
            break;
         case 10:
            var3 = "ShotShoulderStepR";
            break;
         default:
            DebugLog.Physics.debugln("RagdollSettingManager: bodyPartID %s CASE NOT DEFINED", var1);
      }

      return var3;
   }

   public void update() {
      float var1 = this.ragdollSettings[0].adminValue;
      float var2 = this.ragdollSettings[1].adminValue;
      float var3 = this.ragdollSettings[2].adminValue;
      RagdollBuilder.instance.setMass(var1);
      RagdollBuilder.instance.setFriction(var2, var3);
   }

   public static class RagdollSetting {
      private int id;
      private String name;
      private float min = 0.0F;
      private float max = 1.0F;
      private boolean isAdminOverride = false;
      private float adminValue = 0.0F;
      private float finalValue;
      private float defaultValue;

      public RagdollSetting() {
      }

      public RagdollSetting init(int var1, String var2, float var3, float var4, float var5) {
         this.id = var1;
         this.name = var2;
         this.min = var4;
         this.max = var5;
         this.adminValue = var3;
         this.defaultValue = var3;
         return this;
      }

      public String getName() {
         return this.name;
      }

      public float getMin() {
         return this.min;
      }

      public float getMax() {
         return this.max;
      }

      public boolean isEnableAdmin() {
         return this.isAdminOverride;
      }

      public void setEnableAdmin(boolean var1) {
         this.isAdminOverride = var1;
      }

      public float getAdminValue() {
         return this.adminValue;
      }

      public void setAdminValue(float var1) {
         if (this.adminValue != var1) {
            this.adminValue = var1;
            RagdollSettingsManager.getInstance().update();
         }

      }

      private void calculate() {
         if (this.isAdminOverride && !GameClient.bClient) {
            this.finalValue = this.adminValue;
         }

      }

      public void reset() {
         if (this.adminValue != this.defaultValue) {
            this.adminValue = this.defaultValue;
            RagdollSettingsManager.getInstance().update();
         }

      }
   }

   public static class HitReactionSetting {
      private int id;
      private String name;
      private float min = 0.0F;
      private float max = 1.0F;
      private boolean isAdminOverride = false;
      private float adminValue = 0.0F;
      private float finalValue;
      private float defaultValue;

      public HitReactionSetting() {
      }

      public HitReactionSetting init(int var1, String var2, float var3, float var4, float var5) {
         this.id = var1;
         this.name = var2;
         this.min = var4;
         this.max = var5;
         this.adminValue = var3;
         this.defaultValue = var3;
         return this;
      }

      public String getName() {
         return this.name;
      }

      public float getMin() {
         return this.min;
      }

      public float getMax() {
         return this.max;
      }

      public boolean isEnableAdmin() {
         return this.isAdminOverride;
      }

      public void setEnableAdmin(boolean var1) {
         this.isAdminOverride = var1;
      }

      public float getAdminValue() {
         return this.adminValue;
      }

      public void setAdminValue(float var1) {
         this.adminValue = var1;
      }

      private void calculate() {
         if (this.isAdminOverride && !GameClient.bClient) {
            this.finalValue = this.adminValue;
         }

      }

      public void reset() {
         this.adminValue = this.defaultValue;
      }
   }

   public static class ForceHitReactionLocation {
      int id;
      private String name;
      private boolean isAdminOverride = false;
      private boolean adminValue = false;

      public ForceHitReactionLocation() {
      }

      public ForceHitReactionLocation init(int var1, String var2) {
         this.id = var1;
         this.name = var2;
         return this;
      }

      public int getID() {
         return this.id;
      }

      public String getName() {
         return this.name;
      }

      public boolean isEnableAdmin() {
         return this.isAdminOverride;
      }

      public void setEnableAdmin(boolean var1) {
         this.isAdminOverride = var1;
      }

      public boolean getAdminValue() {
         return this.adminValue;
      }

      public void setAdminValue(boolean var1) {
         this.adminValue = var1;
      }
   }
}
