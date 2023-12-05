package zombie.iso.areas.isoregion.jobs;

public enum RegionJobType {
   SquareUpdate,
   ChunkUpdate,
   ApplyChanges,
   ServerSendFullData,
   DebugResetAllData;

   private RegionJobType() {
   }
}
