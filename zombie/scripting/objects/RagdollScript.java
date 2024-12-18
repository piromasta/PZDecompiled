package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.physics.Bullet;
import zombie.network.GameServer;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public final class RagdollScript extends BaseScriptObject {
   private static final int NumberOfRagdollConstraintAttributes = 22;
   private static final int NumberOfRagdollAnchorAttributes = 4;
   private static final ArrayList<RagdollConstraint> ragdollConstraintList = new ArrayList();
   private static final ArrayList<RagdollAnchor> ragdollAnchorList = new ArrayList();

   public RagdollScript() {
      super(ScriptType.Ragdoll);
   }

   public static ArrayList<RagdollConstraint> getRagdollConstraintList() {
      return ragdollConstraintList;
   }

   public static ArrayList<RagdollAnchor> getRagdollAnchorList() {
      return ragdollAnchorList;
   }

   public static void toBullet(boolean var0) {
      if (!GameServer.bServer) {
         uploadConstraints(var0);
         uploadAnchors(var0);
      }
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      if (var3.id.contains("_constraint")) {
         this.loadConstraints(var3);
      } else if (var3.id.contains("_anchor")) {
         this.loadAnchors(var3);
      }

   }

   public static void resetConstraintsToDefaultValues() {
      Iterator var0 = ragdollConstraintList.iterator();

      while(var0.hasNext()) {
         RagdollConstraint var1 = (RagdollConstraint)var0.next();
         var1.constraintAxisA.set(var1.defaultConstraintAxisA);
         var1.constraintAxisB.set(var1.defaultConstraintAxisB);
         var1.constraintPositionOffsetA.set(var1.defaultConstraintPositionOffsetA);
         var1.constraintPositionOffsetB.set(var1.defaultConstraintPositionOffsetB);
         var1.constraintLimit.set(var1.defaultConstraintLimit);
         var1.constraintLimitExtended.set(var1.defaultConstraintLimitExtended);
      }

      toBullet(true);
   }

   public static void resetAnchorsToDefaultValues() {
      RagdollAnchor var1;
      for(Iterator var0 = ragdollAnchorList.iterator(); var0.hasNext(); var1.enabled = var1.defaultEnabled) {
         var1 = (RagdollAnchor)var0.next();
         var1.bodyPart = var1.defaultBodyPart;
         var1.reverse = var1.defaultReverse;
      }

      toBullet(true);
   }

   private void loadConstraints(ScriptParser.Block var1) {
      RagdollConstraint var2 = new RagdollConstraint();
      Iterator var3 = var1.elements.iterator();

      while(var3.hasNext()) {
         ScriptParser.BlockElement var4 = (ScriptParser.BlockElement)var3.next();
         if (var4.asValue() != null) {
            String[] var5 = var4.asValue().string.split("=");
            String var6 = var5[0].trim();
            String var7 = var5[1].trim();
            if ("joint".equals(var6)) {
               var2.joint = Integer.parseInt(var7);
            } else if ("constraintType".equals(var6)) {
               var2.constraintType = Integer.parseInt(var7);
            } else if ("constraintPartA".equals(var6)) {
               var2.constraintPartA = Integer.parseInt(var7);
            } else if ("constraintPartB".equals(var6)) {
               var2.constraintPartB = Integer.parseInt(var7);
            } else if ("constraintAxisA".equals(var6)) {
               this.LoadVector3(var7, var2.constraintAxisA);
               this.LoadVector3(var7, var2.defaultConstraintAxisA);
            } else if ("constraintAxisB".equals(var6)) {
               this.LoadVector3(var7, var2.constraintAxisB);
               this.LoadVector3(var7, var2.defaultConstraintAxisB);
            } else if ("constraintPositionOffsetA".equals(var6)) {
               this.LoadVector3(var7, var2.constraintPositionOffsetA);
               this.LoadVector3(var7, var2.defaultConstraintPositionOffsetA);
            } else if ("constraintPositionOffsetB".equals(var6)) {
               this.LoadVector3(var7, var2.constraintPositionOffsetB);
               this.LoadVector3(var7, var2.defaultConstraintPositionOffsetB);
            } else if ("constraintLimit".equals(var6)) {
               this.LoadVector3(var7, var2.constraintLimit);
               this.LoadVector3(var7, var2.defaultConstraintLimit);
            } else if ("constraintLimitExtended".equals(var6)) {
               this.LoadVector3(var7, var2.constraintLimitExtended);
               this.LoadVector3(var7, var2.defaultConstraintLimitExtended);
            }
         }
      }

      ragdollConstraintList.add(var2);
   }

   private void loadAnchors(ScriptParser.Block var1) {
      RagdollAnchor var2 = new RagdollAnchor();
      Iterator var3 = var1.elements.iterator();

      while(var3.hasNext()) {
         ScriptParser.BlockElement var4 = (ScriptParser.BlockElement)var3.next();
         if (var4.asValue() != null) {
            String[] var5 = var4.asValue().string.split("=");
            String var6 = var5[0].trim();
            String var7 = var5[1].trim();
            if ("bone".equals(var6)) {
               var2.bone = Integer.parseInt(var7);
            } else if ("bodyPart".equals(var6)) {
               var2.defaultBodyPart = var2.bodyPart = Integer.parseInt(var7);
            } else if ("enabled".equals(var6)) {
               var2.defaultEnabled = var2.enabled = Boolean.parseBoolean(var7);
            } else if ("reverse".equals(var6)) {
               var2.defaultReverse = var2.reverse = Boolean.parseBoolean(var7);
            }
         }
      }

      ragdollAnchorList.add(var2);
   }

   private static void uploadConstraints(boolean var0) {
      int var1 = 0;
      float[] var2 = new float[22 * ragdollConstraintList.size()];

      RagdollConstraint var4;
      for(Iterator var3 = ragdollConstraintList.iterator(); var3.hasNext(); var2[var1++] = var4.constraintLimitExtended.z) {
         var4 = (RagdollConstraint)var3.next();
         var2[var1++] = (float)var4.joint;
         var2[var1++] = (float)var4.constraintType;
         var2[var1++] = (float)var4.constraintPartA;
         var2[var1++] = (float)var4.constraintPartB;
         var2[var1++] = var4.constraintAxisA.x;
         var2[var1++] = var4.constraintAxisA.y;
         var2[var1++] = var4.constraintAxisA.z;
         var2[var1++] = var4.constraintAxisB.x;
         var2[var1++] = var4.constraintAxisB.y;
         var2[var1++] = var4.constraintAxisB.z;
         var2[var1++] = var4.constraintPositionOffsetA.x;
         var2[var1++] = var4.constraintPositionOffsetA.y;
         var2[var1++] = var4.constraintPositionOffsetA.z;
         var2[var1++] = var4.constraintPositionOffsetB.x;
         var2[var1++] = var4.constraintPositionOffsetB.y;
         var2[var1++] = var4.constraintPositionOffsetB.z;
         var2[var1++] = var4.constraintLimit.x;
         var2[var1++] = var4.constraintLimit.y;
         var2[var1++] = var4.constraintLimit.z;
         var2[var1++] = var4.constraintLimitExtended.x;
         var2[var1++] = var4.constraintLimitExtended.y;
      }

      Bullet.defineRagdollConstraints(var2, var0);
   }

   private static void uploadAnchors(boolean var0) {
      int var1 = 0;
      float[] var2 = new float[4 * ragdollAnchorList.size()];

      RagdollAnchor var4;
      for(Iterator var3 = ragdollAnchorList.iterator(); var3.hasNext(); var2[var1++] = var4.enabled ? 1.0F : 0.0F) {
         var4 = (RagdollAnchor)var3.next();
         var2[var1++] = (float)var4.bone;
         var2[var1++] = (float)var4.bodyPart;
         var2[var1++] = var4.reverse ? 1.0F : 0.0F;
      }

      Bullet.defineRagdollAnchors(var2, var0);
   }
}
