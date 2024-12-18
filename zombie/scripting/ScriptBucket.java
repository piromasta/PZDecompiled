package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucket<E extends BaseScriptObject> {
   private static String currentScriptObject;
   private final HashMap<String, LoadData<E>> loadData;
   protected final ArrayList<String> loadFiles;
   protected final HashSet<String> dotInName;
   protected final ArrayList<E> scriptList;
   protected final Map<String, E> scriptMap;
   protected final ScriptModule module;
   protected final ScriptType scriptType;
   private boolean reload;
   protected boolean hasLoadErrors;
   private boolean verbose;

   public static final String getCurrentScriptObject() {
      return currentScriptObject;
   }

   public ScriptBucket(ScriptModule var1, ScriptType var2) {
      this(var1, var2, new HashMap());
   }

   public ScriptBucket(ScriptModule var1, ScriptType var2, Map<String, E> var3) {
      this.loadData = new HashMap();
      this.loadFiles = new ArrayList();
      this.dotInName = new HashSet();
      this.scriptList = new ArrayList();
      this.reload = false;
      this.hasLoadErrors = false;
      this.verbose = false;
      this.module = var1;
      this.scriptType = var2;
      if (!(this instanceof Template) && var2.isTemplate()) {
         throw new RuntimeException("ScriptType '" + var2 + "' should not be template!");
      } else {
         if (var3 != null) {
            this.scriptMap = var3;
         } else {
            this.scriptMap = new HashMap();
         }

      }
   }

   public ScriptType getScriptType() {
      return this.scriptType;
   }

   protected void setReload(boolean var1) {
      this.reload = var1;
   }

   public boolean isVerbose() {
      return this.verbose || this.scriptType.isVerbose();
   }

   public void setVerbose(boolean var1) {
      this.verbose = var1;
   }

   public boolean isHasLoadErrors() {
      return this.hasLoadErrors;
   }

   protected void setLoadError() {
      this.hasLoadErrors = true;
   }

   public ArrayList<E> getScriptList() {
      return this.scriptList;
   }

   public Map<String, E> getScriptMap() {
      return this.scriptMap;
   }

   public void reset() {
      this.loadData.clear();
      this.loadFiles.clear();
      this.dotInName.clear();
      this.scriptList.clear();
      this.scriptMap.clear();
   }

   public abstract E createInstance(ScriptModule var1, String var2, String var3);

   public boolean CreateFromTokenPP(ScriptLoadMode var1, String var2, String var3) {
      try {
         if (this.scriptType.getScriptTag().equals(var2)) {
            String[] var4 = var3.split("[{}]");
            String var5 = var4[0];
            var5 = var5.replace(this.scriptType.getScriptTag(), "");
            var5 = var5.trim();
            if (var1 == ScriptLoadMode.Init && !this.loadFiles.contains(ScriptManager.instance.currentFileName)) {
               this.loadFiles.add(ScriptManager.instance.currentFileName);
            }

            String var10001;
            if (this.loadData.containsKey(var5)) {
               LoadData var9 = (LoadData)this.loadData.get(var5);
               if (var1 == ScriptLoadMode.Init) {
                  var9.reloaded = false;
                  var9.script.InitLoadPP(var5);
                  var9.scriptBodies.add(var3);
                  var9.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), var3);
                  var10001 = var9.name;
                  ScriptManager.println(this.scriptType, ": Add ScriptBody: '" + var10001 + "' " + var9.script.debugString());
               } else if (var1 == ScriptLoadMode.Reload && this.reload) {
                  if (this.scriptType.hasFlag(ScriptType.Flags.NewInstanceOnReload)) {
                     BaseScriptObject var10 = this.createInstance(this.module, var5, var3);
                     var10.setModule(this.module);
                     var10.InitLoadPP(var5);
                     var9 = new LoadData(var10);
                     var9.scriptBodies.add(var3);
                     var9.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), var3);
                     var9.reloaded = true;
                     var9.addedOnReload = false;
                     this.loadData.put(var9.name, var9);
                     return true;
                  }

                  if (!var9.reloaded && var9.scriptBodies.size() > 0) {
                     var9.scriptBodies.clear();
                     var9.script.resetLoadedScriptBodies();
                  }

                  var9.reloaded = true;
                  var9.scriptBodies.add(var3);
                  var9.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), var3);
                  var10001 = var9.name;
                  ScriptManager.println(this.scriptType, ": Reload ScriptBody: '" + var10001 + "' " + var9.script.debugString());
               }
            } else if (var1 != ScriptLoadMode.Init && !this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
               DebugLog.General.warn("Found new script but was unable to load, possibly due to not being allowed during reload...");
               DebugLog.log(">>> : Load ScriptBody: '" + var5 + "', File: " + ScriptManager.instance.currentFileName);
               if (!this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
                  DebugLog.log(">>> : Discovery of new scripts during reload not allowed for scripts of type: " + this.scriptType);
                  if (Core.bDebug) {
                     throw new Exception("Not allowed");
                  }
               }
            } else {
               BaseScriptObject var6 = this.createInstance(this.module, var5, var3);
               var6.setModule(this.module);
               var6.InitLoadPP(var5);
               LoadData var7 = new LoadData(var6);
               var7.scriptBodies.add(var3);
               var7.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), var3);
               var7.reloaded = var1 == ScriptLoadMode.Reload;
               var7.addedOnReload = var1 == ScriptLoadMode.Reload;
               this.loadData.put(var7.name, var7);
               var10001 = var7.name;
               ScriptManager.println(this.scriptType, ": New ScriptBody: '" + var10001 + "' " + var7.script.debugString());
            }

            return true;
         }
      } catch (Exception var8) {
         ExceptionLogger.logException(var8);
         this.hasLoadErrors = true;
      }

      return false;
   }

   public void LoadScripts(ScriptLoadMode var1) {
      Iterator var2 = this.loadData.values().iterator();

      while(var2.hasNext()) {
         LoadData var3 = (LoadData)var2.next();

         try {
            currentScriptObject = var3.script != null ? var3.script.getScriptObjectFullType() : null;
            if (var1 != ScriptLoadMode.Reload || var3.reloaded) {
               var3.reloaded = false;
               BaseScriptObject var4 = var3.script;
               DebugLogStream var10000;
               String var10001;
               if (this.isVerbose()) {
                  var10000 = DebugLog.General;
                  var10001 = this.scriptType.getScriptTag();
                  var10000.debugln("[" + var10001 + "] load script = " + var4.getScriptObjectName());
               }

               if (var1 == ScriptLoadMode.Reload && this.scriptType.hasFlag(ScriptType.Flags.ResetOnceOnReload)) {
                  var4.reset();
               }

               int var5 = var1 == ScriptLoadMode.Reload ? 0 : 1;

               for(int var6 = 0; var6 < var3.scriptBodies.size(); ++var6) {
                  String var7 = (String)var3.scriptBodies.get(var6);

                  try {
                     if (this.scriptType.hasFlag(ScriptType.Flags.ResetExisting) && var6 >= var5) {
                        var4.reset();
                     }

                     var4.Load(var3.name, var7);
                     ScriptManager.println(this.scriptType, " - Load: '" + var3.name + "' " + var3.script);
                  } catch (Exception var13) {
                     if (this.scriptType.hasFlag(ScriptType.Flags.RemoveLoadError)) {
                        String var16 = this.scriptType.getScriptTag();
                        DebugLog.log("[" + var16 + "] removing script due to load error = " + var4.getScriptObjectName());
                        var4 = null;
                     }

                     this.hasLoadErrors = true;
                     ExceptionLogger.logException(var13);
                     break;
                  }
               }

               if (var4 != null) {
                  if (var4.getObsolete()) {
                     var10000 = DebugLog.Script;
                     var10001 = this.scriptType.getScriptTag();
                     var10000.debugln("[" + var10001 + "] ignoring script, obsolete = " + var4.getScriptObjectName());
                     continue;
                  }

                  if (!var4.isEnabled()) {
                     var10000 = DebugLog.Script;
                     var10001 = this.scriptType.getScriptTag();
                     var10000.debugln("[" + var10001 + "] ignoring script, disabled = " + var4.getScriptObjectName());
                     continue;
                  }

                  if (var4.isDebugOnly() && !Core.bDebug) {
                     var10000 = DebugLog.Script;
                     var10001 = this.scriptType.getScriptTag();
                     var10000.debugln("[" + var10001 + "] ignoring script, is debug only = " + var4.getScriptObjectName());
                     continue;
                  }

                  var4.calculateScriptVersion();
                  if (var1 != ScriptLoadMode.Init && !var3.addedOnReload) {
                     if (var1 == ScriptLoadMode.Reload) {
                        this.onScriptLoad(var1, var4);
                     }
                  } else {
                     this.onScriptLoad(var1, var4);
                     this.scriptMap.put(var4.getScriptObjectName(), var4);
                     this.scriptList.add(var4);
                     if (var4.getScriptObjectName().contains(".")) {
                        this.dotInName.add(var4.getScriptObjectName());
                     }
                  }
               }

               var3.addedOnReload = false;
            }
         } catch (Exception var14) {
            ExceptionLogger.logException(var14);
            this.hasLoadErrors = true;
         } finally {
            currentScriptObject = null;
         }
      }

   }

   protected void onScriptLoad(ScriptLoadMode var1, E var2) {
   }

   protected abstract E getFromManager(String var1);

   protected abstract E getFromModule(String var1, ScriptModule var2);

   public E get(String var1) {
      if (var1.contains(".") && !this.dotInName.contains(var1)) {
         return this.getFromManager(var1);
      } else {
         BaseScriptObject var2 = (BaseScriptObject)this.scriptMap.get(var1);
         if (var2 != null) {
            return var2;
         } else {
            if (this.scriptType.hasFlag(ScriptType.Flags.SeekImports)) {
               for(int var3 = 0; var3 < this.module.Imports.size(); ++var3) {
                  String var4 = (String)this.module.Imports.get(var3);
                  ScriptModule var5 = ScriptManager.instance.getModule(var4);
                  var2 = this.getFromModule(var1, var5);
                  if (var2 != null) {
                     return var2;
                  }
               }
            }

            return null;
         }
      }
   }

   public abstract static class Template<E extends BaseScriptObject> extends ScriptBucket<E> {
      public Template(ScriptModule var1, ScriptType var2) {
         super(var1, var2);
         if (!var2.isTemplate()) {
            throw new RuntimeException("ScriptType '" + var2 + "' should be template!");
         }
      }

      public boolean CreateFromTokenPP(ScriptLoadMode var1, String var2, String var3) {
         try {
            if ("template".equals(var2)) {
               String[] var4 = var3.split("[{}]");
               String var5 = var4[0];
               var5 = var5.replace("template", "");
               String[] var6 = var5.trim().split("\\s+");
               if (var6.length == 2) {
                  String var7 = var6[0].trim();
                  String var8 = var6[1].trim();
                  if (this.scriptType.getScriptTag().equals(var7)) {
                     BaseScriptObject var9 = this.createInstance(this.module, var8, var3);
                     var9.InitLoadPP(var8);
                     this.scriptMap.put(var9.getScriptObjectName(), var9);
                     if (var9.getScriptObjectName().contains(".")) {
                        this.dotInName.add(var9.getScriptObjectName());
                     }

                     ScriptManager.println(this.scriptType, "Loaded template: " + var9.getScriptObjectName());
                     return true;
                  }
               }
            }
         } catch (Exception var10) {
            ExceptionLogger.logException(var10);
            this.hasLoadErrors = true;
         }

         return false;
      }

      public void LoadScripts(ScriptLoadMode var1) {
         this.scriptList.clear();
         this.scriptList.addAll(this.scriptMap.values());
      }
   }

   private static class LoadData<E extends BaseScriptObject> {
      private final String name;
      private final ArrayList<String> scriptBodies = new ArrayList();
      private final E script;
      private boolean reloaded = false;
      private boolean addedOnReload = false;

      private LoadData(E var1) {
         this.name = var1.getScriptObjectName();
         this.script = var1;
      }
   }
}
