package zombie.scripting.itemConfig;

import zombie.scripting.itemConfig.enums.RootType;

public class BucketRoot {
   private RootType type;
   private String id;
   private SelectorBucket bucketSpawn;
   private SelectorBucket bucketOnCreate;

   public BucketRoot(RootType var1, String var2) {
      this.type = var1;
      this.id = var2;
   }

   public RootType getType() {
      return this.type;
   }

   protected String getId() {
      return this.id;
   }

   protected void setBucketSpawn(SelectorBucket var1) {
      this.bucketSpawn = var1;
   }

   public SelectorBucket getBucketSpawn() {
      return this.bucketSpawn;
   }

   protected void setBucketOnCreate(SelectorBucket var1) {
      this.bucketOnCreate = var1;
   }

   public SelectorBucket getBucketOnCreate() {
      return this.bucketOnCreate;
   }
}
