package zombie.core.physics;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;
import java.io.IOException;
import java.util.EnumSet;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.skinnedmodel.model.FileTask_AbstractLoadModel;
import zombie.core.skinnedmodel.model.ModelTxt;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiSceneParams;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.IFileTaskCallback;

public class FileTask_LoadPhysicsShape extends FileTask_AbstractLoadModel {
   PhysicsShape physicsShape;

   public FileTask_LoadPhysicsShape(PhysicsShape var1, FileSystem var2, IFileTaskCallback var3) {
      super(var2, var3, "media/models", "media/models_x");
      this.physicsShape = var1;
   }

   public String getErrorMessage() {
      return this.m_fileName;
   }

   public void done() {
      PhysicsShapeAssetManager.instance.addWatchedFile(this.m_fileName);
      this.physicsShape.m_fullPath = this.m_fileName;
      this.m_fileName = null;
      this.physicsShape = null;
   }

   public String getRawFileName() {
      String var1 = this.physicsShape.getPath().getPath();
      int var2 = var1.indexOf(124);
      return var2 != -1 ? var1.substring(0, var2) : var1;
   }

   private String getMeshName() {
      String var1 = this.physicsShape.getPath().getPath();
      int var2 = var1.indexOf(124);
      return var2 != -1 ? var1.substring(var2 + 1) : null;
   }

   public ProcessedAiScene loadX() throws IOException {
      DebugLog.Asset.debugln("Loading: %s", this.m_fileName);
      EnumSet var1 = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES);
      AiScene var2 = Jassimp.importFile(this.m_fileName, var1);
      JAssImpImporter.LoadMode var3 = JAssImpImporter.LoadMode.StaticMesh;
      ProcessedAiSceneParams var4 = ProcessedAiSceneParams.create();
      var4.scene = var2;
      var4.mode = var3;
      var4.skinnedTo = null;
      var4.meshName = this.getMeshName();
      ProcessedAiScene var5 = ProcessedAiScene.process(var4);
      JAssImpImporter.takeOutTheTrash(var2);
      return var5;
   }

   public ProcessedAiScene loadFBX() throws IOException {
      DebugLog.Asset.debugln("Loading: %s", this.m_fileName);
      EnumSet var1 = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES);
      this.handlePostProcessFlags(var1, this.physicsShape.assetParams.postProcess);
      AiScene var2 = Jassimp.importFile(this.m_fileName, var1);
      JAssImpImporter.LoadMode var3 = JAssImpImporter.LoadMode.StaticMesh;
      Quaternion var4 = new Quaternion();
      Vector4f var5 = new Vector4f(1.0F, 0.0F, 0.0F, -1.5707964F);
      var4.setFromAxisAngle(var5);
      ProcessedAiSceneParams var6 = ProcessedAiSceneParams.create();
      var6.scene = var2;
      var6.mode = var3;
      var6.skinnedTo = null;
      var6.meshName = this.getMeshName();
      var6.animBonesScaleModifier = 0.01F;
      var6.animBonesRotateModifier = var4;
      var6.bAllMeshes = this.physicsShape.assetParams.bAllMeshes;
      ProcessedAiScene var7 = ProcessedAiScene.process(var6);
      JAssImpImporter.takeOutTheTrash(var2);
      return var7;
   }

   public ProcessedAiScene loadGLTF() throws IOException {
      DebugLog.Asset.debugln("Loading: %s", this.m_fileName);
      EnumSet var1 = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES);
      this.handlePostProcessFlags(var1, this.physicsShape.assetParams.postProcess);
      AiScene var2 = Jassimp.importFile(this.m_fileName, var1);
      JAssImpImporter.LoadMode var3 = JAssImpImporter.LoadMode.StaticMesh;
      ProcessedAiSceneParams var4 = ProcessedAiSceneParams.create();
      var4.scene = var2;
      var4.mode = var3;
      var4.skinnedTo = null;
      var4.meshName = this.getMeshName();
      var4.animBonesScaleModifier = 1.0F;
      var4.animBonesRotateModifier = null;
      var4.bAllMeshes = this.physicsShape.assetParams.bAllMeshes;
      ProcessedAiScene var5 = ProcessedAiScene.process(var4);
      JAssImpImporter.takeOutTheTrash(var2);
      return var5;
   }

   public ModelTxt loadTxt() throws IOException {
      throw new IOException("unsupported format");
   }

   private void handlePostProcessFlags(EnumSet<AiPostProcessSteps> var1, String var2) {
      if (var2 != null) {
         String[] var3 = this.physicsShape.assetParams.postProcess.split(";");
         String[] var4 = var3;
         int var5 = var3.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            String var7 = var4[var6];
            if (var7.startsWith("+")) {
               var1.add(AiPostProcessSteps.valueOf(var7.substring(1)));
            } else if (var7.startsWith("-")) {
               var1.remove(AiPostProcessSteps.valueOf(var7.substring(1)));
            }
         }

      }
   }
}
