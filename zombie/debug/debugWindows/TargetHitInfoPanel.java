package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.network.fields.HitInfo;
import zombie.util.Type;

public class TargetHitInfoPanel extends PZDebugWindow {
   private IsoGameCharacter isoGameCharacter;
   public ArrayList<HitInfo> hitInfoList = new ArrayList();

   public TargetHitInfoPanel() {
   }

   public void setIsoGameCharacter(IsoGameCharacter var1) {
      this.isoGameCharacter = var1;
   }

   public String getTitle() {
      return "Target Hit Info";
   }

   protected void doWindowContents() {
      if (this.isoGameCharacter != null) {
         BallisticsController var1 = this.isoGameCharacter.getBallisticsController();
         int var2;
         float[] var3;
         int var5;
         int var6;
         int var7;
         float var8;
         float var9;
         float var10;
         int var22;
         if (var1 != null) {
            if (ImGui.beginTable("Ballistics Controller Targets", 4, 1984)) {
               ImGui.tableSetupColumn("id");
               ImGui.tableSetupColumn("X");
               ImGui.tableSetupColumn("Y");
               ImGui.tableSetupColumn("Z");
               ImGui.tableHeadersRow();
            }

            var2 = var1.getCachedNumberOfTargets();
            var3 = var1.getCachedBallisticsTargets();
            boolean var4 = false;

            for(var5 = 0; var5 < var2; ++var5) {
               var6 = 0;
               var22 = var5 * 4;
               var7 = (int)var3[var22++];
               var8 = var3[var22++];
               var9 = var3[var22++];
               var10 = var3[var22++];
               ImGui.tableNextRow();
               ImGui.tableSetColumnIndex(var6++);
               ImGui.text(Integer.toString(var7));
               ImGui.tableSetColumnIndex(var6++);
               ImGui.text(Float.toString(var8));
               ImGui.tableSetColumnIndex(var6++);
               ImGui.text(Float.toString(var9));
               ImGui.tableSetColumnIndex(var6++);
               ImGui.text(Float.toString(var10));
            }
         }

         ImGui.endTable();
         if (ImGui.beginTable("Target Hit Info", 11, 1984)) {
            ImGui.tableSetupColumn("id");
            ImGui.tableSetupColumn("Target Facing");
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
            HandWeapon var20 = this.isoGameCharacter.getUseHandWeapon();
            float var21 = var20.getMaxSightRange(this.isoGameCharacter);
            float var23 = var20.getMinSightRange(this.isoGameCharacter);
            var5 = -1;

            for(var6 = 0; var6 < this.hitInfoList.size(); ++var6) {
               byte var24 = 0;
               HitInfo var25 = (HitInfo)this.hitInfoList.get(var6);
               IsoGameCharacter var27 = (IsoGameCharacter)Type.tryCastTo(var25.getObject(), IsoGameCharacter.class);
               if (var27 != null && !var27.isAnimal()) {
                  IsoZombie var28 = (IsoZombie)var25.getObject();
                  ImGui.tableNextRow();
                  var7 = var24 + 1;
                  ImGui.tableSetColumnIndex(var24);
                  if (var28 == null) {
                     ImGui.text("ID");
                  } else {
                     var5 = var28.getID();
                     ImGui.text(Integer.toString(var5));
                  }

                  ImGui.tableSetColumnIndex(var7++);
                  var28.setHitFromBehind(this.isoGameCharacter.isBehind(var28));
                  boolean var11 = var28.isHitFromBehind();
                  ImGui.text(var11 ? "Behind" : "Forward");
                  ImGui.tableSetColumnIndex(var7++);
                  float var12 = PZMath.sqrt(var25.distSq);
                  float var13 = 0.0F;
                  if (var12 < var23) {
                     if (var12 > 3.0F) {
                        var13 -= (var12 - var23) * 3.0F;
                     }
                  } else if (var12 >= var21) {
                     var13 -= (var12 - var21) * 3.0F;
                  } else {
                     float var14 = (var21 - var23) * 0.5F;
                     var13 += 15.0F * (1.0F - Math.abs((var12 - var23 - var14) / var14));
                  }

                  ImGui.text(Float.toString(PZMath.roundFloat(var12, 3)));
                  ImGui.tableSetColumnIndex(var7++);
                  ImGui.text(Float.toString(PZMath.roundFloat(var13, 3)));
                  ImGui.tableSetColumnIndex(var7++);
                  ImGui.text(Float.toString(PZMath.roundFloat(var27.getMovementSpeed() * GameTime.getInstance().getInvMultiplier(), 6)));
                  int var30 = ((IsoPlayer)this.isoGameCharacter).getPlayerNum();
                  ImGui.tableSetColumnIndex(var7++);
                  IsoGridSquare var15 = var27.getCurrentSquare();
                  float var16 = 0.0F;
                  if (var15 != null) {
                     ImGui.text(Float.toString(PZMath.roundFloat(var15.getLightLevel(var30), 3)));
                     var16 = var27.getCurrentSquare().getLightLevel(var30);
                  } else {
                     ImGui.text("null");
                  }

                  ImGui.tableSetColumnIndex(var7++);
                  ImGui.text(Float.toString(PZMath.max(0.0F, 50.0F * (1.0F - var16 / 0.75F))));
                  ImGui.tableSetColumnIndex(var7++);
                  ImGui.text(Float.toString(PZMath.roundFloat(var27.getHealth(), 3)));
                  ImGui.tableSetColumnIndex(var7++);
                  ImGui.text(Float.toString(var13));
                  if (var1 != null) {
                     boolean var17 = var1.isCachedCameraTarget(var5);
                     ImGui.tableSetColumnIndex(var7++);
                     ImGui.text(Boolean.toString(var17));
                     int var18 = RagdollBodyPart.BODYPART_COUNT.ordinal();
                     if (var17) {
                        var18 = var1.getCachedTargetedBodyPart(var5);
                        String var19 = RagdollBodyPart.values()[var18].name();
                        ImGui.tableSetColumnIndex(var7++);
                        ImGui.text(var19);
                     }
                  }
               }
            }
         }

         ImGui.endTable();
         if (var1 != null) {
            var2 = var1.getNumberOfCachedSpreadData();
            if (var2 != 0) {
               if (ImGui.beginTable("Spread Data", 4, 1984)) {
                  ImGui.tableSetupColumn("id");
                  ImGui.tableSetupColumn("x");
                  ImGui.tableSetupColumn("y");
                  ImGui.tableSetupColumn("z");
                  ImGui.tableHeadersRow();
                  var3 = var1.getCachedBallisticsTargetSpreadData();
                  var22 = 0;

                  for(var5 = 0; var5 < var2; ++var5) {
                     byte var26 = 0;
                     ImGui.tableNextRow();
                     var6 = var26 + 1;
                     ImGui.tableSetColumnIndex(var26);
                     float var29 = var3[var22++];
                     var8 = var3[var22++];
                     var9 = var3[var22++] / 2.46F;
                     var10 = var3[var22++];
                     if (var29 != 0.0F) {
                        ImGui.text(Float.toString(var29));
                        ImGui.tableSetColumnIndex(var6++);
                        ImGui.text(Float.toString(var8));
                        ImGui.tableSetColumnIndex(var6++);
                        ImGui.text(Float.toString(var9));
                        ImGui.tableSetColumnIndex(var6++);
                        ImGui.text(Float.toString(var10));
                     }
                  }

                  ImGui.endTable();
               }

            }
         }
      }
   }
}
