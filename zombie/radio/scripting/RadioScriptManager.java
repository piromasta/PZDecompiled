package zombie.radio.scripting;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.radio.ZomboidRadio;

public final class RadioScriptManager {
   private final Map<Integer, RadioChannel> channels = new LinkedHashMap();
   private static RadioScriptManager instance;
   private int currentTimeStamp = 0;
   private ArrayList<RadioChannel> channelsList = new ArrayList();

   public static boolean hasInstance() {
      return instance != null;
   }

   public static RadioScriptManager getInstance() {
      if (instance == null) {
         instance = new RadioScriptManager();
      }

      return instance;
   }

   private RadioScriptManager() {
   }

   public void init(int var1) {
   }

   public Map<Integer, RadioChannel> getChannels() {
      return this.channels;
   }

   public ArrayList getChannelsList() {
      this.channelsList.clear();
      Iterator var1 = this.channels.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         this.channelsList.add((RadioChannel)var2.getValue());
      }

      return this.channelsList;
   }

   public RadioChannel getRadioChannel(String var1) {
      Iterator var2 = this.channels.entrySet().iterator();

      Map.Entry var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (Map.Entry)var2.next();
      } while(!((RadioChannel)var3.getValue()).getGUID().equals(var1));

      return (RadioChannel)var3.getValue();
   }

   public void simulateScriptsUntil(int var1, boolean var2) {
      Iterator var3 = this.channels.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         this.simulateChannelUntil(((RadioChannel)var4.getValue()).GetFrequency(), var1, var2);
      }

   }

   public void simulateChannelUntil(int var1, int var2, boolean var3) {
      if (this.channels.containsKey(var1)) {
         RadioChannel var4 = (RadioChannel)this.channels.get(var1);
         if (var4.isTimeSynced() && !var3) {
            return;
         }

         for(int var5 = 0; var5 < var2; ++var5) {
            int var6 = var5 * 24 * 60;
            var4.UpdateScripts(this.currentTimeStamp, var6);
         }

         var4.setTimeSynced(true);
      }

   }

   public int getCurrentTimeStamp() {
      return this.currentTimeStamp;
   }

   public void PlayerListensChannel(int var1, boolean var2, boolean var3) {
      if (this.channels.containsKey(var1) && ((RadioChannel)this.channels.get(var1)).IsTv() == var3) {
         ((RadioChannel)this.channels.get(var1)).SetPlayerIsListening(var2);
      }

   }

   public void AddChannel(RadioChannel var1, boolean var2) {
      String var3;
      if (var1 == null || !var2 && this.channels.containsKey(var1.GetFrequency())) {
         var3 = var1 != null ? var1.GetName() : "null";
         DebugLog.log(DebugType.Radio, "Error adding radiochannel (" + var3 + "), channel is null or frequency key already exists");
      } else {
         this.channels.put(var1.GetFrequency(), var1);
         var3 = var1.GetCategory().name();
         ZomboidRadio.getInstance().addChannelName(var1.GetName(), var1.GetFrequency(), var3, var2);
      }

   }

   public void RemoveChannel(int var1) {
      if (this.channels.containsKey(var1)) {
         this.channels.remove(var1);
         ZomboidRadio.getInstance().removeChannelName(var1);
      }

   }

   public void UpdateScripts(int var1, int var2, int var3) {
      this.currentTimeStamp = var1 * 24 * 60 + var2 * 60 + var3;
      Iterator var4 = this.channels.entrySet().iterator();

      while(var4.hasNext()) {
         Map.Entry var5 = (Map.Entry)var4.next();
         ((RadioChannel)var5.getValue()).UpdateScripts(this.currentTimeStamp, var1);
      }

   }

   public void update() {
      Iterator var1 = this.channels.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         ((RadioChannel)var2.getValue()).update();
      }

   }

   public void reset() {
      instance = null;
   }

   public void Save(Writer var1) throws IOException {
      Iterator var2 = this.channels.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         RadioScript var4 = ((RadioChannel)var3.getValue()).getCurrentScript();
         RadioBroadCast var5 = ((RadioChannel)var3.getValue()).getAiringBroadcast();
         var1.write(String.join(",", Integer.toString((Integer)var3.getKey()), Integer.toString(((RadioChannel)var3.getValue()).getCurrentScriptLoop()), Integer.toString(((RadioChannel)var3.getValue()).getCurrentScriptMaxLoops()), var4 == null ? "none" : var4.GetName(), var4 == null ? "-1" : Integer.toString(var4.getStartDay()), var5 == null ? "none" : var5.getID(), var5 == null ? "-1" : Integer.toString(var5.getCurrentLineNumber())));
         var1.write(System.lineSeparator());
      }

   }

   public void Load(List<String> var1) throws IOException, NumberFormatException {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         if (var3 != null && !var3.isBlank()) {
            var3 = var3.trim();
            String[] var4 = var3.split(",");
            RadioChannel var5 = (RadioChannel)this.channels.get(Integer.parseInt(var4[0]));
            if (var5 != null) {
               var5.setTimeSynced(true);
               if (var4.length == 7) {
                  if (!"none".equals(var4[3])) {
                     var5.setActiveScript(var4[3], Integer.parseInt(var4[4]), Integer.parseInt(var4[1]), Integer.parseInt(var4[2]));
                  }

                  if (!"none".equals(var4[5])) {
                     var5.LoadAiringBroadcast(var4[5], Integer.parseInt(var4[6]));
                  }
               }
            }
         }
      }

   }
}
