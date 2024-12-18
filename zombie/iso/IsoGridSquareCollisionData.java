package zombie.iso;

public class IsoGridSquareCollisionData {
   public Vector3 hitPosition = new Vector3();
   public IsoGridSquare isoGridSquare = null;
   public LosUtil.TestResults testResults;

   public IsoGridSquareCollisionData() {
      this.testResults = LosUtil.TestResults.Clear;
   }
}
