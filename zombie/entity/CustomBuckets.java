package zombie.entity;

public class CustomBuckets {
   public static final String MetaEntities = "MetaEntities";
   public static final String NonMetaRenderers = "NonMetaRenderers";
   public static final String NonMetaEntities = "NonMetaEntities";
   public static final String NonMetaCraftLogic = "NonMetaCraftLogic";
   private static final Family nonMetaCraftLogicFamily;

   public CustomBuckets() {
   }

   static void initializeCustomBuckets(Engine var0) {
      var0.registerCustomBucket("NonMetaRenderers", new NonMetaRenderersValidator());
      var0.registerCustomBucket("NonMetaEntities", new NonMetaEntitiesValidator());
      var0.registerCustomBucket("NonMetaCraftLogic", new NonMetaCraftLogicValidator());
      var0.registerCustomBucket("MetaEntities", new MetaEntitiesValidator());
   }

   static {
      nonMetaCraftLogicFamily = Family.all(ComponentType.CraftLogic).get();
   }

   private static class NonMetaRenderersValidator implements EntityBucket.EntityValidator {
      private NonMetaRenderersValidator() {
      }

      public boolean acceptsEntity(GameEntity var1) {
         return var1.hasRenderers() && !(var1 instanceof MetaEntity);
      }
   }

   private static class NonMetaEntitiesValidator implements EntityBucket.EntityValidator {
      private NonMetaEntitiesValidator() {
      }

      public boolean acceptsEntity(GameEntity var1) {
         return !(var1 instanceof MetaEntity);
      }
   }

   private static class NonMetaCraftLogicValidator implements EntityBucket.EntityValidator {
      private NonMetaCraftLogicValidator() {
      }

      public boolean acceptsEntity(GameEntity var1) {
         return CustomBuckets.nonMetaCraftLogicFamily.matches(var1) && !(var1 instanceof MetaEntity);
      }
   }

   private static class MetaEntitiesValidator implements EntityBucket.EntityValidator {
      private MetaEntitiesValidator() {
      }

      public boolean acceptsEntity(GameEntity var1) {
         return var1 instanceof MetaEntity;
      }
   }
}
