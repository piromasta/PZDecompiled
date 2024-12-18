package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.popman.ObjectPool;

public final class OffMeshConnection {
   int triFrom;
   int edgeFrom;
   Mesh meshTo;
   int triTo;
   int edgeTo;
   final Vector2f edge1 = new Vector2f();
   final Vector2f edge2 = new Vector2f();
   static ObjectPool<OffMeshConnection> pool = new ObjectPool(OffMeshConnection::new);

   public OffMeshConnection() {
   }
}
