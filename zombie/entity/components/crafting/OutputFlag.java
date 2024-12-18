package zombie.entity.components.crafting;

public enum OutputFlag {
   HandcraftOnly,
   AutomationOnly,
   IsEmpty,
   ForceEmpty,
   AlwaysFill,
   RespectCapacity;

   private OutputFlag() {
   }
}
