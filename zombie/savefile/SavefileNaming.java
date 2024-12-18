package zombie.savefile;

import java.io.File;
import zombie.ZomboidFileSystem;

public final class SavefileNaming {
   public static final String SUBDIR_APOP = "apop";
   public static final String SUBDIR_CHUNKDATA = "chunkdata";
   public static final String SUBDIR_MAP = "map";
   public static final String SUBDIR_ZPOP = "zpop";
   public static final String SUBDIR_METAGRID = "metagrid";

   public SavefileNaming() {
   }

   public static void ensureSubdirectoriesExist(String var0) {
      File var1 = new File(var0);
      ZomboidFileSystem.ensureFolderExists(new File(var1, "apop"));
      ZomboidFileSystem.ensureFolderExists(new File(var1, "chunkdata"));
      ZomboidFileSystem.ensureFolderExists(new File(var1, "map"));
      ZomboidFileSystem.ensureFolderExists(new File(var1, "zpop"));
      ZomboidFileSystem.ensureFolderExists(new File(var1, "metagrid"));
   }
}
