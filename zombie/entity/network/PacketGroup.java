package zombie.entity.network;

public enum PacketGroup {
   GameEntity,
   Generic,
   CraftLogic,
   Resources,
   MashingLogic;

   private PacketGroup() {
   }
}
