package android.os.storage;
import java.util.List;
public interface StorageManager {
    List<StorageVolume> getStorageVolumes();
}