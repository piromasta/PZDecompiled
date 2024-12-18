package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;
import zombie.characters.RagdollBuilder;
import zombie.core.physics.RagdollJoint;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.scripting.objects.RagdollAnchor;
import zombie.scripting.objects.RagdollConstraint;
import zombie.scripting.objects.RagdollScript;

public class RagdollDebugWindow extends PZDebugWindow {
   private static final float ConstraintPositionOffsetMax = 0.25F;
   private static final int HingeConstraintType = 4;
   private static final int ConeTwistConstraintType = 5;
   private final String[] constraintTypeArray = new String[]{"Hinge", "Cone Twist"};
   private final String[] boneNameArray = new String[]{"Dummy01", "Bip01", "Bip01_Pelvis", "Bip01_Spine", "Bip01_Spine1", "Bip01_Neck", "Bip01_Head", "Bip01_L_Clavicle", "Bip01_L_UpperArm", "Bip01_L_Forearm", "Bip01_L_Hand", "Bip01_L_Finger0", "Bip01_L_Finger1", "Bip01_R_Clavicle", "Bip01_R_UpperArm", "Bip01_R_Forearm", "Bip01_R_Hand", "Bip01_R_Finger0", "Bip01_R_Finger1", "Bip01_BackPack", "Bip01_L_Thigh", "Bip01_L_Calf", "Bip01_L_Foot", "Bip01_L_Toe0", "Bip01_R_Thigh", "Bip01_R_Calf", "Bip01_R_Foot", "Bip01_R_Toe0", "Bip01_DressFront", "Bip01_DressFront02", "Bip01_DressBack", "Bip01_DressBack02", "Bip01_Prop1", "Bip01_Prop2", "Translation_Data"};
   private final String[] bodyPartArray = new String[]{"BODYPART_PELVIS", "BODYPART_SPINE", "BODYPART_HEAD", "BODYPART_LEFT_UPPER_LEG", "BODYPART_LEFT_LOWER_LEG", "BODYPART_RIGHT_UPPER_LEG", "BODYPART_RIGHT_LOWER_LEG", "BODYPART_LEFT_UPPER_ARM", "BODYPART_LEFT_LOWER_ARM", "BODYPART_RIGHT_UPPER_ARM", "BODYPART_RIGHT_LOWER_ARM", "BODYPART_COUNT"};

   public RagdollDebugWindow() {
   }

   public String getTitle() {
      return "Ragdoll Editor";
   }

   protected void doWindowContents() {
      if (ImGui.beginTabBar("tabSelector")) {
         if (ImGui.beginTabItem("Joint Constraints")) {
            this.jointConstraintsTab();
            ImGui.endTabItem();
         }

         if (RagdollBuilder.instance.isSkeletonBoneHiearchyInitalized() && ImGui.beginTabItem("Skeleton")) {
            this.skeletonTab();
            ImGui.endTabItem();
         }

         if (ImGui.beginTabItem("Bone To Body Part Anchors")) {
            this.boneToBodyPartAnchorsTab();
            ImGui.endTabItem();
         }

         ImGui.endTabBar();
      }

   }

   private void jointConstraintsTab() {
      Iterator var1 = RagdollScript.getRagdollConstraintList().iterator();

      while(var1.hasNext()) {
         RagdollConstraint var2 = (RagdollConstraint)var1.next();
         if (PZImGui.collapsingHeader(RagdollJoint.values()[var2.joint].toString())) {
            ImGui.beginChild(RagdollJoint.values()[var2.joint].toString());
            int var3 = var2.constraintType == 4 ? 0 : 1;
            ImInt var4 = new ImInt(var3);
            if (PZImGui.combo("Constraint Type", var4, this.constraintTypeArray)) {
               var2.constraintType = var4.get() == 0 ? 4 : 5;
            }

            int var5 = var2.constraintPartA;
            ImInt var6 = new ImInt(var5);
            if (PZImGui.combo("Constraint Part A", var6, this.bodyPartArray)) {
               var2.constraintPartA = var6.get();
            }

            int var7 = var2.constraintPartB;
            ImInt var8 = new ImInt(var7);
            if (PZImGui.combo("Constraint Part B", var8, this.bodyPartArray)) {
               var2.constraintPartB = var6.get();
            }

            var2.constraintAxisA.x = PZImGui.sliderFloat("constraintAxisA.x", var2.constraintAxisA.x, -3.1415927F, 3.1415927F);
            var2.constraintAxisA.y = PZImGui.sliderFloat("constraintAxisA.y", var2.constraintAxisA.y, -3.1415927F, 3.1415927F);
            var2.constraintAxisA.z = PZImGui.sliderFloat("constraintAxisA.z", var2.constraintAxisA.z, -3.1415927F, 3.1415927F);
            var2.constraintAxisB.x = PZImGui.sliderFloat("constraintAxisB.x", var2.constraintAxisB.x, -3.1415927F, 3.1415927F);
            var2.constraintAxisB.y = PZImGui.sliderFloat("constraintAxisB.y", var2.constraintAxisB.y, -3.1415927F, 3.1415927F);
            var2.constraintAxisB.z = PZImGui.sliderFloat("constraintAxisB.z", var2.constraintAxisB.z, -3.1415927F, 3.1415927F);
            var2.constraintPositionOffsetA.x = PZImGui.sliderFloat("constraintPositionOffsetA.x", var2.constraintPositionOffsetA.x, -0.25F, 0.25F);
            var2.constraintPositionOffsetA.y = PZImGui.sliderFloat("constraintPositionOffsetA.y", var2.constraintPositionOffsetA.y, -0.25F, 0.25F);
            var2.constraintPositionOffsetA.z = PZImGui.sliderFloat("constraintPositionOffsetA.z", var2.constraintPositionOffsetA.z, -0.25F, 0.25F);
            var2.constraintPositionOffsetB.x = PZImGui.sliderFloat("constraintPositionOffsetB.x", var2.constraintPositionOffsetB.x, -0.25F, 0.25F);
            var2.constraintPositionOffsetB.y = PZImGui.sliderFloat("constraintPositionOffsetB.y", var2.constraintPositionOffsetB.y, -0.25F, 0.25F);
            var2.constraintPositionOffsetB.z = PZImGui.sliderFloat("constraintPositionOffsetB.z", var2.constraintPositionOffsetB.z, -0.25F, 0.25F);
            var2.constraintLimit.x = PZImGui.sliderFloat("constraintLimit.x", var2.constraintLimit.x, -3.1415927F, 3.1415927F);
            var2.constraintLimit.y = PZImGui.sliderFloat("constraintLimit.y", var2.constraintLimit.y, -3.1415927F, 3.1415927F);
            var2.constraintLimit.z = PZImGui.sliderFloat("constraintLimit.z", var2.constraintLimit.z, -3.1415927F, 3.1415927F);
            ImGui.endChild();
         }
      }

      if (RagdollBuilder.instance.isInitialized()) {
         if (PZImGui.button("Reset To Default")) {
            RagdollScript.resetConstraintsToDefaultValues();
         }

         if (PZImGui.button("Update toBullet()")) {
            RagdollScript.toBullet(true);
         }
      }

   }

   private void skeletonTab() {
      if (ImGui.treeNode("Skeleton")) {
         SkeletonBone[] var1 = SkeletonBone.all();
         SkinningBoneHierarchy var2 = RagdollBuilder.instance.getSkeletonBoneHiearchy();
         SkeletonBone[] var3 = var1;
         int var4 = var1.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            SkeletonBone var6 = var3[var5];
            SkinningBone var7 = var2.getBone(var6);
            if (var7 != null) {
               SkeletonBone var8 = var7.getParentSkeletonBone();
               if (ImGui.treeNode(var8.name())) {
                  StringBuilder var9 = new StringBuilder();
                  var9.append("parent: ").append(var7.Parent).append(", childBone: ").append(var7);
                  ImGui.text(var9.toString());
                  ImGui.treePop();
               }
            }
         }

         ImGui.treePop();
      }

   }

   private void boneToBodyPartAnchorsTab() {
      Iterator var1 = RagdollScript.getRagdollAnchorList().iterator();

      while(var1.hasNext()) {
         RagdollAnchor var2 = (RagdollAnchor)var1.next();
         if (PZImGui.collapsingHeader(SkeletonBone.values()[var2.bone].toString())) {
            ImGui.beginChild(SkeletonBone.values()[var2.bone].toString());
            boolean var3 = var2.enabled;
            ImBoolean var4 = new ImBoolean(var3);
            Objects.requireNonNull(var4);
            Supplier var10002 = var4::get;
            Objects.requireNonNull(var4);
            var2.enabled = PZImGui.checkbox("Enabled", var10002, var4::set);
            if (var3) {
               int var5 = var2.bodyPart;
               ImInt var6 = new ImInt(var5);
               if (PZImGui.combo("Body Part", var6, this.bodyPartArray)) {
                  var2.bodyPart = var6.get();
               }
            }

            boolean var7 = var2.reverse;
            ImBoolean var8 = new ImBoolean(var7);
            Objects.requireNonNull(var8);
            var10002 = var8::get;
            Objects.requireNonNull(var8);
            var2.reverse = PZImGui.checkbox("Reverse", var10002, var8::set);
            ImGui.endChild();
         }
      }

      if (RagdollBuilder.instance.isInitialized()) {
         if (PZImGui.button("Reset To Default")) {
            RagdollScript.resetAnchorsToDefaultValues();
         }

         if (PZImGui.button("Update toBullet()")) {
            RagdollScript.toBullet(true);
         }
      }

   }
}
