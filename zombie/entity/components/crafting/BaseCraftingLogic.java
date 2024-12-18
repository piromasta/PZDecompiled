package zombie.entity.components.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.inventory.InventoryItem;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public abstract class BaseCraftingLogic {
   protected final ArrayList<CraftRecipe> completeRecipeList = new ArrayList();
   protected final ArrayList<CraftRecipe> filteredRecipeList = new ArrayList();
   protected final ArrayList<Resource> sourceResources = new ArrayList();
   protected final ArrayList<InventoryItem> allItems = new ArrayList();
   protected String categoryFilterString = null;
   protected String filterString = null;
   protected final CachedRecipeComparator cachedRecipeComparator;
   protected final HashMap<String, ArrayList<CraftEventHandler>> events = new HashMap();
   protected final IsoGameCharacter player;
   protected final CraftRecipeData testRecipeData;
   protected final ArrayList<CachedRecipeInfo> cachedRecipeInfos = new ArrayList();
   protected final HashMap<CraftRecipe, CachedRecipeInfo> cachedRecipeInfoMap = new HashMap();
   protected boolean cachedRecipeInfosDirty = false;

   public BaseCraftingLogic(IsoGameCharacter var1, CraftBench var2) {
      this.player = var1;
      this.cachedRecipeComparator = new CachedRecipeComparator(this);
      this.testRecipeData = new CraftRecipeData(CraftMode.Handcraft, var2 != null, true, false, true);
   }

   public ArrayList<String> getCategoryList() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.completeRecipeList.size(); ++var2) {
         if (!var1.contains(((CraftRecipe)this.completeRecipeList.get(var2)).getCategory())) {
            var1.add(((CraftRecipe)this.completeRecipeList.get(var2)).getCategory());
         }
      }

      var1.sort(String::compareToIgnoreCase);
      return var1;
   }

   public void filterRecipeList(String var1, String var2) {
      this.filterRecipeList(var1, var2, false);
   }

   public void filterRecipeList(String var1, String var2, boolean var3) {
      this.filterRecipeList(var1, var2, var3, LuaManager.GlobalObject.getSpecificPlayer(0));
   }

   public void filterRecipeList(String var1, String var2, boolean var3, IsoPlayer var4) {
      ArrayList var5 = new ArrayList();
      if (var4 == null) {
         var4 = LuaManager.GlobalObject.getSpecificPlayer(0);
      }

      if (var4 == null) {
         var5 = this.completeRecipeList;
      } else {
         for(int var6 = 0; var6 < this.completeRecipeList.size(); ++var6) {
            CraftRecipe var7 = (CraftRecipe)this.completeRecipeList.get(var6);
            if (!var7.needToBeLearn() || CraftRecipeManager.hasPlayerLearnedRecipe(var7, var4)) {
               var5.add(var7);
            }
         }
      }

      boolean var8 = this.categoryFilterString != null && !this.categoryFilterString.equals(var2) || var2 != null && !var2.equals(this.categoryFilterString);
      boolean var9 = this.filterString != null && !this.filterString.equals(var1) || var1 != null && !var1.equals(this.filterString);
      if (var8 || var9 || var3) {
         this.filterString = var1;
         this.categoryFilterString = var2;
         if (StringUtils.isNullOrWhitespace(this.filterString) && StringUtils.isNullOrWhitespace(this.categoryFilterString)) {
            this.filteredRecipeList.clear();
            this.filteredRecipeList.addAll(var5);
         } else {
            filterRecipeList(var1, this.categoryFilterString, this.filteredRecipeList, var5, var4);
         }

         this.sortRecipeList();
      }

   }

   protected void triggerEvent(String var1, Object... var2) {
      if (this.events.containsKey(var1)) {
         ArrayList var3 = (ArrayList)this.events.get(var1);

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            CraftEventHandler var5 = (CraftEventHandler)var3.get(var4);

            try {
               if (var5.targetTable != null) {
                  Object[] var6 = new Object[var2.length + 1];
                  System.arraycopy(var2, 0, var6, 1, var2.length);
                  var6[0] = var5.targetTable;
                  LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), var5.function, var6);
               } else {
                  LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), var5.function, var2);
               }
            } catch (Exception var7) {
               var7.printStackTrace();
            }
         }
      } else {
         DebugLog.General.warn("Event '" + var1 + "' is unknown.");
      }

   }

   public static List<CraftRecipe> filterRecipeList(String var0, String var1, List<CraftRecipe> var2, List<CraftRecipe> var3, IsoPlayer var4) {
      if (var2 != null) {
         var2.clear();
      }

      if (var3 != null && var2 != null) {
         if (StringUtils.isNullOrWhitespace(var0) && StringUtils.isNullOrWhitespace(var1)) {
            var2.addAll(var3);
            return var2;
         } else {
            CraftRecipe var5;
            Object var6;
            if (!StringUtils.isNullOrWhitespace(var1)) {
               var6 = new ArrayList();

               for(int var7 = 0; var7 < var3.size(); ++var7) {
                  var5 = (CraftRecipe)var3.get(var7);
                  if (var5 != null) {
                     if (var1.equals("*")) {
                        String var8 = getFavouriteModDataString(var5);
                        Object var9 = var4.getModData().rawget(var8);
                        if (var9 != null && (Boolean)var9) {
                           ((List)var6).add(var5);
                        }
                     } else if (var5.getCategory().equalsIgnoreCase(var1)) {
                        ((List)var6).add(var5);
                     }
                  }
               }
            } else {
               var6 = var3;
            }

            if (!StringUtils.isNullOrWhitespace(var0)) {
               CraftRecipeManager.FilterMode var13 = CraftRecipeManager.FilterMode.Name;
               if (var0.startsWith("@")) {
                  var13 = CraftRecipeManager.FilterMode.ModName;
                  var0 = var0.substring(1, var0.length());
               } else if (var0.startsWith("$")) {
                  var13 = CraftRecipeManager.FilterMode.Tags;
                  var0 = var0.substring(1, var0.length());
               } else if (var0.contains("-@-")) {
                  String[] var14 = var0.split("-@-");
                  var0 = var14[0];
                  if ("InputName".equalsIgnoreCase(var14[1])) {
                     var13 = CraftRecipeManager.FilterMode.InputName;
                  }

                  if ("OutputName".equalsIgnoreCase(var14[1])) {
                     var13 = CraftRecipeManager.FilterMode.OutputName;
                  }
               }

               var0 = var0.toLowerCase();
               int var11;
               Item var12;
               int var16;
               int var17;
               switch (var13) {
                  case Name:
                     for(var16 = 0; var16 < ((List)var6).size(); ++var16) {
                        var5 = (CraftRecipe)((List)var6).get(var16);
                        if (var5 != null && var5.getTranslationName() != null && var5.getTranslationName().toLowerCase().contains(var0)) {
                           var2.add(var5);
                        }
                     }

                     return var2;
                  case InputName:
                     for(var16 = 0; var16 < ((List)var6).size(); ++var16) {
                        var5 = (CraftRecipe)((List)var6).get(var16);
                        if (var5 != null) {
                           for(var17 = 0; var17 < var5.getInputs().size(); ++var17) {
                              InputScript var20 = (InputScript)var5.getInputs().get(var17);

                              for(var11 = 0; var11 < var20.getPossibleInputItems().size(); ++var11) {
                                 var12 = (Item)var20.getPossibleInputItems().get(var11);
                                 if (var12.getDisplayName().toLowerCase().contains(var0) && !var2.contains(var5)) {
                                    var2.add(var5);
                                    break;
                                 }
                              }
                           }
                        }
                     }

                     return var2;
                  case OutputName:
                     for(var16 = 0; var16 < ((List)var6).size(); ++var16) {
                        var5 = (CraftRecipe)((List)var6).get(var16);
                        if (var5 != null) {
                           for(var17 = 0; var17 < var5.getOutputs().size(); ++var17) {
                              OutputScript var19 = (OutputScript)var5.getOutputs().get(var17);
                              if (var19.getOutputMapper() != null && var19.getOutputMapper().getResultItems() != null) {
                                 for(var11 = 0; var11 < var19.getOutputMapper().getResultItems().size(); ++var11) {
                                    var12 = (Item)var19.getOutputMapper().getResultItems().get(var11);
                                    if (var12.getDisplayName().toLowerCase().contains(var0) && !var2.contains(var5)) {
                                       var2.add(var5);
                                       break;
                                    }
                                 }
                              }
                           }
                        }
                     }

                     return var2;
                  case ModName:
                     for(var16 = 0; var16 < ((List)var6).size(); ++var16) {
                        var5 = (CraftRecipe)((List)var6).get(var16);
                        if (var5 != null && var5.getModName() != null && var5.getModName().toLowerCase().contains(var0)) {
                           var2.add(var5);
                        }
                     }

                     return var2;
                  case Tags:
                     ArrayList var15 = new ArrayList();

                     for(var17 = 0; var17 < CraftRecipeManager.getAllRecipeTags().size(); ++var17) {
                        String var10 = (String)CraftRecipeManager.getAllRecipeTags().get(var17);
                        if (var10.contains(var0)) {
                           var15.add(var10.toLowerCase());
                        }
                     }

                     for(var17 = 0; var17 < ((List)var6).size(); ++var17) {
                        var5 = (CraftRecipe)((List)var6).get(var17);

                        for(int var18 = 0; var18 < var15.size(); ++var18) {
                           if (var5 != null && var5.hasTag((String)var15.get(var18))) {
                              var2.add(var5);
                              break;
                           }
                        }
                     }
               }
            } else {
               var2.addAll((Collection)var6);
            }

            return var2;
         }
      } else {
         DebugLog.General.error("one of list parameters is null.");
         return var2;
      }
   }

   public void sortRecipeList() {
      this.rebuildCachedRecipeInfo();
      this.filteredRecipeList.sort(this.cachedRecipeComparator);
      this.triggerEvent("onUpdateRecipeList", this.filteredRecipeList);
   }

   protected CachedRecipeInfo createCachedRecipeInfo(CraftRecipe var1) {
      Objects.requireNonNull(var1);
      CachedRecipeInfo var2 = BaseCraftingLogic.CachedRecipeInfo.Alloc(var1);
      var2.isValid = CraftRecipeManager.isValidRecipeForCharacter(var1, this.player, (CraftRecipeMonitor)null);
      this.testRecipeData.setRecipe(var1);
      var2.canPerform = this.testRecipeData.canPerform(this.player, this.sourceResources, this.allItems);
      var2.available = var2.isValid && var2.canPerform;
      this.cachedRecipeInfos.add(var2);
      this.cachedRecipeInfoMap.put(var1, var2);
      return var2;
   }

   protected void rebuildCachedRecipeInfo() {
      if (this.cachedRecipeInfosDirty) {
         int var2;
         for(var2 = 0; var2 < this.cachedRecipeInfos.size(); ++var2) {
            CachedRecipeInfo var1 = (CachedRecipeInfo)this.cachedRecipeInfos.get(var2);
            BaseCraftingLogic.CachedRecipeInfo.Release(var1);
         }

         this.cachedRecipeInfos.clear();
         this.cachedRecipeInfoMap.clear();

         for(var2 = 0; var2 < this.completeRecipeList.size(); ++var2) {
            CraftRecipe var3 = (CraftRecipe)this.completeRecipeList.get(var2);
            this.createCachedRecipeInfo(var3);
         }

         this.cachedRecipeInfosDirty = false;
      }
   }

   public CachedRecipeInfo getCachedRecipeInfo(CraftRecipe var1) {
      if (this.cachedRecipeInfosDirty) {
         this.rebuildCachedRecipeInfo();
      }

      Objects.requireNonNull(var1);
      CachedRecipeInfo var2 = (CachedRecipeInfo)this.cachedRecipeInfoMap.get(var1);
      if (var2 == null) {
         var2 = this.createCachedRecipeInfo(var1);
      }

      return var2;
   }

   public void setRecipes(List<CraftRecipe> var1) {
      this.completeRecipeList.clear();
      this.filteredRecipeList.clear();
      this.completeRecipeList.addAll(var1);
      this.cachedRecipeInfosDirty = true;
      this.filterRecipeList(this.filterString, this.categoryFilterString, true);
      this.triggerEvent("onSetRecipeList", this.filteredRecipeList);
   }

   public void addEventListener(String var1, Object var2) {
      this.addEventListener(var1, var2, (Object)null);
   }

   public void addEventListener(String var1, Object var2, Object var3) {
      if (this.events.containsKey(var1)) {
         ((ArrayList)this.events.get(var1)).add(new CraftEventHandler(var2, var3));
         if (Core.bDebug && ((ArrayList)this.events.get(var1)).size() > 10) {
            throw new RuntimeException("Sanity check, event '" + var1 + "' has >10 listeners");
         }
      } else {
         DebugLog.General.warn("Event '" + var1 + "' is unknown.");
      }

   }

   public static String getFavouriteModDataString(CraftRecipe var0) {
      return var0 != null ? "recipeFavourite:" + var0.getName() : null;
   }

   public void setSelectedRecipeStyle(String var1, String var2, IsoPlayer var3) {
      String var4 = var1 + "RecipeView";
      var3.getModData().rawset(var4, var2);
   }

   public String getSelectedRecipeStyle(String var1, IsoPlayer var2) {
      String var3 = var1 + "RecipeView";
      return (String)var2.getModData().rawget(var3);
   }

   public static boolean callLuaBool(String var0, Object var1) {
      Object var2 = LuaManager.getFunctionObject(var0);
      if (var2 != null) {
         Boolean var3 = LuaManager.caller.protectedCallBoolean(LuaManager.thread, var2, var1);
         return var3 == Boolean.TRUE;
      } else {
         return false;
      }
   }

   public static void callLua(String var0, Object var1) {
      Object var2 = LuaManager.getFunctionObject(var0);
      if (var2 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var2, var1);
      }

   }

   public boolean isCraftCheat() {
      return false;
   }

   public static class CachedRecipeComparator implements Comparator<CraftRecipe> {
      private final BaseCraftingLogic logic;

      public CachedRecipeComparator(BaseCraftingLogic var1) {
         this.logic = var1;
      }

      public int compare(CraftRecipe var1, CraftRecipe var2) {
         CachedRecipeInfo var3 = this.logic.getCachedRecipeInfo(var1);
         CachedRecipeInfo var4 = this.logic.getCachedRecipeInfo(var2);
         boolean var5 = var3.isValid();
         boolean var6 = var4.isValid();
         if (var5 && !var6) {
            return -1;
         } else if (!var5 && var6) {
            return 1;
         } else {
            boolean var7 = var3.isCanPerform();
            boolean var8 = var4.isCanPerform();
            if (var7 && !var8) {
               return -1;
            } else {
               return !var7 && var8 ? 1 : var1.getTranslationName().compareTo(var2.getTranslationName());
            }
         }
      }
   }

   protected static class CraftEventHandler {
      private final Object function;
      private final Object targetTable;

      public CraftEventHandler(Object var1, Object var2) {
         this.function = Objects.requireNonNull(var1);
         this.targetTable = var2;
      }
   }

   public static class CachedRecipeInfo {
      private static final ArrayDeque<CachedRecipeInfo> pool = new ArrayDeque();
      private CraftRecipe recipe;
      private boolean isValid;
      private boolean canPerform;
      private boolean available;

      public CachedRecipeInfo() {
      }

      private static CachedRecipeInfo Alloc(CraftRecipe var0) {
         CachedRecipeInfo var1 = (CachedRecipeInfo)pool.poll();
         if (var1 == null) {
            var1 = new CachedRecipeInfo();
         }

         var1.recipe = var0;
         return var1;
      }

      private static void Release(CachedRecipeInfo var0) {
         var0.reset();

         assert !pool.contains(var0);

         pool.offer(var0);
      }

      public CraftRecipe getRecipe() {
         return this.recipe;
      }

      public boolean isValid() {
         return this.isValid;
      }

      public boolean isCanPerform() {
         return this.canPerform;
      }

      public boolean isAvailable() {
         return this.available;
      }

      public void overrideCanPerform(boolean var1) {
         this.canPerform = var1;
      }

      private void reset() {
         this.recipe = null;
         this.isValid = false;
         this.canPerform = false;
         this.available = false;
      }
   }
}
