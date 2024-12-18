package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 1,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.AnimalCheats,
   handlingType = 3
)
public class AnimalCommandPacket implements INetworkPacket {
   public AnimalCommandPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public static enum Type {
      None,
      AddAnimalFromHandsInTrailer,
      AddAnimalInTrailer,
      RemoveAnimalFromTrailer,
      RemoveAndGrabAnimalFromTrailer,
      AttachAnimalToPlayer,
      AttachAnimalToTree,
      PickupAnimal,
      ButcherAnimal,
      FeedAnimalFromHand,
      HutchGrabAnimal,
      HutchGrabCorpseAction,
      DropAnimal,
      HutchRemoveAnimal,
      UpdateGenome;

      private Type() {
      }
   }
}
