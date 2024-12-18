package zombie.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import zombie.core.logger.ExceptionLogger;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.Item;
import zombie.vehicles.VehiclePart;

public class GameEntityFactory {
   public GameEntityFactory() {
   }

   public static void TransferComponents(GameEntity var0, GameEntity var1) {
      if (var0.hasComponents()) {
         try {
            if (var0.hasComponent(ComponentType.SpriteConfig)) {
               SpriteConfig var2 = var0.getSpriteConfig();
               if (var2.isValidMultiSquare()) {
                  DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                  return;
               }
            }

            if (var0 instanceof Moveable) {
               Moveable var6 = (Moveable)var0;
               if (var6.getSpriteGrid() != null && (var6.getSpriteGrid().getWidth() > 1 || var6.getSpriteGrid().getHeight() > 1)) {
                  DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                  return;
               }
            }

            ArrayList var7 = new ArrayList();

            Component var3;
            int var4;
            for(var4 = 0; var4 < var0.componentSize(); ++var4) {
               var3 = var0.getComponentForIndex(var4);
               var7.add(var3);
            }

            for(var4 = 0; var4 < var7.size(); ++var4) {
               var3 = (Component)var7.get(var4);
               var0.removeComponent(var3);
               if (var1.hasComponent(var3.getComponentType())) {
                  var1.releaseComponent(var3.getComponentType());
               }

               var1.addComponent(var3);
            }

            var7.clear();
            var1.connectComponents();
         } catch (Exception var5) {
            ExceptionLogger.logException(var5);
         }

      }
   }

   public static void CreateIsoEntityFromCellLoading(IsoObject var0) {
      PropertyContainer var1 = var0.getProperties();
      if (var1 != null && (var1.Is(IsoFlagType.EntityScript) || var1.Is("IsMoveAble") && var1.Is("CustomItem"))) {
         try {
            boolean var2 = var1.Is(IsoFlagType.EntityScript);
            boolean var3 = var1.Is("IsMoveAble") && var1.Is("CustomItem");
            if (var2 && var3) {
               DebugLogStream var10000 = DebugLog.General;
               boolean var10001 = var1.Is("CustomItem");
               var10000.warn("Entity has custom item '" + var10001 + "' set, and entity script '" + var1.Val("EntityScriptName") + "' defined");
            }

            if (var3) {
               createIsoEntityFromCustomItem(var0, var1.Val("CustomItem"), true);
            } else {
               createEntity(var0, true);
            }
         } catch (Exception var4) {
            ExceptionLogger.logException(var4);
         }

      }
   }

   private static void createIsoEntityFromCustomItem(IsoObject var0, String var1, boolean var2) throws Exception {
      Item var3 = ScriptManager.instance.FindItem(var1);
      if (var3 != null) {
         createEntity(var0, var3, var2);
      } else {
         DebugLog.General.warn("Custom '" + var1 + "' item not found.");
      }

   }

   public static void CreateInventoryItemEntity(InventoryItem var0, Item var1, boolean var2) {
      if (var1 != null && var1.hasComponents()) {
         try {
            createEntity(var0, var1, var2);
         } catch (Exception var4) {
            ExceptionLogger.logException(var4);
         }

      }
   }

   public static void CreateIsoObjectEntity(IsoObject var0, GameEntityScript var1, boolean var2) {
      try {
         createEntity(var0, var1, var2);
      } catch (Exception var4) {
         ExceptionLogger.logException(var4);
      }

   }

   public static void CreateEntityDebugReload(GameEntity var0, GameEntityScript var1, boolean var2) {
      try {
         createEntity(var0, var1, var2);
      } catch (Exception var4) {
         ExceptionLogger.logException(var4);
      }

   }

   private static void createEntity(GameEntity var0, boolean var1) throws GameEntityException {
      var0 = (GameEntity)Objects.requireNonNull(var0);
      Object var2 = null;
      if (var0 instanceof IsoObject var3) {
         PropertyContainer var4 = var3.getProperties();
         if (var4 != null && var4.Is(IsoFlagType.EntityScript)) {
            String var5 = var4.Val("EntityScriptName");
            if (var5 != null) {
               var2 = ScriptManager.instance.getGameEntityScript(var5);
            }

            if (var2 == null) {
               throw new GameEntityException("EntityScript not found, script: " + (var5 != null ? var5 : "unknown"), var0);
            }
         }
      } else {
         if (!(var0 instanceof InventoryItem)) {
            if (var0 instanceof VehiclePart) {
               throw new GameEntityException("Not implemented yet");
            }

            throw new GameEntityException("Unsupported entity type.");
         }

         InventoryItem var6 = (InventoryItem)var0;
         var2 = var6.getScriptItem();
      }

      if (var2 != null) {
         createEntity(var0, (GameEntityScript)var2, var1);
      }

   }

   private static void createEntity(GameEntity var0, GameEntityScript var1, boolean var2) throws GameEntityException {
      var0 = (GameEntity)Objects.requireNonNull(var0);
      var1 = (GameEntityScript)Objects.requireNonNull(var1);
      if (var0.hasComponents()) {
         throw new GameEntityException("Calling CreateEntity on entity that already has components.", var0);
      } else {
         instanceComponents(var0, var1);
         EntityScriptInfo var3 = (EntityScriptInfo)ComponentType.Script.CreateComponent();
         var3.setOriginalScript(var1);
         var0.addComponent(var3);
         var0.connectComponents();
         if (var2) {
            var0.onFirstCreation();
         }

      }
   }

   private static void instanceComponents(GameEntity var0, GameEntityScript var1) {
      boolean var2 = var0.getGameEntityType() == GameEntityType.IsoObject;
      if (var2 && var1.containsComponent(ComponentType.SpriteConfig)) {
         SpriteConfigScript var9 = (SpriteConfigScript)var1.getComponentScriptFor(ComponentType.SpriteConfig);
         SpriteConfig var10 = (SpriteConfig)var9.type.CreateComponentFromScript(var9);
         var0.addComponent(var10);
         boolean var11 = var10.isMultiSquareMaster();

         for(int var7 = 0; var7 < var1.getComponentScripts().size(); ++var7) {
            ComponentScript var6 = (ComponentScript)var1.getComponentScripts().get(var7);
            if (var6.type != ComponentType.SpriteConfig && (var11 || !var6.isoMasterOnly())) {
               Component var8 = var6.type.CreateComponentFromScript(var6);
               var0.addComponent(var8);
            }
         }
      } else {
         for(int var4 = 0; var4 < var1.getComponentScripts().size(); ++var4) {
            ComponentScript var3 = (ComponentScript)var1.getComponentScripts().get(var4);
            Component var5 = var3.type.CreateComponentFromScript(var3);
            var0.addComponent(var5);
         }
      }

   }

   public static void RemoveComponentType(GameEntity var0, ComponentType var1) {
      if (var0.hasComponent(var1)) {
         var0.releaseComponent(var1);
      }

      var0.connectComponents();
   }

   public static void RemoveComponentTypes(GameEntity var0, EnumSet<ComponentType> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         ComponentType var3 = (ComponentType)var2.next();
         if (var0.hasComponent(var3)) {
            var0.releaseComponent(var3);
         }
      }

      var0.connectComponents();
   }

   public static void RemoveComponent(GameEntity var0, Component var1) {
      if (var0.containsComponent(var1)) {
         var0.releaseComponent(var1);
      }

      var0.connectComponents();
   }

   public static void RemoveComponents(GameEntity var0, Component... var1) {
      if (var1 != null && var1.length != 0) {
         Component[] var2 = var1;
         int var3 = var1.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Component var5 = var2[var4];
            var0.releaseComponent(var5);
         }

         var0.connectComponents();
      }
   }

   public static void AddComponent(GameEntity var0, Component var1) {
      AddComponent(var0, true, var1);
   }

   public static void AddComponents(GameEntity var0, Component... var1) {
      AddComponents(var0, true, var1);
   }

   public static void AddComponent(GameEntity var0, boolean var1, Component var2) {
      if (var0.hasComponent(var2.getComponentType())) {
         if (!var1) {
            return;
         }

         var0.releaseComponent(var2.getComponentType());
      }

      var0.addComponent(var2);
      var0.connectComponents();
   }

   public static void AddComponents(GameEntity var0, boolean var1, Component... var2) {
      if (var2 != null && var2.length != 0) {
         Component[] var3 = var2;
         int var4 = var2.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            Component var6 = var3[var5];
            if (var0.hasComponent(var6.getComponentType())) {
               if (!var1) {
                  continue;
               }

               var0.releaseComponent(var6.getComponentType());
            }

            var0.addComponent(var6);
         }

         var0.connectComponents();
      }
   }
}
