package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucketCollection<E extends BaseScriptObject> {
   private final ScriptManager scriptManager;
   private final ScriptType scriptType;
   private final HashMap<ScriptModule, ScriptBucket<E>> map = new HashMap();
   private final ArrayList<ScriptModule> scriptModules = new ArrayList();
   private final ArrayList<ScriptBucket<E>> scriptBuckets = new ArrayList();
   private final ArrayList<E> allScripts = new ArrayList();
   private final HashMap<String, E> fullTypeToScriptMap = new HashMap();
   protected final ArrayList<String> loadFiles = new ArrayList();

   public ScriptBucketCollection(ScriptManager var1, ScriptType var2) {
      this.scriptManager = var1;
      this.scriptType = var2;
   }

   public ScriptType getScriptType() {
      return this.scriptType;
   }

   public boolean isTemplate() {
      return this.scriptType.isTemplate();
   }

   public void reset() {
      this.map.clear();
      this.scriptModules.clear();
      this.scriptBuckets.clear();
      this.allScripts.clear();
      this.fullTypeToScriptMap.clear();
      this.loadFiles.clear();
   }

   public boolean hasFullType(String var1) {
      return this.fullTypeToScriptMap.containsKey(var1);
   }

   public E getFullType(String var1) {
      return (BaseScriptObject)this.fullTypeToScriptMap.get(var1);
   }

   public HashMap<String, E> getFullTypeToScriptMap() {
      return this.fullTypeToScriptMap;
   }

   public void setReloadBuckets(boolean var1) {
      Iterator var2 = this.scriptBuckets.iterator();

      while(var2.hasNext()) {
         ScriptBucket var3 = (ScriptBucket)var2.next();
         var3.setReload(var1);
      }

   }

   public void registerModule(ScriptModule var1) {
      this.scriptModules.add(var1);
      ScriptBucket var2 = this.getBucketFromModule(var1);
      if (this.scriptType != var2.scriptType) {
         throw new RuntimeException("ScriptType does not match bucket ScriptType");
      } else {
         this.scriptBuckets.add(var2);
         this.map.put(var1, var2);
      }
   }

   public abstract ScriptBucket<E> getBucketFromModule(ScriptModule var1);

   public E getScript(String var1) {
      if (this.scriptType.hasFlag(ScriptType.Flags.CacheFullType) && var1.contains(".") && this.fullTypeToScriptMap.containsKey(var1)) {
         return (BaseScriptObject)this.fullTypeToScriptMap.get(var1);
      } else {
         ScriptModule var2;
         if (!var1.contains(".")) {
            var2 = this.scriptManager.getModule("Base");
         } else {
            var2 = this.scriptManager.getModule(var1);
         }

         if (var2 == null) {
            return null;
         } else {
            ScriptBucket var3 = (ScriptBucket)this.map.get(var2);
            return var3.get(ScriptManager.getItemName(var1));
         }
      }
   }

   public ArrayList<E> getAllScripts() {
      if (!this.scriptType.hasFlag(ScriptType.Flags.Clear) && !this.allScripts.isEmpty()) {
         return this.allScripts;
      } else {
         this.allScripts.clear();

         for(int var1 = 0; var1 < this.scriptBuckets.size(); ++var1) {
            ScriptBucket var2 = (ScriptBucket)this.scriptBuckets.get(var1);
            if (!var2.module.disabled) {
               if (this.scriptType.hasFlag(ScriptType.Flags.FromList)) {
                  this.allScripts.addAll(var2.scriptList);
               } else {
                  this.allScripts.addAll(var2.scriptMap.values());
               }
            }
         }

         this.onSortAllScripts(this.allScripts);
         return this.allScripts;
      }
   }

   public void onSortAllScripts(ArrayList<E> var1) {
   }

   public void LoadScripts(ScriptLoadMode var1) {
      String var10000 = this.scriptType.toString();
      DebugLog.log("Load Scripts: " + var10000 + ", loadMode = " + var1);
      Iterator var2 = this.scriptBuckets.iterator();

      while(var2.hasNext()) {
         ScriptBucket var3 = (ScriptBucket)var2.next();
         var3.LoadScripts(var1);
         Iterator var4 = var3.loadFiles.iterator();

         while(var4.hasNext()) {
            String var5 = (String)var4.next();
            if (!this.loadFiles.contains(var5)) {
               this.loadFiles.add(var5);
            }
         }

         var4 = var3.getScriptList().iterator();

         while(var4.hasNext()) {
            BaseScriptObject var6 = (BaseScriptObject)var4.next();
            this.fullTypeToScriptMap.put(var6.getScriptObjectFullType(), var6);
         }
      }

   }

   public void PreReloadScripts() throws Exception {
      ScriptManager.println(this.scriptType, "<- PreReloadScripts ->");
      Iterator var1 = this.getAllScripts().iterator();

      while(var1.hasNext()) {
         BaseScriptObject var2 = (BaseScriptObject)var1.next();
         var2.PreReload();
      }

      this.allScripts.clear();
   }

   public void PostLoadScripts(ScriptLoadMode var1) throws Exception {
   }

   public boolean hasLoadErrors() {
      return this.hasLoadErrors(false);
   }

   public boolean hasLoadErrors(boolean var1) {
      Iterator var2 = this.scriptBuckets.iterator();

      ScriptBucket var3;
      do {
         do {
            if (!var2.hasNext()) {
               return false;
            }

            var3 = (ScriptBucket)var2.next();
         } while(!var3.isHasLoadErrors());
      } while(var1 && !this.scriptType.isCritical());

      return true;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      ScriptManager.println(this.scriptType, "<- OnScriptsLoaded ->");
      Iterator var2 = this.getAllScripts().iterator();

      while(var2.hasNext()) {
         BaseScriptObject var3 = (BaseScriptObject)var2.next();
         var3.OnScriptsLoaded(var1);
      }

   }

   public void OnLoadedAfterLua() throws Exception {
      ScriptManager.println(this.scriptType, "<- OnLoadedAfterLua ->");
      Iterator var1 = this.getAllScripts().iterator();

      while(var1.hasNext()) {
         BaseScriptObject var2 = (BaseScriptObject)var1.next();
         var2.OnLoadedAfterLua();
      }

   }

   public void OnPostTileDefinitions() throws Exception {
      ScriptManager.println(this.scriptType, "<- OnPostTileDefinitions ->");
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      ScriptManager.println(this.scriptType, "<- OnPostWorldDictionaryInit ->");
      Iterator var1 = this.getAllScripts().iterator();

      while(var1.hasNext()) {
         BaseScriptObject var2 = (BaseScriptObject)var1.next();
         var2.OnPostWorldDictionaryInit();
      }

   }
}
