package zombie.entity;

public interface IBucketListener {
   void onBucketEntityAdded(EntityBucket var1, GameEntity var2);

   void onBucketEntityRemoved(EntityBucket var1, GameEntity var2);
}
