package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;
import zombie.world.scripts.VersionHash;

public class ScriptsDictionary {
   private static final ArrayList<ScriptRegister<?>> registers = new ArrayList();
   public static final ScriptRegister<SpriteConfigScript> spriteConfigs = addScriptRegister(new ScriptRegister<SpriteConfigScript>("SpriteConfigs") {
      protected boolean canRegister(Object var1) {
         return var1 instanceof SpriteConfigScript;
      }
   });

   private ScriptsDictionary() {
   }

   private static <T extends ScriptRegister<?>> T addScriptRegister(T var0) {
      if (!registers.contains(var0)) {
         registers.add(var0);
      }

      return var0;
   }

   protected static void StartScriptLoading() {
      Iterator var0 = registers.iterator();

      while(var0.hasNext()) {
         ScriptRegister var1 = (ScriptRegister)var0.next();
         var1.onStartScriptLoading();
      }

   }

   protected static void reset() {
      Iterator var0 = registers.iterator();

      while(var0.hasNext()) {
         ScriptRegister var1 = (ScriptRegister)var0.next();
         var1.reset();
      }

   }

   public static void registerScript(BaseScriptObject var0) {
      try {
         if (!WorldDictionary.isAllowScriptItemLoading()) {
            throw new WorldDictionaryException("Cannot register script at this time.");
         }

         boolean var1 = false;

         for(int var3 = 0; var3 < registers.size(); ++var3) {
            ScriptRegister var2 = (ScriptRegister)registers.get(var3);
            if (var2.canRegister(var0)) {
               var1 = true;
               var2.register(var0);
               break;
            }
         }

         if (!var1) {
            throw new WorldDictionaryException("Missing script register.");
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   protected static void parseRegisters() throws WorldDictionaryException {
      for(int var1 = 0; var1 < registers.size(); ++var1) {
         ScriptRegister var0 = (ScriptRegister)registers.get(var1);
         if (!GameClient.bClient) {
            var0.parseLoadList();
         } else {
            var0.parseLoadListClient();
         }
      }

   }

   protected static void saveAsText(FileWriter var0, String var1) throws IOException {
      for(int var3 = 0; var3 < registers.size(); ++var3) {
         ScriptRegister var2 = (ScriptRegister)registers.get(var3);
         var2.saveAsText(var0, var1);
      }

   }

   protected static void saveToByteBuffer(ByteBuffer var0) throws IOException {
      var0.putInt(registers.size());
      Iterator var1 = registers.iterator();

      while(var1.hasNext()) {
         ScriptRegister var2 = (ScriptRegister)var1.next();
         GameWindow.WriteString(var0, var2.name);
         var2.save(var0);
      }

   }

   protected static void loadFromByteBuffer(ByteBuffer var0, int var1) throws IOException {
      int var2 = var0.getInt();

      for(int var3 = 0; var3 < var2; ++var3) {
         String var4 = GameWindow.ReadString(var0);
         boolean var5 = false;
         Iterator var6 = registers.iterator();

         while(var6.hasNext()) {
            ScriptRegister var7 = (ScriptRegister)var6.next();
            if (var7.name.equals(var4)) {
               var7.load(var0, var1);
               var5 = true;
               break;
            }
         }

         if (!var5) {
            ScriptsDictionary.ScriptRegister.loadEmpty(var0, var1);
            DebugLog.General.debugln("ScriptRegister not found or deprecated = " + var4);
         }
      }

   }

   public static class ScriptRegister<T extends BaseScriptObject> {
      private final Map<String, DictionaryScriptInfo<T>> namedMap = new HashMap();
      private final Map<Short, DictionaryScriptInfo<T>> idMap = new HashMap();
      private final Map<String, DictionaryScriptInfo<T>> loadList = new HashMap();
      private final VersionHash hash = new VersionHash();
      private final String name;
      private short NextID = 0;
      private final Tuple<T> emptyTuple = new Tuple(false);
      private final Tuple<T> notFoundTuple = new Tuple(true);

      protected ScriptRegister(String var1) {
         if (var1 == null) {
            throw new RuntimeException("Name cannot be null.");
         } else {
            this.name = var1;
         }
      }

      public void saveScript(ByteBuffer var1, T var2) {
         DictionaryScriptInfo var3 = this.getInfoForName(var2.getScriptObjectFullType());
         if (var3 != null) {
            var1.put((byte)1);
            var1.putShort(var3.registryID);
         } else {
            var1.put((byte)0);
            if (Core.bDebug) {
               String var10001 = this.name;
               DebugLog.General.warn("[" + var10001 + "] Unable to save script: '" + var2.getScriptObjectFullType() + "'");
            }
         }

      }

      public T loadScript(ByteBuffer var1, int var2) {
         if (var1.get() == 1) {
            short var3 = var1.getShort();
            DictionaryScriptInfo var4 = this.getInfoForID(var3);
            if (var4 != null) {
               return var4.script;
            }

            if (Core.bDebug) {
               DebugLog.General.warn("[" + this.name + "] Unable to load script with id: " + var3);
            }
         }

         return null;
      }

      public T get(String var1) {
         DictionaryScriptInfo var2 = (DictionaryScriptInfo)this.namedMap.get(var1);
         return var2 != null ? var2.script : null;
      }

      public T get(short var1) {
         DictionaryScriptInfo var2 = (DictionaryScriptInfo)this.idMap.get(var1);
         return var2 != null ? var2.script : null;
      }

      public short getIdFor(String var1) {
         DictionaryScriptInfo var2 = (DictionaryScriptInfo)this.namedMap.get(var1);
         return var2 != null ? var2.registryID : -1;
      }

      public boolean isRegistered(T var1) {
         if (WorldDictionary.isAllowScriptItemLoading()) {
            if (var1 != null && this.loadList.containsKey(var1.getScriptObjectFullType())) {
               if (((DictionaryScriptInfo)this.loadList.get(var1.getScriptObjectFullType())).script == var1) {
                  return true;
               }

               if (Core.bDebug) {
                  String var10001 = this.name;
                  DebugLog.General.warn("[" + var10001 + "] A script with same full type is registered, but objects are not equal, script: '" + var1.getScriptObjectFullType() + "'");
               }
            }

            return false;
         } else {
            if (var1 != null) {
               DictionaryScriptInfo var2 = (DictionaryScriptInfo)this.namedMap.get(var1.getScriptObjectFullType());
               if (var2 != null && var2.script == var1 && var2.isLoaded()) {
                  return true;
               }
            }

            return false;
         }
      }

      public DictionaryScriptInfo<T> getInfoForName(String var1) {
         return (DictionaryScriptInfo)this.namedMap.get(var1);
      }

      public DictionaryScriptInfo<T> getInfoForID(short var1) {
         return (DictionaryScriptInfo)this.idMap.get(var1);
      }

      protected void register(T var1) throws WorldDictionaryException {
         if (!this.canRegister(var1)) {
            throw new WorldDictionaryException("[" + this.name + "]  Cannot register this script!");
         } else {
            DictionaryScriptInfo var2 = new DictionaryScriptInfo();
            var2.script = var1;
            var2.name = var1.getScriptObjectFullType();
            this.loadList.put(var2.name, var2);
         }
      }

      protected boolean canRegister(Object var1) throws WorldDictionaryException {
         throw new WorldDictionaryException("[" + this.name + "]  CanRegister has not been overridden.");
      }

      public long getVersionHash(T var1) throws WorldDictionaryException {
         this.hash.reset();
         var1.getVersion(this.hash);
         String var10002;
         if (this.hash.isEmpty()) {
            var10002 = this.name;
            throw new WorldDictionaryException("[" + var10002 + "] Script hash is empty: " + var1.getScriptObjectFullType());
         } else if (this.hash.isCorrupted()) {
            var10002 = this.name;
            throw new WorldDictionaryException("[" + var10002 + "] Corrupted hash for script: " + var1.getScriptObjectFullType());
         } else {
            return this.hash.getHash();
         }
      }

      protected void parseLoadList() throws WorldDictionaryException {
         if (GameClient.bClient) {
            throw new WorldDictionaryException("[" + this.name + "] Shouldn't be called on client!");
         } else {
            DebugLog.General.debugln("- Parse load list: " + this.name);

            Iterator var1;
            Map.Entry var2;
            for(var1 = this.loadList.entrySet().iterator(); var1.hasNext(); ((DictionaryScriptInfo)var2.getValue()).isLoaded = false) {
               var2 = (Map.Entry)var1.next();
            }

            var1 = this.loadList.entrySet().iterator();

            while(var1.hasNext()) {
               var2 = (Map.Entry)var1.next();
               DictionaryScriptInfo var3 = (DictionaryScriptInfo)var2.getValue();
               DictionaryScriptInfo var4 = (DictionaryScriptInfo)this.namedMap.get(var3.name);
               var3.version = this.getVersionHash(var3.script);
               if (var4 == null) {
                  if (this.NextID >= 32767) {
                     throw new WorldDictionaryException("[" + this.name + "] Max script ID value reached for " + this.name + "!");
                  }

                  short var10003 = this.NextID;
                  this.NextID = (short)(var10003 + 1);
                  var3.registryID = var10003;
                  this.namedMap.put(var3.name, var3);
                  this.idMap.put(var3.registryID, var3);
                  var3.isLoaded = true;
                  WorldDictionaryLogger.log((Log.BaseLog)(new Log.RegisterScript(var3.copy())));
               } else {
                  var4.script = var3.script;
                  var4.isLoaded = true;
                  if (var4.version != var3.version) {
                     DebugLog.log("[" + this.name + "]  Script '" + var4.name + "' changed version.");
                     WorldDictionaryLogger.log((Log.BaseLog)(new Log.VersionChangedScript(var3.copy(), var4.version)));
                     var4.version = var3.version;
                  }
               }
            }

         }
      }

      protected void parseLoadListClient() throws WorldDictionaryException {
         if (!GameClient.bClient) {
            throw new WorldDictionaryException("Should only be called on client!");
         } else {
            Iterator var1 = this.namedMap.entrySet().iterator();

            while(var1.hasNext()) {
               Map.Entry var2 = (Map.Entry)var1.next();
               DictionaryScriptInfo var3 = (DictionaryScriptInfo)var2.getValue();
               if (var3.isLoaded) {
                  DictionaryScriptInfo var4 = (DictionaryScriptInfo)this.loadList.get(var3.name);
                  if (var4 == null) {
                     throw new WorldDictionaryException("[" + this.name + "] Missing dictionary script on client: " + var3.name);
                  }

                  long var5 = this.getVersionHash(var4.script);
                  if (var5 != var3.version) {
                     throw new WorldDictionaryException("[" + this.name + "] Script version mismatch with server: " + var3.name);
                  }

                  var3.script = var4.script;
               }
            }

         }
      }

      protected void reset() {
         this.NextID = 0;
         this.namedMap.clear();
         this.idMap.clear();
      }

      protected void onStartScriptLoading() {
         this.loadList.clear();
      }

      protected void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + this.name + " = {" + System.lineSeparator());
         String var3 = var2 + "\t";
         String var4 = var3 + "\t";
         Iterator var5 = this.namedMap.entrySet().iterator();

         while(var5.hasNext()) {
            Map.Entry var6 = (Map.Entry)var5.next();
            var1.write(var3 + "{" + System.lineSeparator());
            ((DictionaryScriptInfo)var6.getValue()).saveAsText(var1, var4);
            var1.write(var3 + "}," + System.lineSeparator());
         }

         var1.write(var2 + "}," + System.lineSeparator());
      }

      protected void save(ByteBuffer var1) throws IOException {
         ByteBlock var2 = ByteBlock.Start(var1, ByteBlock.Mode.Save);
         var1.putShort(this.NextID);
         var1.putInt(this.namedMap.size());
         Iterator var3 = this.namedMap.entrySet().iterator();

         while(var3.hasNext()) {
            Map.Entry var4 = (Map.Entry)var3.next();
            ((DictionaryScriptInfo)var4.getValue()).save(var1);
         }

         ByteBlock.End(var1, var2);
      }

      protected void load(ByteBuffer var1, int var2) throws IOException {
         ByteBlock var3 = ByteBlock.Start(var1, ByteBlock.Mode.Load);
         this.NextID = var1.getShort();
         int var4 = var1.getInt();

         for(int var5 = 0; var5 < var4; ++var5) {
            DictionaryScriptInfo var6 = new DictionaryScriptInfo();
            var6.load(var1, var2);
            this.namedMap.put(var6.name, var6);
            this.idMap.put(var6.registryID, var6);
         }

         ByteBlock.End(var1, var3);
      }

      private static final void loadEmpty(ByteBuffer var0, int var1) throws IOException {
         ByteBlock var2 = ByteBlock.Start(var0, ByteBlock.Mode.Load);
         ByteBlock.SkipAndEnd(var0, var2);
      }
   }

   public static class Tuple<T extends BaseScriptObject> {
      private T script;
      private long version = 0L;
      private long loadedVersion = 0L;
      private boolean notFound = false;

      public Tuple(boolean var1) {
         this.script = null;
         this.version = 0L;
         this.loadedVersion = 0L;
         this.notFound = var1;
      }

      public Tuple(T var1, long var2, long var4) {
         this.script = var1;
         this.version = var2;
         this.loadedVersion = var4;
      }

      public T getScript() {
         return this.script;
      }

      public long getVersion() {
         return this.version;
      }

      public long getLoadedVersion() {
         return this.loadedVersion;
      }

      public boolean isVersionValid() {
         return this.version == this.loadedVersion;
      }

      public boolean isScriptValid() {
         return this.script != null && this.isVersionValid();
      }

      public boolean isNotFound() {
         return this.notFound;
      }
   }
}
