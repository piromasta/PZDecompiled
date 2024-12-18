package zombie.world;

public class EntityInfo extends DictionaryInfo<EntityInfo> {
   public EntityInfo() {
   }

   public String getInfoType() {
      return "entity";
   }

   public EntityInfo copy() {
      EntityInfo var1 = new EntityInfo();
      var1.copyFrom(this);
      return var1;
   }
}
