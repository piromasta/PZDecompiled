package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.RagdollBodyPart;
import zombie.debug.BaseDebugWindow;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoWorld;
import zombie.network.fields.HitInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.Type;

public class FirearmPanel extends BaseDebugWindow {
   private String selectedNode = "";
   private String selectedAttachment = "";
   private final int defaultflags = 192;
   private final int selectedflags = 193;
   private boolean disablePanic = false;
   private boolean renderAim = false;
   private ArrayList<WeaponPart> attachments = new ArrayList();

   public FirearmPanel() {
   }

   public String getTitle() {
      return "Firearm Debug";
   }

   public int getWindowFlags() {
      return 64;
   }

   boolean doTreeNode(String var1, String var2, boolean var3, boolean var4) {
      boolean var5 = var1.equals(this.selectedNode);
      int var6 = var5 ? 193 : 192;
      if (var3) {
         var6 |= 256;
      }

      if (var4) {
         var6 |= 32;
      }

      boolean var7 = ImGui.treeNodeEx(var1, var6, var2);
      if (ImGui.isItemClicked()) {
         this.selectedNode = var1;
      }

      return var7;
   }

   protected void doWindowContents() {
      if (this.attachments.isEmpty()) {
         this.attachments = (ArrayList)ScriptManager.instance.getAllItems().stream().filter((var0) -> {
            return var0.getType() == Item.Type.WeaponPart;
         }).map((var0) -> {
            return (WeaponPart)InventoryItemFactory.CreateItem(var0.getFullName());
         }).collect(Collectors.toCollection(ArrayList::new));
      }

      if (IsoWorld.instance.CurrentCell != null && IsoPlayer.getInstance() != null) {
         Core var10001 = Core.getInstance();
         Objects.requireNonNull(var10001);
         Supplier var1 = var10001::isToggleToAim;
         Core var10002 = Core.getInstance();
         Objects.requireNonNull(var10002);
         Wrappers.checkbox("Aim Toggling", var1, var10002::setToggleToAim);
         var10001 = Core.getInstance();
         Objects.requireNonNull(var10001);
         var1 = var10001::getOptionPanCameraWhileAiming;
         var10002 = Core.getInstance();
         Objects.requireNonNull(var10002);
         Wrappers.checkbox("Aim Panning", var1, var10002::setOptionPanCameraWhileAiming);
         this.doSettingTweaks();
         this.doPlayerTweaks(IsoPlayer.getInstance());
         this.doWeaponTweaks(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
         this.doTargetInfo(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
      }

   }

   private void doSettingTweaks() {
      if (this.doTreeNode("gunplaySettings", "Global Settings", false, false)) {
         ImGui.text("General");
         SandboxOptions.DoubleSandboxOption var10003 = SandboxOptions.instance.FirearmNoiseMultiplier;
         Objects.requireNonNull(var10003);
         Supplier var1 = var10003::getValue;
         SandboxOptions.DoubleSandboxOption var10004 = SandboxOptions.instance.FirearmNoiseMultiplier;
         Objects.requireNonNull(var10004);
         Wrappers.sliderDouble("Noise", 0.30000001192092896, 2.0, var1, var10004::setValue);
         CombatManager.FirearmRecoilMuscleStrainModifier = PZMath.roundFloat(Wrappers.sliderFloat("Firearm Muscle Strain", CombatManager.FirearmRecoilMuscleStrainModifier, 0.0F, 0.1F), 4);
         ImGui.treePop();
      }

   }

   private void doPlayerTweaks(IsoPlayer var1) {
      if (this.doTreeNode("gunplayPlayer", "Player", false, false)) {
         Objects.requireNonNull(var1);
         Wrappers.SupplyConsumer var10003 = var1::getPerkLevel;
         Objects.requireNonNull(var1);
         Wrappers.sliderInt("Strength", 0, 10, var10003, var1::setPerkLevelDebug, PerkFactory.Perks.Strength);
         Objects.requireNonNull(var1);
         var10003 = var1::getPerkLevel;
         Objects.requireNonNull(var1);
         Wrappers.sliderInt("Aiming", 0, 10, var10003, var1::setPerkLevelDebug, PerkFactory.Perks.Aiming);
         Objects.requireNonNull(var1);
         var10003 = var1::getPerkLevel;
         Objects.requireNonNull(var1);
         Wrappers.sliderInt("Reloading", 0, 10, var10003, var1::setPerkLevelDebug, PerkFactory.Perks.Reloading);
         Objects.requireNonNull(var1);
         var10003 = var1::getPerkLevel;
         Objects.requireNonNull(var1);
         Wrappers.sliderInt("Nimble", 0, 10, var10003, var1::setPerkLevelDebug, PerkFactory.Perks.Nimble);
         Stats var2 = var1.getStats();
         Objects.requireNonNull(var2);
         Supplier var3 = var2::getPanic;
         Stats var10004 = var1.getStats();
         Objects.requireNonNull(var10004);
         Wrappers.sliderFloat("Panic", 0.0F, 100.0F, var3, var10004::setPanic);
         ImGui.sameLine();
         this.disablePanic = Wrappers.checkbox("Disable", this.disablePanic);
         if (this.disablePanic) {
            var1.getStats().Panic = 0.0F;
         }

         var1.Traits.Marksman.set(Wrappers.checkbox("Marksman", var1.Traits.Marksman.isSet()));
         var1.Traits.Dextrous.set(Wrappers.checkbox("Dextrous", var1.Traits.Dextrous.isSet()));
         var1.Traits.NightVision.set(Wrappers.checkbox("NightVision", var1.Traits.NightVision.isSet()));
         var1.Traits.EagleEyed.set(Wrappers.checkbox("Eagle Eyed", var1.Traits.EagleEyed.isSet()));
         var1.Traits.ShortSighted.set(Wrappers.checkbox("Short Sighted", var1.Traits.ShortSighted.isSet()));
         ImGui.treePop();
      }

   }

   private void doWeaponTweaks(IsoPlayer var1, HandWeapon var2) {
      if (var2 != null && var2.isAimedFirearm()) {
         if (this.doTreeNode("gunplayFirearm", "Firearm", false, true)) {
            ImGui.text(var2.getDisplayName());
            int var10002;
            Supplier var10003;
            if (this.doTreeNode("gunplayAmmo", "Ammo", false, false)) {
               var10002 = var2.getMaxAmmo();
               Objects.requireNonNull(var2);
               var10003 = var2::getCurrentAmmoCount;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Count", 0, var10002, var10003, var2::setCurrentAmmoCount);
               Objects.requireNonNull(var2);
               var10003 = var2::getMaxAmmo;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Max", 0, 100, var10003, var2::setMaxAmmo);
               Objects.requireNonNull(var2);
               var10003 = var2::getMaxHitCount;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Max Hit Count", 0, 10, var10003, var2::setMaxHitCount);
               Objects.requireNonNull(var2);
               var10003 = var2::getProjectileCount;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Projectile Count", 0, 10, var10003, var2::setProjectileCount);
               Objects.requireNonNull(var2);
               var10003 = var2::getRecoilDelay;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Recoil Delay", 0, 100, var10003, var2::setRecoilDelay);
               Objects.requireNonNull(var2);
               Supplier var10001 = var2::isRoundChambered;
               Objects.requireNonNull(var2);
               Wrappers.checkbox("Round Chambered", var10001, var2::setRoundChambered);
               ImGui.sameLine();
               Objects.requireNonNull(var2);
               var10001 = var2::isJammed;
               Objects.requireNonNull(var2);
               Wrappers.checkbox("Jammed", var10001, var2::setJammed);
               ImGui.treePop();
            }

            Supplier var10004;
            if (this.doTreeNode("gunplayAccuracy", "Accuracy", false, false)) {
               Objects.requireNonNull(var2);
               var10003 = var2::getAimingTime;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("AimingTime", 0, 100, var10003, var2::setAimingTime);
               Objects.requireNonNull(var2);
               var10003 = var2::getHitChance;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("HitChance", 0, 100, var10003, var2::setHitChance);
               Objects.requireNonNull(var2);
               var10004 = var2::getAimingPerkHitChanceModifier;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("* Per Lvl", 0.0F, 20.0F, 2, var10004, var2::setAimingPerkHitChanceModifier);
               Objects.requireNonNull(var2);
               var10004 = var2::getMinAngle;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MinAngle", 0.8F, 1.0F, 3, var10004, var2::setMinAngle);
               Objects.requireNonNull(var2);
               var10004 = var2::getMinSightRange;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MinSightRange", 1.0F, 40.0F, 2, var10004, var2::setMinSightRange);
               Objects.requireNonNull(var2);
               var10004 = var2::getMaxSightRange;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MaxSightRange", 1.0F, 40.0F, 2, var10004, var2::setMaxSightRange);
               ImGui.treePop();
            }

            if (this.doTreeNode("gunplayDamage", "Damage", false, false)) {
               Objects.requireNonNull(var2);
               var10004 = var2::getMinDamage;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MinDamage", 0.0F, 10.0F, 2, var10004, var2::setMinDamage);
               Objects.requireNonNull(var2);
               var10004 = var2::getMaxDamage;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MaxDamage", 0.0F, 10.0F, 2, var10004, var2::setMaxDamage);
               Objects.requireNonNull(var2);
               var10004 = var2::getCriticalChance;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("Crit Chance", 0.0F, 100.0F, 2, var10004, var2::setCriticalChance);
               Objects.requireNonNull(var2);
               var10004 = var2::getCritDmgMultiplier;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("* Multiplier", 0.0F, 100.0F, 2, var10004, var2::setCritDmgMultiplier);
               Objects.requireNonNull(var2);
               var10003 = var2::getAimingPerkCritModifier;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("* Per Lvl", 0, 50, var10003, var2::setAimingPerkCritModifier);
               ImGui.treePop();
            }

            if (this.doTreeNode("gunplayMisc", "Misc", false, false)) {
               Objects.requireNonNull(var2);
               var10004 = var2::getMaxRange;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("MaxRange", 0.0F, 100.0F, 2, var10004, var2::setMaxRange);
               Objects.requireNonNull(var2);
               var10004 = var2::getAimingPerkRangeModifier;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("* Per Lvl", 0.0F, 20.0F, 2, var10004, var2::setAimingPerkRangeModifier);
               Objects.requireNonNull(var2);
               var10004 = var2::getSwingTime;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("Swingtime", 0.1F, 5.0F, 2, var10004, var2::setSwingTime);
               Objects.requireNonNull(var2);
               var10004 = var2::getMinimumSwingTime;
               Objects.requireNonNull(var2);
               Wrappers.sliderFloat("* Min", 0.1F, 5.0F, 2, var10004, var2::setMinimumSwingTime);
               var10002 = var2.getConditionMax();
               Objects.requireNonNull(var2);
               var10003 = var2::getCondition;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("Condition", 0, var10002, var10003, var2::setCondition);
               Objects.requireNonNull(var2);
               var10003 = var2::getConditionLowerChance;
               Objects.requireNonNull(var2);
               Wrappers.sliderInt("* Lower Chance", 0, 300, var10003, var2::setConditionLowerChance);
               ImGui.treePop();
            }

            if (this.doTreeNode("gunplayAttach", "Attachments", false, false)) {
               Iterator var3 = var2.getAllWeaponParts().iterator();

               WeaponPart var4;
               while(var3.hasNext()) {
                  var4 = (WeaponPart)var3.next();
                  ImGui.text(var4.getFullType());
                  ImGui.sameLine();
                  if (ImGui.button("Remove")) {
                     var2.detachWeaponPart(var4);
                  }
               }

               if (ImGui.beginCombo("Add attachment", this.selectedAttachment)) {
                  var3 = this.attachments.iterator();

                  while(var3.hasNext()) {
                     var4 = (WeaponPart)var3.next();
                     if (var4.canAttach(var1, var2)) {
                        Wrappers.valueBoolean.set(this.selectedAttachment.equals(var4.getFullType()));
                        ImGui.selectable(var4.getFullType(), Wrappers.valueBoolean);
                        if (Wrappers.valueBoolean.get()) {
                           this.selectedAttachment = var4.getFullType();
                           ImGui.setItemDefaultFocus();
                        }
                     }
                  }

                  ImGui.endCombo();
               }

               ImGui.sameLine();
               if (ImGui.button("Add") && !StringUtils.isNullOrEmpty(this.selectedAttachment)) {
                  var2.attachWeaponPart((WeaponPart)InventoryItemFactory.CreateItem(this.selectedAttachment));
               }

               ImGui.treePop();
            }

            ImGui.treePop();
         }

      }
   }

   private void doTargetInfo(IsoPlayer var1, HandWeapon var2) {
      if (var2 != null && var2.isAimedFirearm()) {
         if (this.doTreeNode("gunplayTargets", "Targets", false, false)) {
            if (ImGui.beginTable("gunplayTargetTable", 10, 1984)) {
               if (var1.isAiming()) {
                  CombatManager.getInstance().calculateAttackVars(var1);
                  CombatManager.getInstance().calculateHitInfoList(var1);
               }

               ImGui.tableSetupColumn("id");
               ImGui.tableSetupColumn("distance");
               ImGui.tableSetupColumn("penalty");
               ImGui.tableSetupColumn("speed");
               ImGui.tableSetupColumn("light");
               ImGui.tableSetupColumn("penalty");
               ImGui.tableSetupColumn("health");
               ImGui.tableSetupColumn("chance");
               ImGui.tableSetupColumn("isCameraTarget");
               ImGui.tableSetupColumn("BodyPart");
               ImGui.tableHeadersRow();
               float var3 = var2.getMaxSightRange(var1);
               float var4 = var2.getMinSightRange(var1);
               int var5 = -1;
               ArrayList var6 = var1.getHitInfoList();

               for(int var7 = 0; var7 < var6.size(); ++var7) {
                  int var8 = 0;
                  HitInfo var9 = (HitInfo)var6.get(var7);
                  IsoGameCharacter var10 = (IsoGameCharacter)Type.tryCastTo(var9.getObject(), IsoGameCharacter.class);
                  if (var10 != null) {
                     IsoZombie var11 = (IsoZombie)var9.getObject();
                     ImGui.tableNextRow();
                     ImGui.tableSetColumnIndex(var8++);
                     if (var11 == null) {
                        ImGui.text("ID");
                     } else {
                        var5 = var11.getID();
                        ImGui.text(Integer.toString(var5));
                     }

                     ImGui.tableSetColumnIndex(var8++);
                     float var12 = PZMath.sqrt(var9.distSq);
                     float var13 = 0.0F;
                     if (var12 < var4) {
                        if (var12 > 3.0F) {
                           var13 -= (var12 - var4) * 3.0F;
                        }
                     } else if (var12 >= var3) {
                        var13 -= (var12 - var3) * 3.0F;
                     } else {
                        float var14 = (var3 - var4) * 0.5F;
                        var13 += 15.0F * (1.0F - Math.abs((var12 - var4 - var14) / var14));
                     }

                     ImGui.text(Float.toString(PZMath.roundFloat(var12, 3)));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Float.toString(PZMath.roundFloat(var13, 3)));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Float.toString(PZMath.roundFloat(var10.getMovementSpeed() * GameTime.getInstance().getInvMultiplier(), 6)));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Float.toString(PZMath.roundFloat(var10.getCurrentSquare().getLightLevel(var1.getPlayerNum()), 3)));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Float.toString(PZMath.max(0.0F, 50.0F * (1.0F - var10.getCurrentSquare().getLightLevel(var1.getPlayerNum()) / 0.75F))));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Float.toString(PZMath.roundFloat(var10.getHealth(), 3)));
                     ImGui.tableSetColumnIndex(var8++);
                     ImGui.text(Integer.toString(var9.chance));
                     BallisticsController var18 = var1.getBallisticsController();
                     if (var18 != null) {
                        boolean var15 = var18.isCachedCameraTarget(var5);
                        ImGui.tableSetColumnIndex(var8++);
                        ImGui.text(Boolean.toString(var15));
                        int var16 = RagdollBodyPart.BODYPART_COUNT.ordinal();
                        if (var15) {
                           var16 = var18.getCachedTargetedBodyPart(var11.getID());
                           String var17 = RagdollBodyPart.values()[var16].name();
                           ImGui.tableSetColumnIndex(var8++);
                           ImGui.text(var17);
                        }
                     }
                  }
               }

               ImGui.endTable();
            }

            ImGui.treePop();
         }

      }
   }
}
