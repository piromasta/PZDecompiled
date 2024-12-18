package zombie.world.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import zombie.world.DictionaryInfo;
import zombie.world.DictionaryScriptInfo;
import zombie.world.ItemInfo;

public class Log {
   public Log() {
   }

   public static class VersionChangedScript extends BaseScriptLog {
      private final long oldVersion;

      public VersionChangedScript(DictionaryScriptInfo var1, long var2) {
         super(var1);
         this.oldVersion = var2;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"script_version\", old_version = " + this.oldVersion + "," + this.getScriptString() + " }" + System.lineSeparator());
      }
   }

   public static class RegisterScript extends BaseScriptLog {
      public RegisterScript(DictionaryScriptInfo var1) {
         super(var1);
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"reg_script\", " + this.getScriptString() + " }" + System.lineSeparator());
      }
   }

   public abstract static class BaseScriptLog extends BaseLog {
      protected final DictionaryScriptInfo info;

      public BaseScriptLog(DictionaryScriptInfo var1) {
         this.info = var1;
      }

      abstract void saveAsText(FileWriter var1, String var2) throws IOException;

      protected String getScriptString() {
         String var10000 = this.info.getName();
         return "name = \"" + var10000 + "\", registeryID = " + this.info.getRegistryID() + ", version = " + this.info.getVersion() + ", isLoaded = " + this.info.isLoaded();
      }
   }

   public static class RegisterString extends BaseLog {
      protected static final String reg = "reg_str";
      protected static final String unreg = "un_reg_str";
      protected final String registerName;
      protected final String s;
      protected final int ID;
      protected final boolean register;

      public RegisterString(String var1, String var2, int var3) {
         this(var1, var2, var3, true);
      }

      public RegisterString(String var1, String var2, int var3, boolean var4) {
         this.registerName = var1;
         this.s = var2;
         this.ID = var3;
         this.register = var4;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"" + (this.register ? "reg_str" : "un_reg_str") + "\", register = " + this.registerName + ", id = " + this.ID + ", value = \"" + this.s + "\" }" + System.lineSeparator());
      }
   }

   public static class ModIDChangedItem extends BaseItemLog {
      protected final String oldModID;
      protected final String newModID;

      public ModIDChangedItem(DictionaryInfo<?> var1, String var2, String var3) {
         super(var1);
         this.oldModID = var2;
         this.newModID = var3;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"modchange_" + this.getTypeTag() + "\", oldModID = \"" + this.oldModID + "\", " + this.getItemString() + " }" + System.lineSeparator());
      }
   }

   public static class RemovedItem extends BaseItemLog {
      protected final boolean isScriptMissing;

      public RemovedItem(DictionaryInfo<?> var1, boolean var2) {
         super(var1);
         this.isScriptMissing = var2;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"removed_" + this.getTypeTag() + "\", scriptMissing = " + this.isScriptMissing + ", " + this.getItemString() + " }" + System.lineSeparator());
      }
   }

   public static class ObsoleteItem extends BaseItemLog {
      public ObsoleteItem(DictionaryInfo<?> var1) {
         super(var1);
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"obsolete_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
      }
   }

   public static class ReinstateItem extends BaseItemLog {
      public ReinstateItem(DictionaryInfo<?> var1) {
         super(var1);
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"reinstate_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
      }
   }

   public static class RegisterItem extends BaseItemLog {
      public RegisterItem(DictionaryInfo<?> var1) {
         super(var1);
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"reg_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
      }
   }

   public abstract static class BaseItemLog extends BaseLog {
      protected final DictionaryInfo<?> itemInfo;
      protected final boolean isItem;

      public BaseItemLog(DictionaryInfo<?> var1) {
         this.itemInfo = var1;
         this.isItem = this.itemInfo instanceof ItemInfo;
      }

      public final String getTypeTag() {
         return this.isItem ? "item" : "entity";
      }

      abstract void saveAsText(FileWriter var1, String var2) throws IOException;

      protected String getItemString() {
         String var10000 = this.itemInfo.getFullType();
         return "fulltype = \"" + var10000 + "\", registeryID = " + this.itemInfo.getRegistryID() + ", existsVanilla = " + this.itemInfo.isExistsAsVanilla() + ", isModded = " + this.itemInfo.isModded() + ", modID = \"" + this.itemInfo.getModID() + "\", obsolete = " + this.itemInfo.isObsolete() + ", removed = " + this.itemInfo.isRemoved() + ", isLoaded = " + this.itemInfo.isLoaded();
      }
   }

   public static class RegisterObject extends BaseLog {
      protected final String objectName;
      protected final int ID;

      public RegisterObject(String var1, int var2) {
         this.objectName = var1;
         this.ID = var2;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{ type = \"reg_obj\", id = " + this.ID + ", obj = \"" + this.objectName + "\" }" + System.lineSeparator());
      }
   }

   public static class Comment extends BaseLog {
      protected String txt;

      public Comment(String var1) {
         this.ignoreSaveCheck = true;
         this.txt = var1;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "-- " + this.txt + System.lineSeparator());
      }
   }

   public static class Info extends BaseLog {
      protected final List<String> mods;
      protected final String timeStamp;
      protected final String saveWorld;
      protected final int worldVersion;
      public boolean HasErrored = false;

      public Info(String var1, String var2, int var3, List<String> var4) {
         this.ignoreSaveCheck = true;
         this.timeStamp = var1;
         this.saveWorld = var2;
         this.worldVersion = var3;
         this.mods = var4;
      }

      public void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + "{" + System.lineSeparator());
         var1.write(var2 + "\ttype = \"info\"," + System.lineSeparator());
         var1.write(var2 + "\ttimeStamp = \"" + this.timeStamp + "\"," + System.lineSeparator());
         var1.write(var2 + "\tsaveWorld = \"" + this.saveWorld + "\"," + System.lineSeparator());
         var1.write(var2 + "\tworldVersion = " + this.worldVersion + "," + System.lineSeparator());
         var1.write(var2 + "\thasErrored = " + this.HasErrored + "," + System.lineSeparator());
         var1.write(var2 + "\titemMods = {" + System.lineSeparator());

         for(int var3 = 0; var3 < this.mods.size(); ++var3) {
            var1.write(var2 + "\t\t\"" + (String)this.mods.get(var3) + "\"," + System.lineSeparator());
         }

         var1.write(var2 + "\t}," + System.lineSeparator());
         var1.write(var2 + "}," + System.lineSeparator());
      }
   }

   public abstract static class BaseLog {
      protected boolean ignoreSaveCheck = false;

      public BaseLog() {
      }

      public boolean isIgnoreSaveCheck() {
         return this.ignoreSaveCheck;
      }

      abstract void saveAsText(FileWriter var1, String var2) throws IOException;
   }
}
