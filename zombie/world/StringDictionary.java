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
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;

public class StringDictionary {
   private static final ArrayList<StringRegister> registers = new ArrayList();
   private static final int MaxStringLen = 64;
   public static final StringRegister Generic = addScriptRegister(new StringRegister(64, "Generic") {
   });

   private StringDictionary() {
   }

   private static StringRegister addScriptRegister(StringRegister var0) {
      if (!registers.contains(var0)) {
         registers.add(var0);
      }

      return var0;
   }

   protected static void StartScriptLoading() {
      Iterator var0 = registers.iterator();

      while(var0.hasNext()) {
         StringRegister var1 = (StringRegister)var0.next();
         var1.onStartScriptLoading();
      }

   }

   protected static void reset() {
      Iterator var0 = registers.iterator();

      while(var0.hasNext()) {
         StringRegister var1 = (StringRegister)var0.next();
         var1.reset();
      }

   }

   protected static void parseRegisters() throws WorldDictionaryException {
      for(int var1 = 0; var1 < registers.size(); ++var1) {
         StringRegister var0 = (StringRegister)registers.get(var1);
         if (!GameClient.bClient) {
            var0.parseLoadList();
         } else {
            var0.parseLoadListClient();
         }
      }

   }

   protected static void saveAsText(FileWriter var0, String var1) throws IOException {
      for(int var3 = 0; var3 < registers.size(); ++var3) {
         StringRegister var2 = (StringRegister)registers.get(var3);
         var2.saveAsText(var0, var1);
      }

   }

   protected static void saveToByteBuffer(ByteBuffer var0) throws IOException {
      var0.putInt(registers.size());
      Iterator var1 = registers.iterator();

      while(var1.hasNext()) {
         StringRegister var2 = (StringRegister)var1.next();
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
            StringRegister var7 = (StringRegister)var6.next();
            if (var7.name.equals(var4)) {
               var7.load(var0, var1);
               var5 = true;
               break;
            }
         }

         if (!var5) {
            StringDictionary.StringRegister.loadEmpty(var0, var1);
            DebugLog.General.debugln("ScriptRegister not found or deprecated = " + var4);
         }
      }

   }

   public static class StringRegister {
      private final Map<String, DictionaryStringInfo> stringToIdMap = new HashMap();
      private final Map<Short, DictionaryStringInfo> idToStringMap = new HashMap();
      private final Map<String, DictionaryStringInfo> loadList = new HashMap();
      private final String name;
      private short NextID = 0;
      protected final int maxLen;

      protected StringRegister(int var1, String var2) {
         if (var2 == null) {
            throw new RuntimeException("Name cannot be null.");
         } else {
            this.name = var2;
            this.maxLen = var1;
         }
      }

      public void saveString(ByteBuffer var1, String var2) {
         DictionaryStringInfo var3 = this.getInfoForName(var2);
         if (var3 != null) {
            var1.put((byte)1);
            var1.putShort(var3.registryID);
         } else {
            DebugLog.General.warn("Unable to save string from register: " + var2);
            var1.put((byte)0);
            GameWindow.WriteString(var1, var2);
         }

      }

      public String loadString(ByteBuffer var1, int var2) {
         if (var1.get() == 1) {
            short var3 = var1.getShort();
            DictionaryStringInfo var4 = this.getInfoForID(var3);
            if (var4 != null) {
               return var4.string;
            } else {
               if (Core.bDebug) {
                  DebugLog.General.warn("Unable to load string with id: " + var3);
               }

               return null;
            }
         } else {
            return GameWindow.ReadString(var1);
         }
      }

      public String get(short var1) {
         DictionaryStringInfo var2 = (DictionaryStringInfo)this.idToStringMap.get(var1);
         return var2 != null ? var2.string : null;
      }

      public short getIdFor(String var1) {
         DictionaryStringInfo var2 = (DictionaryStringInfo)this.stringToIdMap.get(var1);
         return var2 != null ? var2.registryID : -1;
      }

      public boolean isRegistered(String var1) {
         if (WorldDictionary.isAllowScriptItemLoading()) {
            return var1 != null ? this.loadList.containsKey(var1) : false;
         } else {
            if (var1 != null) {
               DictionaryStringInfo var2 = (DictionaryStringInfo)this.stringToIdMap.get(var1);
               if (var2 != null && var2.string.equals(var1) && var2.isLoaded()) {
                  return true;
               }
            }

            return false;
         }
      }

      public DictionaryStringInfo getInfoForName(String var1) {
         return (DictionaryStringInfo)this.stringToIdMap.get(var1);
      }

      public DictionaryStringInfo getInfoForID(short var1) {
         return (DictionaryStringInfo)this.idToStringMap.get(var1);
      }

      public void register(String var1) {
         if (var1 != null) {
            try {
               if (!WorldDictionary.isAllowScriptItemLoading()) {
                  throw new WorldDictionaryException("Cannot register string at this time.");
               }

               if (!this.canRegister(var1)) {
                  throw new WorldDictionaryException(this.name + ": Cannot register this string! string = " + var1);
               }

               if (this.loadList.containsKey(var1)) {
                  return;
               }

               DictionaryStringInfo var2 = new DictionaryStringInfo();
               var2.string = var1;
               this.loadList.put(var2.string, var2);
            } catch (Exception var3) {
               var3.printStackTrace();
            }

         }
      }

      protected boolean canRegister(String var1) throws WorldDictionaryException {
         if (var1 != null) {
            if (var1.length() < this.maxLen) {
               return true;
            }

            if (Core.bDebug) {
               int var10002 = this.maxLen;
               throw new RuntimeException("String exceeds default max string length, can be adjusted. maxlen = " + var10002 + ", strlen = " + var1.length() + ", string = " + var1);
            }
         }

         return false;
      }

      protected void parseLoadList() throws WorldDictionaryException {
         if (GameClient.bClient) {
            throw new WorldDictionaryException("Shouldn't be called on client!");
         } else {
            Iterator var1;
            Map.Entry var2;
            for(var1 = this.loadList.entrySet().iterator(); var1.hasNext(); ((DictionaryStringInfo)var2.getValue()).isLoaded = false) {
               var2 = (Map.Entry)var1.next();
            }

            var1 = this.loadList.entrySet().iterator();

            while(var1.hasNext()) {
               var2 = (Map.Entry)var1.next();
               DictionaryStringInfo var3 = (DictionaryStringInfo)var2.getValue();
               DictionaryStringInfo var4 = (DictionaryStringInfo)this.stringToIdMap.get(var3.string);
               if (var4 == null) {
                  if (this.NextID >= 32767) {
                     throw new WorldDictionaryException("Max string ID value reached for " + this.name + "!");
                  }

                  short var10003 = this.NextID;
                  this.NextID = (short)(var10003 + 1);
                  var3.registryID = var10003;
                  this.stringToIdMap.put(var3.string, var3);
                  this.idToStringMap.put(var3.registryID, var3);
                  var3.isLoaded = true;
                  WorldDictionaryLogger.log((Log.BaseLog)(new Log.RegisterString(this.name, var3.string, var3.registryID, true)));
               } else {
                  var4.string = var3.string;
                  var4.isLoaded = true;
               }
            }

         }
      }

      protected void parseLoadListClient() throws WorldDictionaryException {
         if (!GameClient.bClient) {
            throw new WorldDictionaryException("Should only be called on client!");
         } else {
            Iterator var1 = this.stringToIdMap.entrySet().iterator();

            while(var1.hasNext()) {
               Map.Entry var2 = (Map.Entry)var1.next();
               DictionaryStringInfo var3 = (DictionaryStringInfo)var2.getValue();
               if (var3.isLoaded) {
                  DictionaryStringInfo var4 = (DictionaryStringInfo)this.loadList.get(var3.string);
                  if (var4 == null) {
                     throw new WorldDictionaryException("Missing dictionary string on client: " + var3.string);
                  }

                  var3.string = var4.string;
               }
            }

         }
      }

      protected void reset() {
         this.NextID = 0;
         this.stringToIdMap.clear();
         this.idToStringMap.clear();
      }

      protected void onStartScriptLoading() {
         this.loadList.clear();
      }

      protected void saveAsText(FileWriter var1, String var2) throws IOException {
         var1.write(var2 + this.name + " = {" + System.lineSeparator());
         String var3 = var2 + "\t";
         String var4 = var3 + "\t";
         Iterator var5 = this.stringToIdMap.entrySet().iterator();

         while(var5.hasNext()) {
            Map.Entry var6 = (Map.Entry)var5.next();
            var1.write(var3 + "{" + System.lineSeparator());
            ((DictionaryStringInfo)var6.getValue()).saveAsText(var1, var4);
            var1.write(var3 + "}," + System.lineSeparator());
         }

         var1.write(var2 + "}," + System.lineSeparator());
      }

      protected void save(ByteBuffer var1) throws IOException {
         ByteBlock var2 = ByteBlock.Start(var1, ByteBlock.Mode.Save);
         var1.putShort(this.NextID);
         var1.putInt(this.stringToIdMap.size());
         Iterator var3 = this.stringToIdMap.entrySet().iterator();

         while(var3.hasNext()) {
            Map.Entry var4 = (Map.Entry)var3.next();
            ((DictionaryStringInfo)var4.getValue()).save(var1);
         }

         ByteBlock.End(var1, var2);
      }

      protected void load(ByteBuffer var1, int var2) throws IOException {
         ByteBlock var3 = ByteBlock.Start(var1, ByteBlock.Mode.Load);
         this.NextID = var1.getShort();
         int var4 = var1.getInt();

         for(int var5 = 0; var5 < var4; ++var5) {
            DictionaryStringInfo var6 = new DictionaryStringInfo();
            var6.load(var1, var2);
            this.stringToIdMap.put(var6.string, var6);
            this.idToStringMap.put(var6.registryID, var6);
         }

         ByteBlock.End(var1, var3);
      }

      private static final void loadEmpty(ByteBuffer var0, int var1) throws IOException {
         ByteBlock var2 = ByteBlock.Start(var0, ByteBlock.Mode.Load);
         ByteBlock.SkipAndEnd(var0, var2);
      }
   }
}
