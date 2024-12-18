package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugIgnoreField;
import zombie.debug.objects.DebugNonRecursive;
import zombie.iso.Vector3;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;
import zombie.util.hash.PZHash;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
public abstract class BaseScriptObject {
   @DebugIgnoreField
   private final ArrayList<String> loadedScriptBodies = new ArrayList(0);
   @DebugIgnoreField
   private ArrayList<String> linesCache;
   private final ScriptType scriptObjectType;
   @DebugNonRecursive
   private ScriptModule module = null;
   private String scriptObjectName;
   @DebugNonRecursive
   private BaseScriptObject parentScript;
   private String fullTypeCache;
   private boolean fullTypeDirty = true;
   private long scriptVersion = -1L;
   protected boolean enabled = true;
   protected boolean debugOnly = false;

   protected BaseScriptObject(ScriptType var1) {
      this.scriptObjectType = var1;
   }

   public final String debugString() {
      ScriptType var10000 = this.scriptObjectType;
      return "[type=" + var10000 + ", module=" + (this.module != null ? this.module.getName() : "null") + ", name=" + this.scriptObjectName + ", fulltype=" + this.getScriptObjectFullType() + "]";
   }

   /** @deprecated */
   @Deprecated
   public void getVersion(IVersionHash var1) {
      throw new RuntimeException("Not implemented. class = " + this.getClass().getSimpleName());
   }

   public long getScriptVersion() {
      return this.scriptVersion;
   }

   public void calculateScriptVersion() {
      int var1 = this.getScriptObjectFullType().length();

      String var2;
      for(int var3 = 0; var3 < this.loadedScriptBodies.size(); ++var3) {
         var2 = (String)this.loadedScriptBodies.get(var3);
         var1 += var2.length();
      }

      StringBuilder var5 = new StringBuilder(var1);
      var5.append(this.getScriptObjectFullType());

      for(int var4 = 0; var4 < this.loadedScriptBodies.size(); ++var4) {
         var2 = (String)this.loadedScriptBodies.get(var4);
         var5.append(var2);
      }

      this.scriptVersion = PZHash.murmur_64(var5.toString());
   }

   public ScriptModule getModule() {
      return this.module;
   }

   public void setModule(ScriptModule var1) {
      this.module = var1;
      this.fullTypeDirty = true;
   }

   public final boolean isEnabled() {
      return this.enabled;
   }

   public final boolean isDebugOnly() {
      return this.debugOnly;
   }

   public final void setParent(BaseScriptObject var1) {
      this.parentScript = var1;
      if (var1 != null) {
         this.setModule(var1.getModule());
      }

   }

   public final BaseScriptObject getParent() {
      return this.parentScript;
   }

   public final ScriptType getScriptObjectType() {
      return this.scriptObjectType;
   }

   public final String getScriptObjectName() {
      return this.scriptObjectName;
   }

   public final String getScriptObjectFullType() {
      if (!this.fullTypeDirty && this.fullTypeCache != null) {
         return this.fullTypeCache;
      } else {
         this.fullTypeDirty = false;
         String var1 = this.module != null && this.module.name != null ? this.module.getName() : null;
         String var2 = this.scriptObjectName != null ? this.scriptObjectName : null;
         if (var1 != null && var2 != null) {
            this.fullTypeCache = var1 + "." + var2;
            return this.fullTypeCache;
         } else {
            throw new RuntimeException("[" + this.scriptObjectType + "] Module or name missing, module: " + var1 + ", script: " + var2);
         }
      }
   }

   public final void resetLoadedScriptBodies() {
      this.loadedScriptBodies.clear();
   }

   public final void addLoadedScriptBody(String var1, String var2) {
      this.loadedScriptBodies.add(var1);
      this.loadedScriptBodies.add(var2);
   }

   public final ArrayList<String> getLoadedScriptBodies() {
      return this.loadedScriptBodies;
   }

   public final int getLoadedScriptBodyCount() {
      return this.loadedScriptBodies.size();
   }

   public boolean getObsolete() {
      return false;
   }

   public void InitLoadPP(String var1) {
      this.scriptObjectName = var1;
      this.fullTypeDirty = true;
   }

   public final void LoadCommonBlock(String var1) throws Exception {
      ScriptParser.Block var2 = ScriptParser.parse(var1);
      var2 = (ScriptParser.Block)var2.children.get(0);
      this.LoadCommonBlock(var2);
   }

   public final void LoadCommonBlock(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if (var4.equalsIgnoreCase("enabled")) {
               this.enabled = var5.equalsIgnoreCase("true");
            } else if (var4.equalsIgnoreCase("debugOnly")) {
               this.debugOnly = var5.equalsIgnoreCase("true");
            }
         }
      }

   }

   public void Load(String var1, String var2) throws Exception {
      if (Core.bDebug) {
         throw new RuntimeException("Load(name,totalFile) not overridden. [" + this.getClass().getSimpleName() + "]");
      } else {
         DebugLog.General.warn("Load(name,totalFile) not overridden. [" + this.getClass().getSimpleName() + "]");
      }
   }

   public void PreReload() {
   }

   public void reset() {
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   public ArrayList<String> getScriptLines() {
      if (this.linesCache == null) {
         this.linesCache = new ArrayList();
         this.getAllScriptLines(this.linesCache);
      }

      return this.linesCache;
   }

   public final ArrayList<String> getAllScriptLines(ArrayList<String> var1) {
      if (this.loadedScriptBodies.size() < 2) {
         return var1;
      } else {
         for(int var2 = this.loadedScriptBodies.size() - 2; var2 >= 0; var2 -= 2) {
            Object var10001 = this.loadedScriptBodies.get(var2);
            var1.add("/* SCRIPT BODY: " + (String)var10001 + " */");
            String[] var3 = ((String)this.loadedScriptBodies.get(var2 + 1)).split("\\r?\\n");
            int var4 = 0;
            String[] var5 = var3;
            int var6 = var3.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               String var8 = var5[var7];
               if (StringUtils.isNullOrWhitespace(var8)) {
                  ++var4;
               } else {
                  var4 = 0;
               }

               if (var4 < 2) {
                  var1.add(var8);
               }
            }
         }

         return var1;
      }
   }

   public final ArrayList<String> getBodyScriptLines(int var1, ArrayList<String> var2) {
      var1 = var1 * 2 + 1;
      if (var1 >= 0 && var1 < this.loadedScriptBodies.size()) {
         String[] var3 = ((String)this.loadedScriptBodies.get(var1)).split("\\r?\\n");
         int var4 = 0;
         String[] var5 = var3;
         int var6 = var3.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String var8 = var5[var7];
            if (StringUtils.isNullOrWhitespace(var8)) {
               ++var4;
            } else {
               var4 = 0;
            }

            if (var4 < 2) {
               var2.add(var8);
            }
         }
      }

      return var2;
   }

   protected void LoadVector3(String var1, Vector3 var2) {
      String[] var3 = var1.split(" ");
      var2.set(Float.parseFloat(var3[0]), Float.parseFloat(var3[1]), Float.parseFloat(var3[2]));
   }
}
