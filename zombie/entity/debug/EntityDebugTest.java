package zombie.entity.debug;

import java.util.ArrayList;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;

public abstract class EntityDebugTest {
   private static final ArrayList<EntityDebugTest> entityDebugTests = new ArrayList();
   private static final boolean B_BUILD_PIPES = true;
   private static final boolean B_BUILD_WIRES = true;
   private static final String tilePipeEastWest = "industry_02_226";
   private static final String tilePipeNorthSouth = "industry_02_224";
   private static final String tileWireEastWest = "industry_02_197";
   private static final String tileWireNorthSouth = "industry_02_198";

   public EntityDebugTest() {
   }

   public static void CreateTest(EntityDebugTestType var0, IsoGridSquare var1) {
      DebugLog.General.println("Creating Entity Meta Test: " + var0);
      if (var1 == null) {
         DebugLog.General.warn("Square is null");
      } else {
         BaseTest var2 = null;
         switch (var0) {
            case BaseTest:
               var2 = new BaseTest();
            default:
               if (var2 != null) {
                  var2.create(var1);
                  entityDebugTests.add(var2);
               } else {
                  DebugLog.General.warn("Test is null.");
               }

         }
      }
   }

   public static void Update() {
      for(int var0 = 0; var0 < entityDebugTests.size(); ++var0) {
         ((EntityDebugTest)entityDebugTests.get(var0)).update();
      }

   }

   public static void Reset() {
      entityDebugTests.clear();
   }

   public abstract void create(IsoGridSquare var1);

   public abstract void update();

   protected IsoObject createEntity(IsoGridSquare var1, String var2) {
      GameEntityScript var3 = ScriptManager.instance.getGameEntityScript(var2);
      SpriteConfigManager.ObjectInfo var4 = SpriteConfigManager.GetObjectInfo(var2);
      String var5 = var4.getFace("single").getTileInfo(0, 0, 0).getSpriteName();
      IsoObject var6 = new IsoObject(var1, var5, var2);
      GameEntityFactory.CreateIsoObjectEntity(var6, var3, true);
      var1.AddSpecialObject(var6);
      return var6;
   }

   protected IsoObject createDummyObject(IsoGridSquare var1, String var2) {
      IsoObject var3 = new IsoObject(var1, var2, "DummyObject");
      var1.AddSpecialObject(var3);
      return var3;
   }

   protected IsoGridSquare createPipes(IsoGridSquare var1, IsoDirections var2, int var3, boolean var4) {
      return this.createUtility(var1, var2, var3, var4, "industry_02_224", "industry_02_226");
   }

   protected IsoGridSquare createWires(IsoGridSquare var1, IsoDirections var2, int var3, boolean var4) {
      return this.createUtility(var1, var2, var3, var4, "industry_02_198", "industry_02_197");
   }

   protected IsoGridSquare createUtility(IsoGridSquare var1, IsoDirections var2, int var3, boolean var4, String var5, String var6) {
      for(int var7 = 0; var7 < var3; ++var7) {
         var1 = var1.getAdjacentSquare(var2);
         if (var4) {
            if (var2 != IsoDirections.N && var2 != IsoDirections.S) {
               this.createDummyObject(var1, var6);
            } else {
               this.createDummyObject(var1, var5);
            }
         }
      }

      return var1.getAdjacentSquare(var2);
   }

   protected boolean isObjectConnected(IsoObject var1, IsoDirections var2, boolean var3) {
      IsoGridSquare var4 = var1.getSquare();
      var4 = var4.getAdjacentSquare(var2);
      if (var3) {
         return this.squareContainsSprite(var4, "industry_02_198") || this.squareContainsSprite(var4, "industry_02_197");
      } else {
         return this.squareContainsSprite(var4, "industry_02_224") || this.squareContainsSprite(var4, "industry_02_226");
      }
   }

   protected boolean squareContainsSprite(IsoGridSquare var1, String var2) {
      if (var1.getSpecialObjects() != null) {
         for(int var3 = 0; var3 < var1.getSpecialObjects().size(); ++var3) {
            IsoObject var4 = (IsoObject)var1.getSpecialObjects().get(var3);
            if (var4.getSpriteName() != null && var4.getSpriteName().equals(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   protected boolean isRunning(IsoObject var1) {
      CraftLogic var2 = (CraftLogic)var1.getComponent(ComponentType.CraftLogic);
      return var2 != null;
   }

   public static class BaseTest extends EntityDebugTest {
      private IsoObject waterSource;

      public BaseTest() {
      }

      public void create(IsoGridSquare var1) {
      }

      public void update() {
      }
   }
}
