package zombie.inventory.recipemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.objects.Recipe;
import zombie.util.StringUtils;

public class RecipeMonitor {
   private static boolean enabled = false;
   private static boolean suspended = false;
   private static int monitorID = -1;
   private static int tabs = 0;
   private static String tabStr = "";
   private static final String tabSize = "  ";
   private static final Color defColor;
   public static final Color colGray;
   public static final Color colNeg;
   public static final Color colPos;
   public static final Color colHeader;
   private static final ArrayList<String> lines;
   private static final ArrayList<Color> colors;
   private static String recipeName;
   private static Recipe lastRecipe;
   private static final ArrayList<String> recipeLines;

   public RecipeMonitor() {
   }

   public static void Enable(boolean var0) {
      enabled = var0;
   }

   public static boolean IsEnabled() {
      return enabled;
   }

   public static int getMonitorID() {
      return monitorID;
   }

   public static void StartMonitor() {
      if (enabled) {
         ++monitorID;
         suspended = false;
         lines.clear();
         colors.clear();
         recipeLines.clear();
         recipeName = "none";
         lastRecipe = null;
         ResetTabs();
         Log("MonitorID = " + monitorID);
      }

   }

   public static Color getColGray() {
      return colGray;
   }

   public static Color getColBlack() {
      return Color.black;
   }

   public static void setRecipe(Recipe var0) {
      recipeName = var0.getOriginalname();
      lastRecipe = var0;
   }

   public static String getRecipeName() {
      return recipeName;
   }

   public static Recipe getRecipe() {
      return lastRecipe;
   }

   /** @deprecated */
   @Deprecated
   public static ArrayList<String> getRecipeLines() {
      return recipeLines;
   }

   public static boolean canLog() {
      return Core.bDebug && enabled && !suspended;
   }

   public static void suspend() {
      suspended = true;
   }

   public static void resume() {
      suspended = false;
   }

   public static void Log(String var0) {
      if (canLog()) {
         Log(var0, defColor);
      }
   }

   public static void Log(String var0, Color var1) {
      if (canLog()) {
         lines.add(tabStr + var0);
         colors.add(var1);
      }

   }

   public static void LogBlanc() {
      if (canLog()) {
         Log("");
      }
   }

   public static <T> void LogList(String var0, ArrayList<T> var1) {
      if (canLog()) {
         Log(var0 + " {");
         IncTab();
         if (var1 != null) {
            Iterator var2 = var1.iterator();

            while(var2.hasNext()) {
               Object var3 = var2.next();
               Log(var3.toString());
            }
         }

         DecTab();
         Log("}");
      }
   }

   public static void LogInit(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3, ArrayList<InventoryItem> var4, boolean var5) {
      if (canLog()) {
         Log("[Recipe]", colHeader);
         Log("Starting recipe: " + var0.getOriginalname());
         Log("All items = " + var5);
         Log("character = " + var1.getFullName());
         Log("selected item = " + var3);
         LogContainers("containers", var2);
         LogBlanc();
      }
   }

   public static String getContainerString(ItemContainer var0) {
      if (var0 == null) {
         return "ItemContainer:[null]";
      } else {
         if (var0.getParent() != null) {
            if (var0.getParent() instanceof IsoGameCharacter) {
               return "ItemContainer:[type:" + var0.type + ", parent:PlayerInventory]";
            }

            if (var0.getParent().getSprite() != null) {
               String var10000 = var0.type;
               return "ItemContainer:[type:" + var10000 + ", parent:PlayerInventory, sprite:" + var0.getParent().getSprite().name + "]";
            }
         }

         return var0.toString();
      }
   }

   private static void LogContainers(String var0, ArrayList<ItemContainer> var1) {
      LogContainers(var0, var1, false);
   }

   private static void LogContainers(String var0, ArrayList<ItemContainer> var1, boolean var2) {
      if (canLog()) {
         Log(var0 + " {");
         IncTab();
         if (var1 != null) {
            Iterator var3 = var1.iterator();

            label34:
            while(true) {
               while(true) {
                  if (!var3.hasNext()) {
                     break label34;
                  }

                  ItemContainer var4 = (ItemContainer)var3.next();
                  if (var2) {
                     Log(getContainerString(var4));
                     IncTab();
                     Iterator var5 = var4.getItems().iterator();

                     while(var5.hasNext()) {
                        InventoryItem var6 = (InventoryItem)var5.next();
                        Log("item > " + var6);
                     }

                     DecTab();
                  } else {
                     Log(getContainerString(var4));
                  }
               }
            }
         } else {
            Log("null");
         }

         DecTab();
         Log("}");
      }
   }

   public static void LogSources(List<Recipe.Source> var0) {
      if (canLog()) {
         Log("[Sources]", colHeader);
         if (var0 == null) {
            Log("Sources null.", colNeg);
         } else {
            for(int var1 = 0; var1 < var0.size(); ++var1) {
               LogSource("[" + var1 + "] Source: ", (Recipe.Source)var0.get(var1));
            }

         }
      }
   }

   private static void LogSource(String var0, Recipe.Source var1) {
      if (canLog()) {
         Log(var0 + " {");
         IncTab();
         if (var1 != null) {
            String var10000 = var1.keep ? "(keep)" : "";
            Log(var10000 + (var1.destroy ? "(destroy)" : "") + "(count=" + var1.count + ")(use=" + var1.use + "):");
            IncTab();
            Log("items=" + var1.getItems().toString());
            Log("orig=" + var1.getOriginalItems().toString());
            DecTab();
         }

         DecTab();
         Log("}");
      }
   }

   public static void LogItem(String var0, InventoryItem var1) {
      if (canLog()) {
         Log(var0 + " = " + var1);
      }
   }

   public static String getResultString(Recipe.Result var0) {
      String var10000 = var0.getFullType();
      return "result = [" + var10000 + ", count=" + var0.getCount() + ", drain=" + var0.getDrainableCount() + "]";
   }

   private static void setTabStr() {
      if (tabs > 0) {
         tabStr = "  ".repeat(tabs);
      } else {
         tabStr = "";
      }

   }

   public static void ResetTabs() {
      tabs = 0;
      setTabStr();
   }

   public static void SetTab(int var0) {
      if (canLog()) {
         tabs = var0;
         setTabStr();
      }
   }

   public static void IncTab() {
      if (canLog()) {
         ++tabs;
         setTabStr();
      }
   }

   public static void DecTab() {
      if (canLog()) {
         --tabs;
         if (tabs < 0) {
            tabs = 0;
         }

         setTabStr();
      }
   }

   public static ArrayList<String> GetLines() {
      return lines;
   }

   public static ArrayList<Color> GetColors() {
      return colors;
   }

   public static Color GetColorForLine(int var0) {
      return var0 >= 0 && var0 < colors.size() ? (Color)colors.get(var0) : defColor;
   }

   public static String GetSaveDir() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      return var10000 + File.separator + "RecipeLogs" + File.separator;
   }

   public static void SaveToFile() {
      if (lines.size() > 0) {
         try {
            String var0 = (new SimpleDateFormat("yyyyMMdd_HHmmss")).format(new Date());
            String var1 = "log_" + var0;
            String var2 = recipeName;
            if (var2 != null) {
               var2 = var2.toLowerCase();
               var2 = var2.replaceAll("\\s", "_");
               var2 = var2.replace("\\.", "");
            }

            if (StringUtils.isNullOrWhitespace(var2)) {
               var2 = "unkown";
            }

            var1 = var1 + "_" + var2;
            String var3 = GetSaveDir();
            File var4 = new File(var3);
            if (!var4.exists() && !var4.mkdirs()) {
               DebugLog.log("Failed to create path = " + var3);
               return;
            }

            String var5 = var3 + var1 + ".txt";
            DebugLog.log("Attempting to save recipe log to: " + var5);
            File var6 = new File(var5);

            try {
               BufferedWriter var7 = new BufferedWriter(new FileWriter(var6, false));

               try {
                  w_write(var7, "Recipe name = " + recipeName);
                  w_write(var7, "# Recipe at time of recording:");
                  w_blanc(var7);
                  Iterator var8 = recipeLines.iterator();

                  label51:
                  while(true) {
                     String var9;
                     if (!var8.hasNext()) {
                        w_blanc(var7);
                        w_write(var7, "# Recipe monitor log:");
                        w_blanc(var7);
                        var8 = lines.iterator();

                        while(true) {
                           if (!var8.hasNext()) {
                              break label51;
                           }

                           var9 = (String)var8.next();
                           w_write(var7, var9);
                        }
                     }

                     var9 = (String)var8.next();
                     w_write(var7, var9);
                  }
               } catch (Throwable var11) {
                  try {
                     var7.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var7.close();
            } catch (Exception var12) {
               var12.printStackTrace();
            }
         } catch (Exception var13) {
            var13.printStackTrace();
         }
      }

   }

   private static void w_blanc(BufferedWriter var0) throws IOException {
      w_write(var0, (String)null);
   }

   private static void w_write(BufferedWriter var0, String var1) throws IOException {
      if (var1 != null) {
         var0.write(var1);
      }

      var0.newLine();
   }

   static {
      defColor = Color.black;
      colGray = new Color(0.5F, 0.5F, 0.5F);
      colNeg = Colors.Maroon;
      colPos = Colors.DarkGreen;
      colHeader = Colors.SaddleBrown;
      lines = new ArrayList();
      colors = new ArrayList();
      recipeName = "none";
      lastRecipe = null;
      recipeLines = new ArrayList();
   }
}
