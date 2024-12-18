package zombie.core.physics;

import java.util.HashSet;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask_RunFileTask;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.util.StringUtils;

public final class PhysicsShapeAssetManager extends AssetManager {
   public static final PhysicsShapeAssetManager instance = new PhysicsShapeAssetManager();
   private final HashSet<String> m_watchedFiles = new HashSet();
   private final PredicatedFileWatcher m_watcher = new PredicatedFileWatcher(PhysicsShapeAssetManager::isWatched, PhysicsShapeAssetManager::watchedFileChanged);

   private PhysicsShapeAssetManager() {
      DebugFileWatcher.instance.add(this.m_watcher);
   }

   protected void startLoading(Asset var1) {
      PhysicsShape var2 = (PhysicsShape)var1;
      FileSystem var3 = this.getOwner().getFileSystem();
      FileTask_LoadPhysicsShape var4 = new FileTask_LoadPhysicsShape(var2, var3, (var2x) -> {
         this.loadCallback(var2, var2x);
      });
      var4.setPriority(6);
      AssetTask_RunFileTask var5 = new AssetTask_RunFileTask(var4, var1);
      this.setTask(var1, var5);
      var5.execute();
   }

   private void loadCallback(PhysicsShape var1, Object var2) {
      if (var2 instanceof ProcessedAiScene) {
         var1.onLoadedX((ProcessedAiScene)var2);
         this.onLoadingSucceeded(var1);
      } else {
         DebugLog.General.warn("Failed to load asset: " + var1.getPath());
         this.onLoadingFailed(var1);
      }

   }

   protected Asset createAsset(AssetPath var1, AssetManager.AssetParams var2) {
      return new PhysicsShape(var1, this, (PhysicsShape.PhysicsShapeAssetParams)var2);
   }

   protected void destroyAsset(Asset var1) {
   }

   private static boolean isWatched(String var0) {
      if (!StringUtils.endsWithIgnoreCase(var0, ".fbx") && !StringUtils.endsWithIgnoreCase(var0, ".glb") && !StringUtils.endsWithIgnoreCase(var0, ".x")) {
         return false;
      } else {
         String var1 = ZomboidFileSystem.instance.getString(var0);
         return instance.m_watchedFiles.contains(var1);
      }
   }

   private static void watchedFileChanged(String var0) {
      DebugLog.Asset.printf("%s changed\n", var0);
      String var1 = ZomboidFileSystem.instance.getString(var0);
      instance.getAssetTable().forEachValue((var1x) -> {
         PhysicsShape var2 = (PhysicsShape)var1x;
         if (!var2.isEmpty() && var1.equalsIgnoreCase(var2.m_fullPath)) {
            PhysicsShape.PhysicsShapeAssetParams var3 = new PhysicsShape.PhysicsShapeAssetParams();
            var3.postProcess = var2.postProcess;
            var3.bAllMeshes = var2.bAllMeshes;
            instance.reload(var1x, var3);
         }

         return true;
      });
   }

   public void addWatchedFile(String var1) {
      this.m_watchedFiles.add(var1);
   }
}
